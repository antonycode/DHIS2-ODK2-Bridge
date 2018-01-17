package org.opendatakit.dhis2sync.common.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.hisp.dhis.common.Pager;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class PagerMixin extends Pager {
  @JsonCreator
  public PagerMixin(@JsonProperty("page") int page,
                    @JsonProperty("total") long total,
                    @JsonProperty("pageSize") int pageSize) {
    super(page, total, pageSize);
  }
}
