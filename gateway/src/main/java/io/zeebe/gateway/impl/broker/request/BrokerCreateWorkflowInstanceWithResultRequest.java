/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.0. You may not use this file
 * except in compliance with the Zeebe Community License 1.0.
 */
package io.zeebe.gateway.impl.broker.request;

import io.zeebe.protocol.impl.record.value.workflowinstance.WorkflowInstanceCreationWithResultRecord;
import io.zeebe.protocol.impl.record.value.workflowinstance.WorkflowInstanceResultRecord;
import io.zeebe.protocol.record.ValueType;
import io.zeebe.protocol.record.intent.WorkflowInstanceCreationIntent;
import java.util.List;
import org.agrona.DirectBuffer;

public class BrokerCreateWorkflowInstanceWithResultRequest
    extends BrokerExecuteCommand<WorkflowInstanceResultRecord> {
  private final WorkflowInstanceCreationWithResultRecord requestDto =
      new WorkflowInstanceCreationWithResultRecord();

  public BrokerCreateWorkflowInstanceWithResultRequest() {
    super(
        ValueType.WORKFLOW_INSTANCE_CREATION_WITH_RESULT,
        WorkflowInstanceCreationIntent.CREATE_WITH_AWAITING_RESULT);
  }

  public BrokerCreateWorkflowInstanceWithResultRequest setBpmnProcessId(String bpmnProcessId) {
    requestDto.setBpmnProcessId(bpmnProcessId);
    return this;
  }

  public BrokerCreateWorkflowInstanceWithResultRequest setKey(long key) {
    requestDto.setWorkflowKey(key);
    return this;
  }

  public BrokerCreateWorkflowInstanceWithResultRequest setVersion(int version) {
    requestDto.setVersion(version);
    return this;
  }

  public BrokerCreateWorkflowInstanceWithResultRequest setVariables(DirectBuffer variables) {
    requestDto.setVariables(variables);
    return this;
  }

  public BrokerCreateWorkflowInstanceWithResultRequest setFetchVariables(
      List<String> fetchVariables) {
    requestDto.setFetchVariables(fetchVariables);
    return this;
  }

  @Override
  public WorkflowInstanceCreationWithResultRecord getRequestWriter() {
    return requestDto;
  }

  @Override
  protected WorkflowInstanceResultRecord toResponseDto(DirectBuffer buffer) {
    final WorkflowInstanceResultRecord responseDto = new WorkflowInstanceResultRecord();
    responseDto.wrap(buffer);
    return responseDto;
  }

  @Override
  protected boolean isValidResponse() {
    return response.getValueType() == ValueType.WORKFLOW_INSTANCE_RESULT;
  }
}
