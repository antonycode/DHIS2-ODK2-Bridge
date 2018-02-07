package org.opendatakit.dhis2odk2bridge.cli.model;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.opendatakit.dhis2odk2bridge.common.consts.Dhis2Const;

@JsonTypeName(Dhis2Const.DATA_SET_TYPE_NAME)
public class DataSetSyncOptions extends SyncOptions {
  // params for sync endpoint -> dhis2
  // empty for now

  // params for dhis2 -> sync endpoint
  public String startDate;
  public String endDate;
  public boolean children;
}
