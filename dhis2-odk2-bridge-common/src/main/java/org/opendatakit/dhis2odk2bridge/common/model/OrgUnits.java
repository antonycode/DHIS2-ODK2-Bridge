package org.opendatakit.dhis2odk2bridge.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hisp.dhis.common.Pager;

import java.util.List;

public class OrgUnits implements NameIdPairContainer<OrgUnit> {
  private Pager pager;
  private List<OrgUnit> list;

  @JsonProperty
  public Pager getPager() {
    return pager;
  }

  public void setPager(Pager pager) {
    this.pager = pager;
  }

  @JsonProperty("organisationUnits")
  public List<OrgUnit> getList() {
    return list;
  }

  public void setList(List<OrgUnit> list) {
    this.list = list;
  }

  @Override
  public String toString() {
    return "OrgUnits{" +
        "pager=" + pager +
        ", list=" + list +
        '}';
  }
}
