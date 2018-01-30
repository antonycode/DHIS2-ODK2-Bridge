package org.opendatakit.dhis2odk2bridge.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hisp.dhis.common.Pager;

import java.util.List;

public class OrgUnits {
  private Pager pager;
  private List<OrgUnit> organisationUnits;

  @JsonProperty
  public Pager getPager() {
    return pager;
  }

  public void setPager(Pager pager) {
    this.pager = pager;
  }

  @JsonProperty
  public List<OrgUnit> getOrganisationUnits() {
    return organisationUnits;
  }

  public void setOrganisationUnits(List<OrgUnit> organisationUnits) {
    this.organisationUnits = organisationUnits;
  }

  @Override
  public String toString() {
    return "OrgUnits{" +
        "pager=" + pager +
        ", organisationUnits=" + organisationUnits +
        '}';
  }
}
