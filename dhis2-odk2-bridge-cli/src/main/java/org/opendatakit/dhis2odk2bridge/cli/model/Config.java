package org.opendatakit.dhis2odk2bridge.cli.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

public class Config {
  public String syncEndpointUrl;
  public String syncEndpointAppId;
  public String syncEndpointUsername;
  public String syncEndpointPassword;

  public String dhis2Url;
  public String dhis2Username;
  public String dhis2Password;

  @JsonProperty("toDhis2")
  public Map<String, SyncOptions> toDhis2;

  @JsonProperty("toSyncEndpoint")
  public Map<String, SyncOptions> toSyncEndpoint;

  public Config() {
    toDhis2 = new HashMap<>();
    toSyncEndpoint = new HashMap<>();
  }

  @Override
  public String toString() {
    return "Config{" +
        "syncEndpointUrl='" + syncEndpointUrl + '\'' +
        ", syncEndpointAppId='" + syncEndpointAppId + '\'' +
        ", syncEndpointUsername='" + syncEndpointUsername + '\'' +
        ", syncEndpointPassword='" + syncEndpointPassword + '\'' +
        ", dhis2Url='" + dhis2Url + '\'' +
        ", dhis2Username='" + dhis2Username + '\'' +
        ", dhis2Password='" + dhis2Password + '\'' +
        ", toDhis2=" + toDhis2 +
        '}';
  }
}
