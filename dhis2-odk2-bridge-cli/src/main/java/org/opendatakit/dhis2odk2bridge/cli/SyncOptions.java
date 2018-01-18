package org.opendatakit.dhis2odk2bridge.cli;

import org.hisp.dhis.dxf2.common.ImportOptions;

public class SyncOptions {
  public ImportOptions importOptions;

  public String dataSet;
  public String orgUnit;
  public String startDate;
  public String endDate;
  public boolean children;
  public boolean includeDeleted;

  public boolean forceInsert;
}
