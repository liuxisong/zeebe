/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.0. You may not use this file
 * except in compliance with the Zeebe Community License 1.0.
 */
package io.zeebe.broker.it.client.command;

import static io.zeebe.test.util.TestUtil.waitUntil;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.zeebe.broker.it.util.GrpcClientRule;
import io.zeebe.broker.test.EmbeddedBrokerRule;
import io.zeebe.client.api.ZeebeFuture;
import io.zeebe.client.api.command.ClientException;
import io.zeebe.client.api.response.ActivateJobsResponse;
import io.zeebe.client.api.response.WorkflowInstanceResult;
import io.zeebe.model.bpmn.Bpmn;
import io.zeebe.protocol.record.intent.JobIntent;
import io.zeebe.test.util.BrokerClassRuleHelper;
import io.zeebe.test.util.collection.Maps;
import io.zeebe.test.util.record.RecordingExporter;
import java.util.Map;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

public class CreateWorkflowInstanceWithResultTest {

  private static final EmbeddedBrokerRule BROKER_RULE = new EmbeddedBrokerRule();
  private static final GrpcClientRule CLIENT_RULE = new GrpcClientRule(BROKER_RULE);

  @ClassRule
  public static RuleChain ruleChain = RuleChain.outerRule(BROKER_RULE).around(CLIENT_RULE);

  @Rule public BrokerClassRuleHelper helper = new BrokerClassRuleHelper();

  private String processId;
  private long workflowKey;
  private String jobType;

  @Before
  public void deployProcess() {
    processId = helper.getBpmnProcessId();
    workflowKey =
        CLIENT_RULE.deployWorkflow(Bpmn.createExecutableProcess(processId).startEvent("v1").done());
    jobType = helper.getJobType();
  }

  @Test
  public void shouldCreateWorkflowInstanceAwaitResults() {
    final Map<String, Object> variables = Maps.of(entry("foo", "bar"));
    final WorkflowInstanceResult result =
        CLIENT_RULE
            .getClient()
            .newCreateInstanceCommand()
            .bpmnProcessId(processId)
            .latestVersion()
            .variables(variables)
            .withResult()
            .send()
            .join();

    assertThat(result.getBpmnProcessId()).isEqualTo(processId);
    assertThat(result.getWorkflowKey()).isEqualTo(workflowKey);
    assertThat(result.getVariablesAsMap()).containsExactly(entry("foo", "bar"));
  }

  @Test
  public void shouldCreateWorkflowInstanceAwaitResultsWithNoVariables() {
    // given
    final WorkflowInstanceResult result =
        CLIENT_RULE
            .getClient()
            .newCreateInstanceCommand()
            .workflowKey(workflowKey)
            .withResult()
            .send()
            .join();

    // then
    assertThat(result.getBpmnProcessId()).isEqualTo(processId);
    assertThat(result.getWorkflowKey()).isEqualTo(workflowKey);
    assertThat(result.getVariablesAsMap()).isEmpty();
  }

  @Test
  public void shouldCollectMergedVariables() {
    // given
    deployProcessWithJob();
    final Map<String, Object> variables = Maps.of(entry("foo", "bar"));
    final ZeebeFuture<WorkflowInstanceResult> resultFuture =
        CLIENT_RULE
            .getClient()
            .newCreateInstanceCommand()
            .workflowKey(workflowKey)
            .variables(variables)
            .withResult()
            .send();

    waitUntil(() -> RecordingExporter.jobRecords(JobIntent.CREATED).withType(jobType).exists());

    final ActivateJobsResponse response =
        CLIENT_RULE
            .getClient()
            .newActivateJobsCommand()
            .jobType(jobType)
            .maxJobsToActivate(1)
            .send()
            .join();

    // when
    CLIENT_RULE
        .getClient()
        .newCompleteCommand(response.getJobs().iterator().next().getKey())
        .variables(Map.of("x", "y"))
        .send();

    // then
    final WorkflowInstanceResult result = resultFuture.join();
    assertThat(result.getBpmnProcessId()).isEqualTo(processId);
    assertThat(result.getVariablesAsMap())
        .containsExactlyInAnyOrderEntriesOf(Map.of("foo", "bar", "x", "y"));
  }

  private void deployProcessWithJob() {
    processId = helper.getBpmnProcessId();
    workflowKey =
        CLIENT_RULE.deployWorkflow(
            Bpmn.createExecutableProcess(processId)
                .startEvent("v1")
                .serviceTask(
                    "task",
                    t -> {
                      t.zeebeTaskType(jobType);
                    })
                .endEvent("end")
                .done());
  }

  @Test
  public void shouldReceiveRejectionCreateWorkflowInstanceAwaitResults() {
    final var command =
        CLIENT_RULE.getClient().newCreateInstanceCommand().workflowKey(123L).withResult().send();

    assertThatThrownBy(() -> command.join())
        .isInstanceOf(ClientException.class)
        .hasMessageContaining(
            "Expected to find workflow definition with key '123', but none found");
  }
}
