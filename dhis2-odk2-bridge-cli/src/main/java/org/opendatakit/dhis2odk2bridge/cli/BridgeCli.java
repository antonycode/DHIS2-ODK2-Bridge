package org.opendatakit.dhis2odk2bridge.cli;

import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.wink.json4j.JSONException;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dxf2.datavalue.DataValue;
import org.hisp.dhis.dxf2.datavalueset.DataValueSet;
import org.hisp.dhis.dxf2.events.trackedentity.Attribute;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstances;
import org.hisp.dhis.dxf2.importsummary.ImportCount;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.opendatakit.aggregate.odktables.rest.TableConstants;
import org.opendatakit.aggregate.odktables.rest.entity.*;
import org.opendatakit.dhis2odk2bridge.cli.logging.ConfigMessageSupplier;
import org.opendatakit.dhis2odk2bridge.cli.logging.ImportSummaryMsgSupplier;
import org.opendatakit.dhis2odk2bridge.cli.logging.RowOutcomeListMsgSupplier;
import org.opendatakit.dhis2odk2bridge.cli.model.Config;
import org.opendatakit.dhis2odk2bridge.cli.model.DataSetSyncOptions;
import org.opendatakit.dhis2odk2bridge.cli.model.SyncOptions;
import org.opendatakit.dhis2odk2bridge.cli.model.TrackedEntitySyncOptions;
import org.opendatakit.dhis2odk2bridge.common.Dhis2Client;
import org.opendatakit.dhis2odk2bridge.common.SimpleSyncClient;
import org.opendatakit.dhis2odk2bridge.common.consts.Dhis2Const;
import org.opendatakit.dhis2odk2bridge.common.consts.MetadataColumn;
import org.opendatakit.dhis2odk2bridge.common.model.DataValueIdentifier;
import org.opendatakit.dhis2odk2bridge.common.model.TrackedEntityAttribute;
import org.opendatakit.dhis2odk2bridge.common.model.TrackedEntityAttributes;
import org.opendatakit.dhis2odk2bridge.common.util.TypeUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class BridgeCli {
  private static final Logger logger = LogManager.getLogger();

  public static void main(String[] args) {
    String configPath = getConfigPropsPath(args);

    if (configPath == null) {
      return;
    }

    Config config = parseConfigProps(configPath);

    if (config == null) {
      return;
    }

    logger.info(new ConfigMessageSupplier(config));

    Dhis2Client dhis2Client = Util.buildDhis2Client(config);
    try (SimpleSyncClient syncClient = Util.buildSyncClient(config)) {
      config.toDhis2.forEach((tableId, options) -> pushToDhis2(tableId, options, syncClient, dhis2Client)
          .map(summary -> new ImportSummaryMsgSupplier(summary, tableId))
          .ifPresent(logger::info)
      );

      config.toSyncEndpoint.forEach((tableId, options) -> pushToSyncEndpoint(tableId, options, syncClient, dhis2Client)
          .map(outcomes -> new RowOutcomeListMsgSupplier(outcomes, tableId))
          .ifPresent(logger::info)
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

    int ouIndex = keyIndexMap.get(MetadataColumn.OU);

    // TODO: break this into methods
    switch (options.type) {
      case Dhis2Const.DATA_SET_TYPE_NAME:
        int periodIndex = keyIndexMap.get(MetadataColumn.PERIOD);

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

      case Dhis2Const.TRACKED_ENTITY_TYPE_NAME:
        int teiIndex = keyIndexMap.get(MetadataColumn.TEI);

        String trackedEntityId;
        try {
          trackedEntityId = dhis2Client.getTrackedEntityId(tableId).orElseThrow(IOException::new);
        } catch (IOException e) {
          logger.error("failed to get tracked entity id for table {}", tableId);
          logger.catching(e);
          return Optional.empty();
        }

        Map<String, String> attrNameToIdMap = buildAttrNameToIdMap(dhis2Client);

        TrackedEntityInstances teiToDhis = new TrackedEntityInstances();
        for (RowResource row : allRows) {
          ArrayList<DataKeyValue> values = row.getValues();

          String tei = values.remove(teiIndex).value;
          String ou;
          try {
            ou = dhis2Client.getOrgUnitId(values.remove(ouIndex).value).orElseThrow(IllegalArgumentException::new);
          } catch (IOException e) {
            return Optional.empty();
          }

          TrackedEntityInstance instance = new TrackedEntityInstance();
          instance.setTrackedEntity(trackedEntityId);
          instance.setTrackedEntityInstance(tei);
          instance.setOrgUnit(ou);

          Attribute rowIdAttr =
              new Attribute(attrNameToIdMap.get(MetadataColumn.SYNC_ROW_ID), ValueType.TEXT, row.getRowId());
          instance.getAttributes().add(rowIdAttr);

          values
              .stream()
              .map(dkv -> TypeUtil.toAttribute(dkv, attrNameToIdMap))
              .forEach(instance.getAttributes()::add);

          teiToDhis.getTrackedEntityInstances().add(instance);
        }

        try {
          return Optional
              .ofNullable(dhis2Client.postTrackedEntityInstances(teiToDhis, options.importOptions))
              .map(s ->
                  new ImportSummary(
                      s.getStatus(),
                      "",
                      new ImportCount(s.getImported(), s.getUpdated(), s.getIgnored(), s.getDeleted())
                  )
              );
        } catch (IOException e) {
          logger.error("failed to post tracked entity instances for table {}", tableId);
          logger.catching(e);
          return Optional.empty();
        }

      default:
        logger.error("unrecognized type {} for table {}", options.type, tableId);
        return Optional.empty();
    }
  }

  private static Optional<List<RowOutcome>> pushToSyncEndpoint(String tableId, SyncOptions options,
                                                               SimpleSyncClient syncClient, Dhis2Client dhis2Client) {
    List<String> orgUnits;
    try {
      orgUnits = convertOrgUnitsToIds(dhis2Client, options.orgUnit);
    } catch (IOException e) {
      logger.error("failed to retrieve organization units for data set {}", tableId);
      logger.catching(e);
      return Optional.empty();
    }

    if (orgUnits.isEmpty()) {
      logger.error("at least 1 valid organization unit is required for {}", tableId);
      return Optional.empty();
    }

    // TODO: break this into methods
    switch (options.type) {
      case Dhis2Const.DATA_SET_TYPE_NAME:
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

        DataSetSyncOptions dataSetSyncOptions = ((DataSetSyncOptions) options);

        DataValueSet dataValueSet;
        try {
          dataValueSet =
              dhis2Client.getDataValueSets(
                  dataSetId.get(),
                  Arrays.asList(options.orgUnit.split(",")),
                  dataSetSyncOptions.startDate,
                  dataSetSyncOptions.endDate,
                  dataSetSyncOptions.children,
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
                null,
                null,
                null,
                null,
                TableConstants.nanoSecondsFromMillis(System.currentTimeMillis()),
                null,
                RowFilterScope.EMPTY_ROW_FILTER,
                TypeUtil.dataValueToDkvl(entry)
            ))
            .collect(Collectors.toList());

        try {
          return Optional.ofNullable(syncClient.insertRows(tableId, toSyncEndpoint, options.forceInsert));
        } catch (JSONException | IOException e) {
          logger.error("failed to post rows for data set {}", tableId);
          logger.catching(e);
          return Optional.empty();
        }

      case Dhis2Const.TRACKED_ENTITY_TYPE_NAME:
        Optional<String> trackedEntityId;
        try {
          trackedEntityId = dhis2Client.getTrackedEntityId(tableId);
        } catch (IOException e) {
          logger.error("failed to lookup tracked entity {}", tableId);
          logger.catching(e);
          return Optional.empty();
        }

        if (!trackedEntityId.isPresent()) {
          logger.error("failed to find tracked entity id for tracked entity {}", tableId);
          return Optional.empty();
        }

        TrackedEntitySyncOptions teSyncOptions = ((TrackedEntitySyncOptions) options);

        TrackedEntityInstances teis;
        try {
          teis =
              dhis2Client.getTrackedEntityInstances(
                  trackedEntityId.get(),
                  orgUnits,
                  teSyncOptions.ouMode,
                  options.includeDeleted
              );
        } catch (IOException e) {
          logger.error("failed to retrieve tracked entity instances from tracked entity {}", tableId);
          logger.catching(e);
          return Optional.empty();
        }

        if (teis.getTrackedEntityInstances().isEmpty()) {
          logger.info("tracked entity {} is empty, skipped", tableId);
          return Optional.empty();
        }

        Map<String, RowResource> teiSyncEndpointRows;
        try {
          teiSyncEndpointRows = syncClient.getAllRows(tableId).stream().collect(Collectors.toMap(Row::getRowId, Function.identity()));
        } catch (IOException e) {
          logger.error("failed to retrieve rows from tracked entity {}", tableId);
          logger.catching(e);
          return Optional.empty();
        }

        Map<String, TrackedEntityInstance> teiGroupedById = teis
            .getTrackedEntityInstances()
            .stream()
            .collect(Collectors.toMap(
                tei -> tei
                    .getAttributes()
                    .stream()
                    .filter(attr -> attr.getDisplayName().equals(MetadataColumn.SYNC_ROW_ID))
                    .findAny()
                    .map(Attribute::getValue)
                    .orElseThrow(IllegalStateException::new),
                Function.identity()
            ));

        List<Row> teiToSyncEndpoint = teiGroupedById
            .entrySet()
            .stream()
            .map(entry -> Row.forUpdate(
                entry.getKey(),
                teiSyncEndpointRows.get(entry.getKey()).getRowETag(),
                teiSyncEndpointRows.get(entry.getKey()).getFormId(),
                teiSyncEndpointRows.get(entry.getKey()).getLocale(),
                teiSyncEndpointRows.get(entry.getKey()).getSavepointType(),
                teiSyncEndpointRows.get(entry.getKey()).getSavepointTimestamp(),
                teiSyncEndpointRows.get(entry.getKey()).getSavepointCreator(),
                teiSyncEndpointRows.get(entry.getKey()).getRowFilterScope(),
                TypeUtil.teiToDkvl(entry)
            ))
            .collect(Collectors.toList());

        try {
          return Optional.ofNullable(syncClient.insertRows(tableId, teiToSyncEndpoint, options.forceInsert));
        } catch (JSONException | IOException e) {
          logger.error("failed to post rows for tracked entity {}", tableId);
          logger.catching(e);
          return Optional.empty();
        }

      default:
        logger.error("unrecognized type {} for table {}", options.type, tableId);
        return Optional.empty();
    }
  }

  private static List<String> convertOrgUnitsToIds(Dhis2Client dhis2Client, String orgUnitNames) throws IOException {
    Objects.requireNonNull(dhis2Client);
    Objects.requireNonNull(orgUnitNames);

    List<String> idList = new ArrayList<>();

    for (String s : orgUnitNames.split(",")) {
      Optional<String> id = dhis2Client.getOrgUnitId(s);

      if (id.isPresent()) {
        idList.add(id.get());
      } else {
        logger.warn("failed to find organization unit with name {}", s);
      }
    }

    return idList;
  }

  private static Optional<String> rowIdFinder(DataValueIdentifier dvId, List<RowResource> rows) {
    int periodIndex = Collections.binarySearch(
        rows.get(0).getValues(),
        new DataKeyValue(MetadataColumn.PERIOD, ""),
        Comparator.comparing(dkv -> dkv.column)
    );

    int ouIndex = Collections.binarySearch(
        rows.get(0).getValues(),
        new DataKeyValue(MetadataColumn.OU, ""),
        Comparator.comparing(dkv -> dkv.column)
    );

    if (periodIndex < 0 || ouIndex < 0) {
      return Optional.empty();
    }

    return rows
        .stream()
        .filter(r -> r.getValues().get(periodIndex).value.equals(dvId.getPeriod()))
        .filter(r -> r.getValues().get(ouIndex).value.equals(dvId.getOrgUnit()))
        .findAny()
        .map(Row::getRowId);
  }

  private static Map<String, String> buildAttrNameToIdMap(Dhis2Client dhis2Client) {
    TrackedEntityAttributes attributes;
    try {
      attributes = dhis2Client.getTrackedEntityAttributes();
    } catch (IOException e) {
      return Collections.emptyMap();
    }

    return attributes
        .getList()
        .stream()
        .collect(Collectors.toMap(TrackedEntityAttribute::getDisplayName, TrackedEntityAttribute::getId));
  }

  private static String getConfigPropsPath(String[] args) {
    if (args.length > 0) {
      return args[0];
    } else {
      logger.fatal("Please enter path to configuration as the first argument.");
      return null;
    }
  }

  private static Config parseConfigProps(String path) {
    logger.info("Reading configuration from {}", path);

    try {
      JavaPropsMapper mapper = new JavaPropsMapper();
      return mapper.readerFor(Config.class).readValue(Files.readAllBytes(Paths.get(path)));
    } catch (IOException e) {
      logger.fatal("failed to parse configuration", e);
      return null;
    }
  }
}
