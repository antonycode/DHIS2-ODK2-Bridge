package org.opendatakit.dhis2odk2bridge.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hisp.dhis.common.Pager;

import java.util.List;

public class TrackedEntities implements NameIdPairContainer<TrackedEntity> {
  private Pager pager;
  private List<TrackedEntity> list;

  @JsonProperty
  public Pager getPager() {
    return pager;
  }

  public void setPager(Pager pager) {
    this.pager = pager;
  }

  @JsonProperty("trackedEntities")
  public List<TrackedEntity> getList() {
    return list;
  }

  public void setList(List<TrackedEntity> list) {
    this.list = list;
  }

  @Override
  public String toString() {
    return "TrackedEntities{" +
        "pager=" + pager +
        ", list=" + list +
        '}';
  }
}
