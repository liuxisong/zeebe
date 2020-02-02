/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.operate.zeebeimport.v22.record;

import io.zeebe.protocol.record.RecordValue;
import io.zeebe.protocol.record.RecordValueWithVariables;
import java.util.Map;
import java.util.Objects;

public abstract class RecordValueWithPayloadImpl implements RecordValue, RecordValueWithVariables {
  private Map<String, Object> variables;

  public RecordValueWithPayloadImpl() {
  }

  @Override
  public Map<String, Object> getVariables() {
    return variables;
  }

  public void setVariables(Map<String, Object> variables) {
    this.variables = variables;
  }

  @Override
  public String toJson() {
    throw new UnsupportedOperationException("toJson operation is not supported");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    RecordValueWithPayloadImpl that = (RecordValueWithPayloadImpl) o;

    return Objects.equals(variables, that.variables);
  }

  @Override
  public int hashCode() {
    return variables != null ? variables.hashCode() : 0;
  }
}
