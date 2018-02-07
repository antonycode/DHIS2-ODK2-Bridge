package org.opendatakit.dhis2odk2bridge.common.model;

import org.hisp.dhis.common.Pager;

import java.util.List;

public interface NameIdPairContainer<T extends NameIdPair> {
  Pager getPager();

  void setPager(Pager pager);

  List<T> getList();

  void setList(List<T> list);
}
