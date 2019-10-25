/*
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.zeebe.client.impl.response;

import io.zeebe.client.impl.ZeebeObjectMapper;
import java.util.Map;

public class Variables {

  private final String variables;
  private final ZeebeObjectMapper objectMapper;

  public Variables(String variables, ZeebeObjectMapper objectMapper) {
    this.variables = variables;
    this.objectMapper = objectMapper;
  }

  public String getVariables() {
    return variables;
  }

  public Map<String, Object> getVariablesAsMap() {
    return objectMapper.fromJsonAsMap(variables);
  }

  public <T> T getVariablesAsType(Class<T> variableType) {
    return objectMapper.fromJson(variables, variableType);
  }
}
