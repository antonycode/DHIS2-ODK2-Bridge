package org.opendatakit.dhis2odk2bridge.common.util;

import org.hisp.dhis.dxf2.datavalue.DataValue;
import org.opendatakit.aggregate.odktables.rest.entity.DataKeyValue;
import org.opendatakit.dhis2odk2bridge.common.model.DataValueIdentifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class TypeUtil {
  public static DataValue toDataValue(DataKeyValue dkv, String period, String orgUnit) {
    DataValue output = new DataValue();

    output.setDataElement(dkv.column);
    output.setValue(dkv.value);
    output.setPeriod(period);
    output.setOrgUnit(orgUnit);

    return output;
  }

  public static ArrayList<DataKeyValue> toDkvl(Map.Entry<DataValueIdentifier, List<DataValue>> entry) {
    ArrayList<DataKeyValue> dkvl = new ArrayList<>(toPartialDkvl(entry.getKey()));
    entry
        .getValue()
        .stream()
        .map(TypeUtil::toDataKeyValue)
        .forEach(dkvl::add);

    return dkvl;
  }

  private static List<DataKeyValue> toPartialDkvl(DataValueIdentifier dvId) {
    return Arrays.asList(
        new DataKeyValue("period", dvId.getPeriod()),
        new DataKeyValue("orgUnit", dvId.getOrgUnit())
    );
  }

  private static DataKeyValue toDataKeyValue(DataValue dv) {
    return new DataKeyValue(dv.getDataElement(), dv.getValue());
  }
}
