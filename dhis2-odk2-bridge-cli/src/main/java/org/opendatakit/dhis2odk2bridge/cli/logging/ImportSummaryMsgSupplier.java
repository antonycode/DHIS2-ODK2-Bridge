package org.opendatakit.dhis2odk2bridge.cli.logging;

import org.apache.logging.log4j.message.FormattedMessage;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.MessageSupplier;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;

public class ImportSummaryMsgSupplier implements MessageSupplier {
  private final ImportSummary summary;
  private final String tableId;

  public ImportSummaryMsgSupplier(ImportSummary summary, String tableId) {
    this.summary = summary;
    this.tableId = tableId;
  }

  @Override
  public Message get() {
    return new FormattedMessage(
        "{} (Sync Endpoint -> DHIS2): {}, imports = {}, updates = {}, ignores = {}",
        tableId,
        summary.getDescription(),
        summary.getImportCount().getImported(),
        summary.getImportCount().getUpdated(),
        summary.getImportCount().getIgnored()
    );
  }
}
