package org.opendatakit.dhis2sync.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WebMessage {
  private String httpStatus;
  private String httpStatusCode;
  private String status;
  private String message;

  @JsonProperty
  public String getHttpStatus() {
    return httpStatus;
  }

  public void setHttpStatus(String httpStatus) {
    this.httpStatus = httpStatus;
  }

  @JsonProperty
  public String getHttpStatusCode() {
    return httpStatusCode;
  }

  public void setHttpStatusCode(String httpStatusCode) {
    this.httpStatusCode = httpStatusCode;
  }

  @JsonProperty
  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  @JsonProperty
  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  @Override
  public String toString() {
    return "WebMessage{" +
        "httpStatus='" + httpStatus + '\'' +
        ", httpStatusCode='" + httpStatusCode + '\'' +
        ", status='" + status + '\'' +
        ", message='" + message + '\'' +
        '}';
  }
}
