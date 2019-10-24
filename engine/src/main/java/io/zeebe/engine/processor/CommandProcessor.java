/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.0. You may not use this file
 * except in compliance with the Zeebe Community License 1.0.
 */
package io.zeebe.engine.processor;

import io.zeebe.protocol.impl.record.UnifiedRecordValue;
import io.zeebe.protocol.record.RejectionType;
import io.zeebe.protocol.record.intent.Intent;

/**
 * High-level record processor abstraction that implements the common behavior of most
 * command-handling processors.
 */
public interface CommandProcessor<T extends UnifiedRecordValue, R extends UnifiedRecordValue> {

  default boolean onCommand(TypedRecord<T> command, CommandControl<R> commandControl) {
    return true;
  }

  default boolean onCommand(
      TypedRecord<T> command, CommandControl<R> commandControl, TypedStreamWriter streamWriter) {
    return onCommand(command, commandControl);
  }

  interface CommandControl<R> {
    /** @return the key of the entity */
    long accept(Intent newState, R updatedValue);

    void reject(RejectionType type, String reason);
  }
}
