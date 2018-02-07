package org.opendatakit.dhis2odk2bridge.common.util;

import org.hisp.dhis.dxf2.datavalue.DataValue;
import org.hisp.dhis.dxf2.events.trackedentity.Attribute;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance;
import org.opendatakit.aggregate.odktables.rest.entity.DataKeyValue;
import org.opendatakit.dhis2odk2bridge.common.consts.MetadataColumn;
import org.opendatakit.dhis2odk2bridge.common.model.DataValueIdentifier;

import java.util.*;

public class TypeUtil {
  public static DataValue toDataValue(DataKeyValue dkv, String period, String orgUnit) {
    DataValue output = new DataValue();

    output.setDataElement(dkv.column);
    output.setValue(dkv.value);
    output.setPeriod(period);
    output.setOrgUnit(orgUnit);

    return output;
  }

  public static Attribute toAttribute(DataKeyValue dkv, Map<String, String> colToAttrId) {
    Attribute attribute = new Attribute();

    attribute.setAttribute(colToAttrId.get(dkv.column));
    attribute.setValue(dkv.value);

    return attribute;
  }

  public static ArrayList<DataKeyValue> dataValueToDkvl(Map.Entry<DataValueIdentifier, List<DataValue>> entry) {
    ArrayList<DataKeyValue> dkvl = new ArrayList<>();

    dkvl.add(new DataKeyValue(MetadataColumn.PERIOD, entry.getKey().getPeriod()));
    dkvl.add(new DataKeyValue(MetadataColumn.OU, entry.getKey().getOrgUnit()));

    entry
        .getValue()
        .stream()
        .map(TypeUtil::toDataKeyValue)
        .forEach(dkvl::add);

    return dkvl;
  }

  public static ArrayList<DataKeyValue> teiToDkvl(Map.Entry<String, TrackedEntityInstance> entry) {
    ArrayList<DataKeyValue> dkvl = new ArrayList<>();

    entry
        .getValue()
        .getAttributes()
        .stream()
        .filter(attr -> !attr.getDisplayName().equals(MetadataColumn.SYNC_ROW_ID))
        .map(TypeUtil::toDataKeyValue)
        .forEach(dkvl::add);

    dkvl.add(new DataKeyValue(MetadataColumn.TEI, entry.getValue().getTrackedEntityInstance()));

    return dkvl;
  }

  private static DataKeyValue toDataKeyValue(DataValue dv) {
    return new DataKeyValue(dv.getDataElement(), dv.getValue());
  }

  private static DataKeyValue toDataKeyValue(Attribute attr) {
    return new DataKeyValue(attr.getDisplayName(), attr.getValue());
  }
}
