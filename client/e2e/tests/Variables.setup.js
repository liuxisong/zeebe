/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */

import {deploy, createSingleInstance} from '../setup-utils';

export async function setup() {
  await deploy(['./tests/resources/onlyIncidentsProcess_v_1.bpmn']);
  const instanceId = await createSingleInstance('onlyIncidentsProcess', 1, {
    testData: 'something',
  });
  return {instanceId};
}
