package org.opendatakit.dhis2odk2bridge.common.model;

import org.hisp.dhis.dxf2.datavalue.DataValue;

import java.util.Objects;

public class DataValueIdentifier {
  private final String period;
  private final String orgUnit;

  public String getPeriod() {
    return period;
  }

  public String getOrgUnit() {
    return orgUnit;
  }

  public DataValueIdentifier(String period, String orgUnit) {
    Objects.requireNonNull(period);
    Objects.requireNonNull(orgUnit);

    this.period = period;
    this.orgUnit = orgUnit;
  }

  public DataValueIdentifier(DataValue dataValue) {
    this(dataValue.getPeriod(), dataValue.getOrgUnit());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    DataValueIdentifier that = (DataValueIdentifier) o;

    if (!getPeriod().equals(that.getPeriod())) return false;
    return getOrgUnit().equals(that.getOrgUnit());
  }

  @Override
  public int hashCode() {
    int result = getPeriod().hashCode();
    result = 31 * result + getOrgUnit().hashCode();
    return result;
  }
}
