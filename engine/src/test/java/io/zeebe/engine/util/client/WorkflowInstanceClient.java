/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.0. You may not use this file
 * except in compliance with the Zeebe Community License 1.0.
 */
package io.zeebe.engine.util.client;

import static io.zeebe.util.buffer.BufferUtil.wrapString;

import io.zeebe.engine.util.StreamProcessorRule;
import io.zeebe.msgpack.property.ArrayProperty;
import io.zeebe.msgpack.value.StringValue;
import io.zeebe.protocol.impl.record.value.workflowinstance.WorkflowInstanceCreationRecord;
import io.zeebe.protocol.impl.record.value.workflowinstance.WorkflowInstanceCreationWithResultRecord;
import io.zeebe.protocol.impl.record.value.workflowinstance.WorkflowInstanceRecord;
import io.zeebe.protocol.record.Record;
import io.zeebe.protocol.record.intent.WorkflowInstanceCreationIntent;
import io.zeebe.protocol.record.intent.WorkflowInstanceIntent;
import io.zeebe.protocol.record.value.WorkflowInstanceRecordValue;
import io.zeebe.test.util.MsgPackUtil;
import io.zeebe.test.util.record.RecordingExporter;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class WorkflowInstanceClient {

  private final StreamProcessorRule environmentRule;

  public WorkflowInstanceClient(StreamProcessorRule environmentRule) {
    this.environmentRule = environmentRule;
  }

  public WorkflowInstanceCreationClient ofBpmnProcessId(String bpmnProcessId) {
    return new WorkflowInstanceCreationClient(environmentRule, bpmnProcessId);
  }

  public ExistingInstanceClient withInstanceKey(long workflowInstanceKey) {
    return new ExistingInstanceClient(environmentRule, workflowInstanceKey);
  }

  public static class WorkflowInstanceCreationClient {

    private final StreamProcessorRule environmentRule;
    private final WorkflowInstanceCreationRecord workflowInstanceCreationRecord;

    public WorkflowInstanceCreationClient(
        StreamProcessorRule environmentRule, String bpmnProcessId) {
      this.environmentRule = environmentRule;
      this.workflowInstanceCreationRecord = new WorkflowInstanceCreationRecord();
      workflowInstanceCreationRecord.setBpmnProcessId(bpmnProcessId);
    }

    public WorkflowInstanceCreationClient withVariables(Map<String, Object> variables) {
      workflowInstanceCreationRecord.setVariables(MsgPackUtil.asMsgPack(variables));
      return this;
    }

    public WorkflowInstanceCreationClient withVariables(String variables) {
      workflowInstanceCreationRecord.setVariables(MsgPackUtil.asMsgPack(variables));
      return this;
    }

    public WorkflowInstanceCreationClient withVariable(String key, Object value) {
      workflowInstanceCreationRecord.setVariables(MsgPackUtil.asMsgPack(key, value));
      return this;
    }

    public WorkflowInstanceCreationWithResultClient withResult() {
      return new WorkflowInstanceCreationWithResultClient(
          environmentRule, workflowInstanceCreationRecord);
    }

    public long create() {
      final long position =
          environmentRule.writeCommand(
              WorkflowInstanceCreationIntent.CREATE, workflowInstanceCreationRecord);

      return RecordingExporter.workflowInstanceCreationRecords()
          .withIntent(WorkflowInstanceCreationIntent.CREATED)
          .withSourceRecordPosition(position)
          .getFirst()
          .getValue()
          .getWorkflowInstanceKey();
    }

    public WorkflowInstanceCreationWithResultClient withResult(Set<String> fetchVariables) {
      return new WorkflowInstanceCreationWithResultClient(
          environmentRule, workflowInstanceCreationRecord, fetchVariables);
    }
  }

  public static class WorkflowInstanceCreationWithResultClient {
    private StreamProcessorRule environmentRule;
    private final WorkflowInstanceCreationWithResultRecord record =
        new WorkflowInstanceCreationWithResultRecord();
    private long requestId = 1L;
    private int requestStreamId = 1;

    public WorkflowInstanceCreationWithResultClient(
        StreamProcessorRule environmentRule, WorkflowInstanceCreationRecord creationRecord) {
      this(environmentRule, creationRecord, Set.of());
    }

    public WorkflowInstanceCreationWithResultClient(
        StreamProcessorRule environmentRule,
        WorkflowInstanceCreationRecord creationRecord,
        Set<String> fetchVariables) {
      this.environmentRule = environmentRule;
      record
          .setBpmnProcessId(creationRecord.getBpmnProcessId())
          .setVariables(creationRecord.getVariablesBuffer())
          .setVersion(creationRecord.getVersion())
          .setWorkflowKey(creationRecord.getWorkflowKey());

      final ArrayProperty<StringValue> variablesToCollect = record.fetchVariables();
      fetchVariables.forEach(variable -> variablesToCollect.add().wrap(wrapString(variable)));
    }

    public WorkflowInstanceCreationWithResultClient withRequestId(long requestId) {
      this.requestId = requestId;
      return this;
    }

    public WorkflowInstanceCreationWithResultClient withRequestStreamId(int requestStreamId) {
      this.requestStreamId = requestStreamId;
      return this;
    }

    public long create() {
      final long position =
          environmentRule.writeCommand(
              requestStreamId,
              requestId,
              WorkflowInstanceCreationIntent.CREATE_WITH_AWAITING_RESULT,
              record);

      return RecordingExporter.workflowInstanceCreationRecords()
          .withIntent(WorkflowInstanceCreationIntent.CREATED)
          .withSourceRecordPosition(position)
          .getFirst()
          .getValue()
          .getWorkflowInstanceKey();
    }

    public void asyncCreate() {
      environmentRule.writeCommand(
          requestStreamId,
          requestId,
          WorkflowInstanceCreationIntent.CREATE_WITH_AWAITING_RESULT,
          record);
    }
  }

  public static class ExistingInstanceClient {

    public static final Function<Long, Record<WorkflowInstanceRecordValue>> SUCCESS_EXPECTATION =
        (workflowInstanceKey) ->
            RecordingExporter.workflowInstanceRecords()
                .withRecordKey(workflowInstanceKey)
                .withIntent(WorkflowInstanceIntent.ELEMENT_TERMINATED)
                .withWorkflowInstanceKey(workflowInstanceKey)
                .getFirst();

    public static final Function<Long, Record<WorkflowInstanceRecordValue>> REJECTION_EXPECTATION =
        (workflowInstanceKey) ->
            RecordingExporter.workflowInstanceRecords()
                .onlyCommandRejections()
                .withIntent(WorkflowInstanceIntent.CANCEL)
                .withRecordKey(workflowInstanceKey)
                .withWorkflowInstanceKey(workflowInstanceKey)
                .getFirst();

    private static final int DEFAULT_PARTITION = -1;
    private final StreamProcessorRule environmentRule;
    private final long workflowInstanceKey;

    private int partition = DEFAULT_PARTITION;
    private Function<Long, Record<WorkflowInstanceRecordValue>> expectation = SUCCESS_EXPECTATION;

    public ExistingInstanceClient(StreamProcessorRule environmentRule, long workflowInstanceKey) {
      this.environmentRule = environmentRule;
      this.workflowInstanceKey = workflowInstanceKey;
    }

    public ExistingInstanceClient onPartition(int partition) {
      this.partition = partition;
      return this;
    }

    public ExistingInstanceClient expectRejection() {
      expectation = REJECTION_EXPECTATION;
      return this;
    }

    public Record<WorkflowInstanceRecordValue> cancel() {
      if (partition == DEFAULT_PARTITION) {
        partition =
            RecordingExporter.workflowInstanceRecords()
                .withWorkflowInstanceKey(workflowInstanceKey)
                .getFirst()
                .getPartitionId();
      }

      environmentRule.writeCommandOnPartition(
          partition,
          workflowInstanceKey,
          WorkflowInstanceIntent.CANCEL,
          new WorkflowInstanceRecord().setWorkflowInstanceKey(workflowInstanceKey));

      return expectation.apply(workflowInstanceKey);
    }
  }
}
