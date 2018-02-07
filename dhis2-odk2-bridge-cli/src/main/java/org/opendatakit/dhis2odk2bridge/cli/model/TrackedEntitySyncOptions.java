package org.opendatakit.dhis2odk2bridge.cli.model;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.opendatakit.dhis2odk2bridge.common.consts.Dhis2Const;

@JsonTypeName(Dhis2Const.TRACKED_ENTITY_TYPE_NAME)
public class TrackedEntitySyncOptions extends SyncOptions {
  public OrganisationUnitSelectionMode ouMode;
}
