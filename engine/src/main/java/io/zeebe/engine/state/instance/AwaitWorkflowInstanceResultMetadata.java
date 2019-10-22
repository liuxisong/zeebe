/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.0. You may not use this file
 * except in compliance with the Zeebe Community License 1.0.
 */
package io.zeebe.engine.state.instance;

import io.zeebe.msgpack.property.ArrayProperty;
import io.zeebe.msgpack.property.IntegerProperty;
import io.zeebe.msgpack.property.LongProperty;
import io.zeebe.msgpack.value.StringValue;
import io.zeebe.protocol.impl.record.UnifiedRecordValue;

public class AwaitWorkflowInstanceResultMetadata extends UnifiedRecordValue {

  private final LongProperty requestIdProperty = new LongProperty("requestId", -1);
  private final IntegerProperty requestStreamIdProperty =
      new IntegerProperty("requestStreamId", -1);
  private final ArrayProperty<StringValue> fetchVariablesProperty =
      new ArrayProperty<>("fetchVariables", new StringValue());

  public AwaitWorkflowInstanceResultMetadata() {
    this.declareProperty(requestIdProperty)
        .declareProperty(requestStreamIdProperty)
        .declareProperty(fetchVariablesProperty);
  }

  public long getRequestId() {
    return requestIdProperty.getValue();
  }

  public AwaitWorkflowInstanceResultMetadata setRequestId(long requestId) {
    requestIdProperty.setValue(requestId);
    return this;
  }

  public int getRequestStreamId() {
    return requestStreamIdProperty.getValue();
  }

  public AwaitWorkflowInstanceResultMetadata setRequestStreamId(int requestStreamId) {
    requestStreamIdProperty.setValue(requestStreamId);
    return this;
  }

  public ArrayProperty<StringValue> fetchVariables() {
    return fetchVariablesProperty;
  }

  public AwaitWorkflowInstanceResultMetadata setFetchVariables(
      ArrayProperty<StringValue> variables) {
    fetchVariablesProperty.reset();
    variables.forEach(variable -> fetchVariablesProperty.add().wrap(variable));
    return this;
  }
}
