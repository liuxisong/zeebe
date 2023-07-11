/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */
package io.camunda.operate.zeebeimport.v8_2.processors;

import io.camunda.operate.entities.ProcessEntity;
import io.camunda.operate.exceptions.PersistenceException;
import io.camunda.operate.schema.indices.ProcessIndex;
import io.camunda.operate.schema.templates.ListViewTemplate;
import io.camunda.operate.store.BatchRequest;
import io.camunda.operate.util.ConversionUtils;
import io.camunda.operate.zeebeimport.ElasticsearchQueries;
import io.camunda.operate.zeebeimport.util.XMLUtil;
import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.intent.ProcessIntent;
import io.camunda.zeebe.protocol.record.value.deployment.Process;
import io.camunda.zeebe.protocol.record.value.deployment.ProcessMetadataValue;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ProcessZeebeRecordProcessor {

  private static final Logger logger = LoggerFactory.getLogger(ProcessZeebeRecordProcessor.class);

  private static final Charset CHARSET = StandardCharsets.UTF_8;

  private final static Set<String> STATES = new HashSet<>();
  static {
    STATES.add(ProcessIntent.CREATED.name());
  }

  @Autowired
  private ListViewTemplate listViewTemplate;

  @Autowired
  private ElasticsearchQueries elasticsearchQueries;

  @Autowired
  private ProcessIndex processIndex;

  @Autowired
  private XMLUtil xmlUtil;

  public void processDeploymentRecord(Record record, BatchRequest batchRequest) throws PersistenceException {
    final String intentStr = record.getIntent().name();

    if (STATES.contains(intentStr)) {
      ProcessMetadataValue recordValue = (ProcessMetadataValue)record.getValue();
      persistProcess((Process) recordValue, batchRequest);
    }

  }

  private void persistProcess(Process process, BatchRequest batchRequest) throws PersistenceException {
    final ProcessEntity processEntity = createEntity(process);
    logger.debug("Process: key {}, bpmnProcessId {}", processEntity.getKey(), processEntity.getBpmnProcessId());
    updateFieldsInInstancesFor(processEntity, batchRequest);
    batchRequest.addWithId(processIndex.getFullQualifiedName(),ConversionUtils.toStringOrNull(processEntity.getKey()), processEntity);
  }

  private void updateFieldsInInstancesFor(final ProcessEntity processEntity, BatchRequest batchRequest)
      throws PersistenceException {
    List<Long> processInstanceKeys = elasticsearchQueries.queryProcessInstancesWithEmptyProcessVersion(processEntity.getKey());
    for (Long processInstanceKey : processInstanceKeys) {
      Map<String, Object> updateFields = new HashMap<>();
      updateFields.put(ListViewTemplate.PROCESS_NAME, processEntity.getName());
      updateFields.put(ListViewTemplate.PROCESS_VERSION, processEntity.getVersion());

      batchRequest.update(listViewTemplate.getFullQualifiedName(),processInstanceKey.toString(), updateFields);
    }
  }

  private ProcessEntity createEntity(Process process) {
    ProcessEntity processEntity = new ProcessEntity();

    processEntity.setId(String.valueOf(process.getProcessDefinitionKey()));
    processEntity.setKey(process.getProcessDefinitionKey());
    processEntity.setBpmnProcessId(process.getBpmnProcessId());
    processEntity.setVersion(process.getVersion());

    byte[] byteArray = process.getResource();

    String bpmn = new String(byteArray, CHARSET);
    processEntity.setBpmnXml(bpmn);

    String resourceName = process.getResourceName();
    processEntity.setResourceName(resourceName);

    final Optional<ProcessEntity> diagramData = xmlUtil.extractDiagramData(byteArray);
    if (diagramData.isPresent()) {
      processEntity.setName(diagramData.get().getName());
      processEntity.setFlowNodes(diagramData.get().getFlowNodes());
    }

    return processEntity;
  }
}
