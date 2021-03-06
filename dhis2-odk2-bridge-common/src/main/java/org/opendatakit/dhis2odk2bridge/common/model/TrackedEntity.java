package org.opendatakit.dhis2odk2bridge.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TrackedEntity implements NameIdPair {
  private String id;
  private String displayName;

  @JsonProperty
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  @JsonProperty
  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  @Override
  public String toString() {
    return "TrackedEntity{" +
        "id='" + id + '\'' +
        ", displayName='" + displayName + '\'' +
        '}';
  }
}
