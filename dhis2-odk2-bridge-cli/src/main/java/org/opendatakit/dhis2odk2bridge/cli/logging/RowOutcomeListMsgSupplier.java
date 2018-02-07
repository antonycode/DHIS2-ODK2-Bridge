package org.opendatakit.dhis2odk2bridge.cli.logging;

import org.apache.logging.log4j.message.FormattedMessage;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.MessageSupplier;
import org.opendatakit.aggregate.odktables.rest.entity.RowOutcome;

import java.util.List;

public class RowOutcomeListMsgSupplier implements MessageSupplier {
  private final List<RowOutcome> rowOutcomes;
  private final String tableId;

  public RowOutcomeListMsgSupplier(List<RowOutcome> rowOutcomes, String tableId) {
    this.rowOutcomes = rowOutcomes;
    this.tableId = tableId;
  }

  @Override
  public Message get() {
    return new FormattedMessage(
        "{} (DHIS2 -> Sync Endpoint): inserted {} rows successfully",
        tableId,
        rowOutcomes.stream().filter(out -> out.getOutcome() == RowOutcome.OutcomeType.SUCCESS).count()
    );
  }
}
