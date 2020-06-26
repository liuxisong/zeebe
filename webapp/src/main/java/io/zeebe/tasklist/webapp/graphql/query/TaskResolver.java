/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package io.zeebe.tasklist.webapp.graphql.query;

import static io.zeebe.tasklist.webapp.graphql.TasklistGraphQLContextBuilder.USER_DATA_LOADER;

import graphql.kickstart.execution.context.GraphQLContext;
import graphql.kickstart.tools.GraphQLResolver;
import graphql.schema.DataFetchingEnvironment;
import io.zeebe.tasklist.webapp.es.cache.WorkflowCache;
import io.zeebe.tasklist.webapp.graphql.entity.TaskDTO;
import io.zeebe.tasklist.webapp.graphql.entity.UserDTO;
import java.util.concurrent.CompletableFuture;
import org.dataloader.DataLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TaskResolver implements GraphQLResolver<TaskDTO> {

  @Autowired private WorkflowCache workflowCache;

  public CompletableFuture<UserDTO> getAssignee(TaskDTO task, DataFetchingEnvironment dfe) {
    if (task.getAssigneeUsername() == null) {
      return null;
    }
    final DataLoader<String, UserDTO> dataloader =
        ((GraphQLContext) dfe.getContext())
            .getDataLoaderRegistry()
            .get()
            .getDataLoader(USER_DATA_LOADER);

    return dataloader.load(task.getAssigneeUsername());
  }

  public String getWorkflowName(TaskDTO task) {
    final String workflowName = workflowCache.getWorkflowName(task.getWorkflowId());
    if (workflowName == null) {
      return task.getBpmnProcessId();
    }
    return workflowName;
  }

  public String getName(TaskDTO task) {
    final String taskName = workflowCache.getTaskName(task.getWorkflowId(), task.getElementId());
    if (taskName == null) {
      return task.getElementId();
    }
    return taskName;
  }
}
