/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.0. You may not use this file
 * except in compliance with the Zeebe Community License 1.0.
 */
package io.zeebe.protocol.impl.record.value.workflowinstance;

import static io.zeebe.util.buffer.BufferUtil.bufferAsString;
import static io.zeebe.util.buffer.BufferUtil.wrapString;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.zeebe.msgpack.property.ArrayProperty;
import io.zeebe.msgpack.property.ObjectProperty;
import io.zeebe.msgpack.value.StringValue;
import io.zeebe.protocol.impl.record.UnifiedRecordValue;
import io.zeebe.protocol.record.value.WorkflowInstanceCreationRecordValue;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.agrona.DirectBuffer;

public class WorkflowInstanceCreationWithResultRecord extends UnifiedRecordValue
    implements WorkflowInstanceCreationRecordValue {

  private final ObjectProperty<WorkflowInstanceCreationRecord> creationRecordProperty =
      new ObjectProperty<>("creationRecord", new WorkflowInstanceCreationRecord());

  private final ArrayProperty<StringValue> fetchVariablesProperty =
      new ArrayProperty<>("fetchVariables", new StringValue());

  public WorkflowInstanceCreationWithResultRecord() {
    this.declareProperty(creationRecordProperty).declareProperty(fetchVariablesProperty);
  }

  public WorkflowInstanceCreationRecord getCreationRecord() {
    return creationRecordProperty.getValue();
  }

  @Override
  public String getBpmnProcessId() {
    return creationRecordProperty.getValue().getBpmnProcessId();
  }

  public int getVersion() {
    return creationRecordProperty.getValue().getVersion();
  }

  @Override
  public long getWorkflowKey() {
    return creationRecordProperty.getValue().getWorkflowKey();
  }

  public WorkflowInstanceCreationWithResultRecord setWorkflowKey(long key) {
    creationRecordProperty.getValue().setWorkflowKey(key);
    return this;
  }

  public WorkflowInstanceCreationWithResultRecord setVersion(int version) {
    creationRecordProperty.getValue().setVersion(version);
    return this;
  }

  public WorkflowInstanceCreationWithResultRecord setBpmnProcessId(String bpmnProcessId) {
    creationRecordProperty.getValue().setBpmnProcessId(bpmnProcessId);
    return this;
  }

  public WorkflowInstanceCreationWithResultRecord setBpmnProcessId(DirectBuffer bpmnProcessId) {
    creationRecordProperty.getValue().setBpmnProcessId(bpmnProcessId);
    return this;
  }

  public Set<String> getRequestedVariables() {
    final Set<String> set = new HashSet<>();
    fetchVariablesProperty.iterator().forEachRemaining(s -> set.add(bufferAsString(s.getValue())));
    return set;
  }

  @Override
  public long getWorkflowInstanceKey() {
    return creationRecordProperty.getValue().getWorkflowInstanceKey();
  }

  public WorkflowInstanceCreationWithResultRecord setWorkflowInstanceKey(long instanceKey) {
    creationRecordProperty.getValue().setWorkflowInstanceKey(instanceKey);
    return this;
  }

  @Override
  public Map<String, Object> getVariables() {
    return creationRecordProperty.getValue().getVariables();
  }

  public WorkflowInstanceCreationWithResultRecord setVariables(DirectBuffer variables) {
    creationRecordProperty.getValue().setVariables(variables);
    return this;
  }

  public WorkflowInstanceCreationWithResultRecord setFetchVariables(List<String> fetchVariables) {
    fetchVariables.forEach(variable -> fetchVariablesProperty.add().wrap(wrapString(variable)));
    return this;
  }

  public ArrayProperty<StringValue> fetchVariables() {
    return fetchVariablesProperty;
  }

  @JsonIgnore
  public DirectBuffer getBpmnProcessIdBuffer() {
    return creationRecordProperty.getValue().getBpmnProcessIdBuffer();
  }

  @JsonIgnore
  public DirectBuffer getVariablesBuffer() {
    return creationRecordProperty.getValue().getVariablesBuffer();
  }
}
