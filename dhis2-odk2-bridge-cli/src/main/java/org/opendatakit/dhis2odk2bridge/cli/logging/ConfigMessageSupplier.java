package org.opendatakit.dhis2odk2bridge.cli.logging;

import org.apache.logging.log4j.message.FormattedMessage;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.MessageSupplier;
import org.opendatakit.dhis2odk2bridge.cli.model.Config;

public class ConfigMessageSupplier implements MessageSupplier {
  private final Config config;

  public ConfigMessageSupplier(Config config) {
    this.config = config;
  }

  @Override
  public Message get() {
    return new FormattedMessage(
        "Configuration:\n" +
            "Using Sync Endpoint server {}\n" +
            "Using DHIS2 server {}\n" +
            "Pushing {} table{} to DHIS2\n" +
            "Pushing {} data set{}/tracked entit{} to Sync Endpoint",
        config.syncEndpointUrl,
        config.dhis2Url,
        config.toDhis2.size(), config.toDhis2.size() > 1 ? "s" : "",
        config.toSyncEndpoint.size(),
        config.toSyncEndpoint.size() > 1 ? "s" : "",
        config.toSyncEndpoint.size() > 1 ? "ies" : "y"
    );
  }
}
