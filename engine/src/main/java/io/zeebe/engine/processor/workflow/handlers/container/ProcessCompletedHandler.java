/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.0. You may not use this file
 * except in compliance with the Zeebe Community License 1.0.
 */
package io.zeebe.engine.processor.workflow.handlers.container;

import io.zeebe.engine.processor.TypedResponseWriter;
import io.zeebe.engine.processor.workflow.BpmnStepContext;
import io.zeebe.engine.processor.workflow.deployment.model.element.ExecutableFlowElementContainer;
import io.zeebe.engine.processor.workflow.handlers.element.ElementCompletedHandler;
import io.zeebe.engine.state.instance.AwaitWorkflowInstanceResultMetadata;
import io.zeebe.engine.state.instance.VariablesState;
import io.zeebe.protocol.impl.record.value.workflowinstance.WorkflowInstanceResultRecord;
import io.zeebe.protocol.record.ValueType;
import io.zeebe.protocol.record.intent.WorkflowInstanceIntent;
import io.zeebe.protocol.record.intent.WorkflowInstanceResultIntent;
import java.util.HashSet;
import java.util.Set;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

public class ProcessCompletedHandler
    extends ElementCompletedHandler<ExecutableFlowElementContainer> {

  @Override
  protected boolean handleState(BpmnStepContext<ExecutableFlowElementContainer> context) {

    final var record = context.getValue();
    final var parentWorkflowInstanceKey = record.getParentWorkflowInstanceKey();
    final var parentElementInstanceKey = record.getParentElementInstanceKey();

    if (parentWorkflowInstanceKey > 0) {
      // workflow instance is created by a call activity

      final var parentElementInstance =
          context.getStateDb().getElementInstanceState().getInstance(parentElementInstanceKey);

      if (parentElementInstance != null && parentElementInstance.isActive()) {
        // complete the corresponding call activity

        context
            .getOutput()
            .appendFollowUpEvent(
                parentElementInstanceKey,
                WorkflowInstanceIntent.ELEMENT_COMPLETING,
                parentElementInstance.getValue());

        // propagate the variables to the parent
        final var variablesState = context.getElementInstanceState().getVariablesState();

        final var variables = variablesState.getVariablesAsDocument(context.getKey());
        variablesState.setTemporaryVariables(parentElementInstanceKey, variables);
      }
    } else {
      sendProcessCompletedResult(context);
    }

    return super.handleState(context);
  }

  private void sendProcessCompletedResult(BpmnStepContext context) {

    final long elementInstanceKey = context.getElementInstance().getKey();
    final AwaitWorkflowInstanceResultMetadata requestMetadata =
        context.getElementInstanceState().getAwaitResultRequestMetadata(elementInstanceKey);
    if (requestMetadata != null) {
      final DirectBuffer variablesAsDocument =
          collectVariables(
              context.getElementInstanceState().getVariablesState(),
              requestMetadata,
              elementInstanceKey);

      final WorkflowInstanceResultRecord resultRecord = new WorkflowInstanceResultRecord();
      resultRecord
          .setWorkflowInstanceKey(context.getValue().getWorkflowInstanceKey())
          .setWorkflowKey(context.getValue().getWorkflowKey())
          .setVariables(variablesAsDocument)
          .setBpmnProcessId(context.getValue().getBpmnProcessId())
          .setVersion(context.getValue().getVersion());

      final TypedResponseWriter responseWriter = context.getOutput().getResponseWriter();
      responseWriter.writeResponse(
          context.getKey(),
          WorkflowInstanceResultIntent.COMPLETED,
          resultRecord,
          ValueType.WORKFLOW_INSTANCE_RESULT,
          requestMetadata.getRequestId(),
          requestMetadata.getRequestStreamId());

      context.getSideEffect().add(responseWriter::flush);
    }
  }

  private DirectBuffer collectVariables(
      VariablesState variablesState,
      AwaitWorkflowInstanceResultMetadata requestMetadata,
      long elementInstanceKey) {

    final Set<DirectBuffer> variablesToCollect = new HashSet<>();
    if (requestMetadata.fetchVariables().iterator().hasNext()) {
      requestMetadata
          .fetchVariables()
          .forEach(
              variable -> {
                final MutableDirectBuffer nameCopy =
                    new UnsafeBuffer(new byte[variable.getValue().capacity()]);
                nameCopy.putBytes(0, variable.getValue(), 0, variable.getValue().capacity());
                variablesToCollect.add(nameCopy);
              });

      return variablesState.getVariablesAsDocument(elementInstanceKey, variablesToCollect);
    } else {
      return variablesState.getVariablesAsDocument(elementInstanceKey);
    }
  }
}
