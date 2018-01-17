package org.opendatakit.dhis2sync.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.opendatakit.aggregate.odktables.rest.entity.*;
import org.opendatakit.dhis2sync.common.util.Util;
import org.opendatakit.sync.client.SyncClient;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SimpleSyncClient implements AutoCloseable {
  public static final int BATCH_ALTER_LIMIT = 500;

  private final SyncClient syncClient;
  private final String syncEndpointUrl;
  private final String syncEndpointAppId;

  private final ObjectMapper objectMapper;

  private final Logger logger;

  public SimpleSyncClient(String url, String appId, String username, String password) {
    Objects.requireNonNull(url);
    Objects.requireNonNull(appId);
    Objects.requireNonNull(username);

    this.syncClient = new SyncClient();
    this.syncClient.init(null, username, password);

    this.syncEndpointUrl = url;
    this.syncEndpointAppId = appId;

    this.objectMapper = new ObjectMapper();

    this.logger = LogManager.getLogger();
  }

  public String getSchemaETag(String tableId) {
    Objects.requireNonNull(tableId);

    return syncClient.getSchemaETagForTable(syncEndpointUrl, syncEndpointAppId, tableId);
  }

  public String getDataETag(String tableId) throws IOException, JSONException {
    Objects.requireNonNull(tableId);

    return syncClient.getTableDataETag(syncEndpointUrl, syncEndpointAppId, tableId);
  }

  public TableResourceList getTables() throws IOException, JSONException {
    JSONObject jsonTables = syncClient.getTables(syncEndpointUrl, syncEndpointAppId);

    return Util.convertToObj(TableResourceList.class, jsonTables, objectMapper);
  }

  public List<RowResource> getAllRows(String tableId) throws IOException {
    List<RowResource> output = new ArrayList<>();
    String resumeCursor = null;

    do {
      RowResourceList rowList = getRows(tableId, resumeCursor, -1);
      output.addAll(rowList.getRows());

      resumeCursor = rowList.isHasMoreResults() ? rowList.getWebSafeResumeCursor() : null;
    } while (resumeCursor != null);

    return output;
  }

  public RowResourceList getRows(String tableId, String cursor, int fetchLimit) throws IOException {
    Objects.requireNonNull(tableId);

    String schemaETag = getSchemaETag(tableId);
    String fetchLimitStr = fetchLimit > 0 ? String.valueOf(fetchLimit) : null;

    JSONObject rows =
        syncClient.getRows(syncEndpointUrl, syncEndpointAppId, tableId, schemaETag, cursor, fetchLimitStr);

    return Util.convertToObj(RowResourceList.class, rows, objectMapper);
  }

  public int deleteTable(String tableId) throws IOException {
    Objects.requireNonNull(tableId);

    return syncClient.deleteTableDefinition(syncEndpointUrl, syncEndpointAppId, tableId, getSchemaETag(tableId));
  }

  public TableResource createTable(String tableId, ArrayList<Column> columns) throws IOException, JSONException {
    Objects.requireNonNull(tableId);
    Objects.requireNonNull(columns);

    if (columns.isEmpty()) {
      throw new IllegalArgumentException("Cannot create table without columns");
    }

    JSONObject table = syncClient.createTable(syncEndpointUrl, syncEndpointAppId, tableId, null, columns);

    return Util.convertToObj(TableResource.class, table, objectMapper);
  }

  public List<RowOutcome> insertRows(String tableId, List<Row> rows, boolean forceInsert) throws IOException, JSONException {
    Objects.requireNonNull(tableId);
    Objects.requireNonNull(rows);

    if (rows.isEmpty()) {
      return Collections.emptyList();
    }

    String schemaETag = getSchemaETag(tableId);
    String dataETag = getDataETag(tableId);

    List<RowOutcome> allRowOutcome = new ArrayList<>(rows.size());
    Map<String, Row> rowIdMap = null; // map from rowId -> row, compute lazily

    for (int i = 0; i < rows.size(); i += BATCH_ALTER_LIMIT) {
      ArrayList<Row> sublist = new ArrayList<>(rows.subList(i, Math.min(i + BATCH_ALTER_LIMIT, rows.size())));

      RowOutcomeList outcome = insertRowsSingleBatch(tableId, schemaETag, dataETag, sublist);
      dataETag = outcome.getDataETag();

      ArrayList<Row> resendList = new ArrayList<>();
      for (RowOutcome rowOutcome : outcome.getRows()) {
        allRowOutcome.add(rowOutcome);
        logRowOutcome(rowOutcome);

        // don't bother resending without forceInsert
        if (rowOutcome.getOutcome() == RowOutcome.OutcomeType.IN_CONFLICT && forceInsert) {
          if (rowIdMap == null) {
            rowIdMap = rows.stream().collect(Collectors.toMap(Row::getRowId, Function.identity()));
          }

          Row toResend = rowIdMap.get(rowOutcome.getRowId());
          // toResend would be null if the row in conflict didn't come with a row id (e.g. a new row)
          if (toResend != null) {
            // update dataETag to prevent conflict
            toResend.setRowETag(rowOutcome.getRowETag());
            resendList.add(toResend);
            // remove this row's rowOutcome,
            // to be replace by the resend outcome
            allRowOutcome.remove(allRowOutcome.size() - 1);
          }
        }
      }

      if (!resendList.isEmpty()) {
        logger.debug("forceInsert: {}, resending rows in conflict", forceInsert);

        outcome = insertRowsSingleBatch(tableId, schemaETag, dataETag, resendList);
        allRowOutcome.addAll(outcome.getRows());
        dataETag = outcome.getDataETag();

        for (RowOutcome rowOutcome : outcome.getRows()) {
          logRowOutcome(rowOutcome);

          if (rowOutcome.getOutcome() == RowOutcome.OutcomeType.IN_CONFLICT) {
            logger.info("Row still in conflict after resend {}", rowOutcome.getRowId());
          }
        }
      }
    }

    return allRowOutcome;
  }

  public RowOutcomeList insertRowsSingleBatch(String tableId, String schemaETag, String dataETag, ArrayList<Row> rows) throws IOException, JSONException {
    if (rows.isEmpty()) {
      return new RowOutcomeList();
    }

    if (rows.size() > BATCH_ALTER_LIMIT) {
      throw new IllegalArgumentException("Batch size exceeded limit of " + BATCH_ALTER_LIMIT);
    }

    return syncClient.alterRowsUsingSingleBatch(syncEndpointUrl, syncEndpointAppId, tableId, schemaETag, dataETag, rows);
  }

  private void logRowOutcome(RowOutcome rowOutcome) {
    switch (rowOutcome.getOutcome()) {
      case IN_CONFLICT:
        logger.warn("Row in conflict {}", rowOutcome.getRowId());
        break;
      case DENIED:
        logger.error("Row insert denied for {}", rowOutcome.getRowId());
        break;
      case FAILED:
        logger.error("Row insert failed for {}", rowOutcome.getRowId());
        break;
      case SUCCESS:
        logger.debug("Inserted row {}", rowOutcome.getRowId());
        break;
      default:
        // ignored
        break;
    }
  }

  @Override
  public void close() {
    this.syncClient.close();
  }
}
