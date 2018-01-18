package org.opendatakit.dhis2odk2bridge.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hisp.dhis.common.Pager;

import java.util.List;

public class DataSets {
  private Pager pager;
  private List<DataSet> dataSets;

  @JsonProperty
  public Pager getPager() {
    return pager;
  }

  public void setPager(Pager pager) {
    this.pager = pager;
  }

  @JsonProperty
  public List<DataSet> getDataSets() {
    return dataSets;
  }

  public void setDataSets(List<DataSet> dataSets) {
    this.dataSets = dataSets;
  }

  @Override
  public String toString() {
    return "DataSets{" +
        "pager=" + pager +
        ", dataSets=" + dataSets +
        '}';
  }
}
