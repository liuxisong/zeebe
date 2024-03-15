/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */
package io.camunda.tasklist.es;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.camunda.tasklist.property.TasklistProperties;
import io.camunda.tasklist.schema.IndexSchemaValidator;
import io.camunda.tasklist.schema.indices.IndexDescriptor;
import io.camunda.tasklist.schema.manager.SchemaManager;
import io.camunda.tasklist.util.NoSqlHelper;
import io.camunda.tasklist.util.TasklistZeebeIntegrationTest;
import io.camunda.tasklist.util.TestUtil;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class ElasticSearchSchemaManagementIT extends TasklistZeebeIntegrationTest {

  @Autowired private TasklistProperties tasklistProperties;
  @Autowired private List<IndexDescriptor> indexDescriptors;
  @Autowired private RetryElasticsearchClient retryElasticsearchClient;
  @Autowired private IndexSchemaValidator indexSchemaValidator;
  @Autowired private NoSqlHelper noSqlHelper;
  @Autowired private SchemaManager schemaManager;

  @BeforeAll
  public static void beforeClass() {
    assumeTrue(TestUtil.isElasticSearch());
  }

  @Test
  public void shouldChangeNumberOfReplicas() throws IOException {
    final Integer initialNumberOfReplicas = 0;
    final Integer modifiedNumberOfReplicas = 1;

    tasklistProperties.getElasticsearch().setNumberOfReplicas(initialNumberOfReplicas);
    schemaManager.createSchema();

    for (IndexDescriptor indexDescriptor : indexDescriptors) {
      assertThat(
              noSqlHelper.indexHasAlias(
                  indexDescriptor.getFullQualifiedName(), indexDescriptor.getAlias()))
          .isTrue();
      assertThat(
              retryElasticsearchClient
                  .getIndexSettingsFor(
                      indexDescriptor.getFullQualifiedName(),
                      RetryElasticsearchClient.NUMBERS_OF_REPLICA)
                  .get(RetryElasticsearchClient.NUMBERS_OF_REPLICA))
          .isEqualTo(String.valueOf(initialNumberOfReplicas));
    }

    tasklistProperties.getElasticsearch().setNumberOfReplicas(modifiedNumberOfReplicas);

    assertThat(indexSchemaValidator.schemaExists()).isFalse();

    schemaManager.createSchema();

    for (IndexDescriptor indexDescriptor : indexDescriptors) {
      assertThat(
              noSqlHelper.indexHasAlias(
                  indexDescriptor.getFullQualifiedName(), indexDescriptor.getAlias()))
          .isTrue();
      assertThat(
              retryElasticsearchClient
                  .getIndexSettingsFor(
                      indexDescriptor.getFullQualifiedName(),
                      RetryElasticsearchClient.NUMBERS_OF_REPLICA)
                  .get(RetryElasticsearchClient.NUMBERS_OF_REPLICA))
          .isEqualTo(String.valueOf(modifiedNumberOfReplicas));
    }
  }
}
