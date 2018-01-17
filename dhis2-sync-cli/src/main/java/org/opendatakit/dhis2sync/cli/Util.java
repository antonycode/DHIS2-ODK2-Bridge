package org.opendatakit.dhis2sync.cli;

import org.opendatakit.dhis2sync.common.Dhis2Client;
import org.opendatakit.dhis2sync.common.SimpleSyncClient;

public class Util {
  public static SimpleSyncClient buildSyncClient(Config config) {
    return new SimpleSyncClient(config.syncEndpointUrl, config.syncEndpointAppId, config.syncEndpointUsername, config.syncEndpointPassword);
  }

  public static Dhis2Client buildDhis2Client(Config config) {
    return new Dhis2Client(config.dhis2Url, config.dhis2Username, config.dhis2Password);
  }
}
