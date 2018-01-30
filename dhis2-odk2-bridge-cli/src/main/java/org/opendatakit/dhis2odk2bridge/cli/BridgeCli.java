package org.opendatakit.dhis2odk2bridge.cli;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.wink.json4j.JSONException;
import org.hisp.dhis.dxf2.datavalue.DataValue;
import org.hisp.dhis.dxf2.datavalueset.DataValueSet;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.opendatakit.aggregate.odktables.rest.TableConstants;
import org.opendatakit.aggregate.odktables.rest.entity.*;
import org.opendatakit.dhis2odk2bridge.common.Dhis2Client;
import org.opendatakit.dhis2odk2bridge.common.SimpleSyncClient;
import org.opendatakit.dhis2odk2bridge.common.model.DataValueIdentifier;
import org.opendatakit.dhis2odk2bridge.common.util.TypeUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class BridgeCli {
  private static final Logger logger = LogManager.getLogger();

  public static void main(String[] args) throws IOException {
    String configPath;
    if (args.length > 0) {
      configPath = args[0];
    } else {
      logger.fatal("Please enter path to configuration as the first argument.");
      return;
    }

    logger.info("Reading configuration from {}", configPath);

    Config config;
    try {
      JavaPropsMapper mapper = new JavaPropsMapper();
      config = mapper.readerFor(Config.class).readValue(Files.readAllBytes(Paths.get(configPath)));
    } catch (JsonMappingException e) {
      logger.fatal("failed to parse configuration", e);
      return;
    }

    logger.info("Using Sync Endpoint server {}", config.syncEndpointUrl);
    logger.info("Using DHIS2 server {}", config.dhis2Url);

    logger.info("Pushing {} table{} to DHIS2",
        config.toDhis2.size(), config.toDhis2.size() > 1 ? "s" : "");
    logger.info("Pushing {} data set{} to Sync Endpoint",
        config.toSyncEndpoint.size(), config.toSyncEndpoint.size() > 1 ? "s" : "");

    Dhis2Client dhis2Client = Util.buildDhis2Client(config);
    try (SimpleSyncClient syncClient = Util.buildSyncClient(config)) {
      config.toDhis2.forEach((tableId, options) ->
          pushToDhis2(tableId, options, syncClient, dhis2Client).ifPresent(sum ->
              logger.info("{} (Sync Endpoint -> DHIS2): {}, imports = {}, updates = {}, ignores = {}",
                  tableId,
                  sum.getDescription(),
                  sum.getImportCount().getImported(),
                  sum.getImportCount().getUpdated(),
                  sum.getImportCount().getIgnored()
              )
          )
      );

      config.toSyncEndpoint.forEach((tableId, options) ->
          pushToSyncEndpoint(tableId, options, syncClient, dhis2Client).ifPresent(list ->
              logger.info("{} (DHIS2 -> Sync Endpoint): inserted {} rows successfully",
                  tableId,
                  list.stream().filter(out -> out.getOutcome() == RowOutcome.OutcomeType.SUCCESS).count()
              )
          )
      );
    }

    logger.info("Done.");
  }

  private static Optional<ImportSummary> pushToDhis2(String tableId, SyncOptions options,
                                                     SimpleSyncClient syncClient, Dhis2Client dhis2Client) {
    List<RowResource> allRows;
    try {
      allRows = syncClient.getAllRows(tableId);
    } catch (IOException e) {
      logger.error("failed to retrieve rows from table {}", tableId);
      logger.catching(e);
      return Optional.empty();
    }

    if (allRows.isEmpty()) {
      logger.info("table {} is empty, skipped", tableId);
      return Optional.empty();
    }

    // construct element key -> index map for dkvl
    List<DataKeyValue> sampleDkvl = allRows.get(0).getValues();
    Map<String, Integer> keyIndexMap = new HashMap<>(sampleDkvl.size());
    for (int i = 0; i < sampleDkvl.size(); i++) {
      keyIndexMap.put(sampleDkvl.get(i).column, i);
    }

    int periodIndex = keyIndexMap.get("period");
    int ouIndex = keyIndexMap.get("orgUnit");

    DataValueSet toDhis = new DataValueSet();
    for (RowResource row : allRows) {
      ArrayList<DataKeyValue> values = row.getValues();

      String period = values.remove(periodIndex).value;
      String ou = values.remove(ouIndex).value;

      values
          .stream()
          .map(dkv -> TypeUtil.toDataValue(dkv, period, ou))
          .forEach(toDhis.getDataValues()::add);
    }

    toDhis.setDataSet(tableId);
    try {
      return Optional.ofNullable(dhis2Client.postDataValueSet(toDhis, options.importOptions));
    } catch (IOException e) {
      logger.error("failed to post data value set for table {}", tableId);
      logger.catching(e);
      return Optional.empty();
    }
  }

  private static Optional<List<RowOutcome>> pushToSyncEndpoint(String tableId, SyncOptions options,
                                                               SimpleSyncClient syncClient, Dhis2Client dhis2Client) {
    Optional<String> dataSetId;
    try {
      dataSetId = dhis2Client.getDataSetId(tableId);
    } catch (IOException e) {
      logger.error("failed to lookup data set {}", tableId);
      logger.catching(e);
      return Optional.empty();
    }

    if (!dataSetId.isPresent()) {
      logger.error("failed to find data set id for data set {}", tableId);
      return Optional.empty();
    }

    String orgUnits;
    try {
      orgUnits = convertOrgUnitsToString(dhis2Client, options.orgUnit);
    } catch (IOException e) {
      logger.error("failed to retrieve organization units for data set {}", tableId);
      logger.catching(e);
      return Optional.empty();
    }

    if (orgUnits == null) {
      logger.error("at least 1 valid organization unit is required for {}", tableId);
      return Optional.empty();
    }

    DataValueSet dataValueSet;
    try {
      dataValueSet =
          dhis2Client.getDataValueSets(
              dataSetId.get(),
              orgUnits,
              options.startDate,
              options.endDate,
              options.children,
              options.includeDeleted
          );
    } catch (IOException e) {
      logger.error("failed to retrieve data value sets from data set {}", tableId);
      logger.catching(e);
      return Optional.empty();
    }

    if (dataValueSet.getDataValues().isEmpty()) {
      logger.info("data set {} is empty, skipped", tableId);
      return Optional.empty();
    }

    List<RowResource> syncEndpointRows;
    try {
      syncEndpointRows = syncClient.getAllRows(tableId);
    } catch (IOException e) {
      logger.error("failed to retrieve rows from data set {}", tableId);
      logger.catching(e);
      return Optional.empty();
    }

    Map<DataValueIdentifier, List<DataValue>> dvGroupedByIdentifier = dataValueSet
        .getDataValues()
        .stream()
        .collect(Collectors.groupingBy(DataValueIdentifier::new));

    List<Row> toSyncEndpoint = dvGroupedByIdentifier
        .entrySet()
        .stream()
        .map(entry -> Row.forUpdate(
            rowIdFinder(entry.getKey(), syncEndpointRows).orElse(null),
            null, null, null, null,
            TableConstants.nanoSecondsFromMillis(System.currentTimeMillis()), null,
            RowFilterScope.EMPTY_ROW_FILTER, TypeUtil.toDkvl(entry)
        ))
        .collect(Collectors.toList());

    try {
      return Optional.ofNullable(syncClient.insertRows(tableId, toSyncEndpoint, options.forceInsert));
    } catch (JSONException | IOException e) {
      logger.error("failed to post rows for data set {}", tableId);
      logger.catching(e);
      return Optional.empty();
    }
  }

  private static String convertOrgUnitsToString(Dhis2Client dhis2Client, String orgUnitNames) throws IOException {
    Objects.requireNonNull(dhis2Client);
    Objects.requireNonNull(orgUnitNames);

    StringJoiner stringJoiner = new StringJoiner(",");

    for (String s : orgUnitNames.split(",")) {
      Optional<String> id = dhis2Client.getOrgUnitId(s);

      if (id.isPresent()) {
        stringJoiner.add(id.get());
      } else {
        logger.warn("failed to find organization unit with name {}", s);
      }
    }

    return stringJoiner.length() == 0 ? null : stringJoiner.toString();
  }

  private static Optional<String> rowIdFinder(DataValueIdentifier dvId, List<RowResource> rows) {
    int periodIndex = -1;
    int ouIndex = -1;

    for (int i = 0; i < rows.get(0).getValues().size(); i++) {
      String column = rows.get(0).getValues().get(i).column;

      if (column.equals("period") && periodIndex < 0) {
        periodIndex = i;
      } else if (column.equals("orgUnit") && ouIndex < 0) {
        ouIndex = i;
      } else if (periodIndex != -1 && ouIndex != -1) {
        break;
      }
    }

    int periodIndexFinal = periodIndex;
    int ouIndexFinal = ouIndex;

    return rows
        .stream()
        .filter(r -> r.getValues().get(periodIndexFinal).value.equals(dvId.getPeriod()))
        .filter(r -> r.getValues().get(ouIndexFinal).value.equals(dvId.getOrgUnit()))
        .findAny()
        .map(Row::getRowId);
  }
}
