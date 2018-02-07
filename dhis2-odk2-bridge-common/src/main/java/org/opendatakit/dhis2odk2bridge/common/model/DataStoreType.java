package org.opendatakit.dhis2odk2bridge.common.model;

import org.opendatakit.dhis2odk2bridge.common.consts.Dhis2Const;

public enum  DataStoreType {
  DATA_SET(Dhis2Const.DATA_SET_TYPE_NAME),
  TRACKED_ENTITY(Dhis2Const.TRACKED_ENTITY_TYPE_NAME),
  EVENT(Dhis2Const.ENTITY_TYPE_NAME);

  String name;

  DataStoreType(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return name;
  }
}
