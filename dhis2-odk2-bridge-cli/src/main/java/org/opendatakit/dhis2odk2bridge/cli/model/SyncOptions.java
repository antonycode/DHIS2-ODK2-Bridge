package org.opendatakit.dhis2odk2bridge.cli.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.hisp.dhis.dxf2.common.ImportOptions;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = DataSetSyncOptions.class),
    @JsonSubTypes.Type(value = TrackedEntitySyncOptions.class)
})
public abstract class SyncOptions {
  public String type;

  // params for sync endpoint -> dhis2
  public ImportOptions importOptions;

  // params for dhis2 -> sync endpoint
  public String orgUnit;
  public boolean includeDeleted;
  public boolean forceInsert;
}
