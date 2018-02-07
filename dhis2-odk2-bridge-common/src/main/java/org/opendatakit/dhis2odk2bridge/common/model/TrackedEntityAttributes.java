package org.opendatakit.dhis2odk2bridge.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hisp.dhis.common.Pager;

import java.util.List;

public class TrackedEntityAttributes implements NameIdPairContainer<TrackedEntityAttribute> {
  private Pager pager;
  private List<TrackedEntityAttribute> list;

  @JsonProperty
  public Pager getPager() {
    return pager;
  }

  public void setPager(Pager pager) {
    this.pager = pager;
  }

  @JsonProperty("trackedEntityAttributes")
  public List<TrackedEntityAttribute> getList() {
    return list;
  }

  public void setList(List<TrackedEntityAttribute> list) {
    this.list = list;
  }

  @Override
  public String toString() {
    return "TrackedEntityAttributes{" +
        "pager=" + pager +
        ", list=" + list +
        '}';
  }
}
