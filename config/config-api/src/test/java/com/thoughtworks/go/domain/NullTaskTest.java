/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.domain;

import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

public class NullTaskTest {
    @Test
    public void shouldNotAllowSettingOfConfigAttributes() {
        Task task = new NullTask();
        try {
            task.setConfigAttributes(new HashMap<>());
            fail("should have failed, as configuration of kill-all task is not allowed");
        } catch (UnsupportedOperationException e) {
            assertThat(e.getMessage()).isEqualTo("Not a configurable task");
        }
    }

    @Test
    public void validateShouldReturnNoErrors() {
        Task task = new NullTask();
        task.validate(null);
        assertThat(task.errors().isEmpty()).isTrue();
    }

    @Test
    public void shouldKnowItsType() {
        assertThat(new NullTask().getTaskType()).isEqualTo("null");
    }

    @Test
    public void shouldReturnEmptyPropertiesForDisplay() {
        assertThat(new NullTask().getPropertiesForDisplay().isEmpty()).isTrue();
    }
}
