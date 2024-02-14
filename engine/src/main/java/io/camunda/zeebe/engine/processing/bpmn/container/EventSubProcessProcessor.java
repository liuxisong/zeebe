/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.engine.processing.bpmn.container;

import io.camunda.zeebe.engine.processing.bpmn.BpmnElementContainerProcessor;
import io.camunda.zeebe.engine.processing.bpmn.BpmnElementContext;
import io.camunda.zeebe.engine.processing.bpmn.behavior.BpmnBehaviors;
import io.camunda.zeebe.engine.processing.bpmn.behavior.BpmnIncidentBehavior;
import io.camunda.zeebe.engine.processing.bpmn.behavior.BpmnStateBehavior;
import io.camunda.zeebe.engine.processing.bpmn.behavior.BpmnStateTransitionBehavior;
import io.camunda.zeebe.engine.processing.bpmn.behavior.BpmnVariableMappingBehavior;
import io.camunda.zeebe.engine.processing.common.Failure;
import io.camunda.zeebe.engine.processing.deployment.model.element.ExecutableFlowElementContainer;
import io.camunda.zeebe.util.Either;

public final class EventSubProcessProcessor
    implements BpmnElementContainerProcessor<ExecutableFlowElementContainer> {

  private final BpmnStateBehavior stateBehavior;
  private final BpmnStateTransitionBehavior stateTransitionBehavior;
  private final BpmnVariableMappingBehavior variableMappingBehavior;
  private final BpmnIncidentBehavior incidentBehavior;

  public EventSubProcessProcessor(
      final BpmnBehaviors bpmnBehaviors,
      final BpmnStateTransitionBehavior stateTransitionBehavior) {
    stateBehavior = bpmnBehaviors.stateBehavior();
    this.stateTransitionBehavior = stateTransitionBehavior;
    variableMappingBehavior = bpmnBehaviors.variableMappingBehavior();
    incidentBehavior = bpmnBehaviors.incidentBehavior();
  }

  @Override
  public Class<ExecutableFlowElementContainer> getType() {
    return ExecutableFlowElementContainer.class;
  }

  @Override
  public Either<Failure, ?> onActivate(
      final ExecutableFlowElementContainer element, final BpmnElementContext activating) {
    return variableMappingBehavior
        .applyInputMappings(activating, element)
        .thenDo(
            ok -> {
              final var activated =
                  stateTransitionBehavior.transitionToActivated(activating, element.getEventType());
              stateTransitionBehavior.activateChildInstance(
                  activated, element.getStartEvents().getFirst());
            });
  }

  @Override
  public Either<Failure, ?> onComplete(
      final ExecutableFlowElementContainer element, final BpmnElementContext completing) {

    return variableMappingBehavior
        .applyOutputMappings(completing, element)
        .flatMap(ok -> stateTransitionBehavior.transitionToCompleted(element, completing));
  }

  @Override
  public void onTerminate(
      final ExecutableFlowElementContainer element, final BpmnElementContext terminating) {

    incidentBehavior.resolveIncidents(terminating);

    final var noActiveChildInstances = stateTransitionBehavior.terminateChildInstances(terminating);
    if (noActiveChildInstances) {
      onChildTerminated(element, terminating, null);
    }
  }

  @Override
  public void afterExecutionPathCompleted(
      final ExecutableFlowElementContainer element,
      final BpmnElementContext flowScopeContext,
      final BpmnElementContext childContext,
      final Boolean satisfiesCompletionCondition) {
    if (stateBehavior.canBeCompleted(childContext)) {
      stateTransitionBehavior.completeElement(flowScopeContext);
    }
  }

  @Override
  public void onChildTerminated(
      final ExecutableFlowElementContainer element,
      final BpmnElementContext flowScopeContext,
      final BpmnElementContext childContext) {
    final var flowScopeInstance = stateBehavior.getElementInstance(flowScopeContext);

    if (childContext == null || stateBehavior.canBeTerminated(childContext)) {

      if (flowScopeInstance.isTerminating()) {
        // the event subprocess was terminated by its flow scope
        final var terminated =
            stateTransitionBehavior.transitionToTerminated(
                flowScopeContext, element.getEventType());
        stateTransitionBehavior.onElementTerminated(element, terminated);

      } else if (stateBehavior.isInterruptedByTerminateEndEvent(
          flowScopeContext, flowScopeInstance)) {
        // the child element instances were terminated by a terminate end event in the
        // event subprocess
        stateTransitionBehavior.completeElement(flowScopeContext);
      }
    }
  }
}
