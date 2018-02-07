package org.opendatakit.dhis2odk2bridge.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;

public class WebMessageWithSummaries extends WebMessage {
  public ImportSummaries response;

  @JsonProperty
  public ImportSummaries getResponse() {
    return response;
  }

  public void setResponse(ImportSummaries response) {
    this.response = response;
  }

  @Override
  public String toString() {
    return "WebMessageWithSummaries{" +
        "response=" + response +
        "} " + super.toString();
  }
}
