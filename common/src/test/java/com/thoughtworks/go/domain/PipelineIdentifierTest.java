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

import static org.assertj.core.api.Assertions.assertThat;

public class PipelineIdentifierTest {

    @Test
    public void shouldUseCounterIfItExists() {
        PipelineIdentifier identifier = new PipelineIdentifier("cruise", 1, "label-1");
        assertThat(identifier.pipelineLocator()).isEqualTo("cruise/1");
    }

    @Test
    public void shouldUseLabelForDisplay() {
        PipelineIdentifier identifier = new PipelineIdentifier("cruise", 1, "label-1");
        assertThat(identifier.pipelineLocatorForDisplay()).isEqualTo("cruise/label-1");
    }

    @Test
    public void shouldReturnURN() {
        PipelineIdentifier identifier = new PipelineIdentifier("cruise", 1, "label-1");
        assertThat(identifier.asURN()).isEqualTo("urn:x-go.studios.thoughtworks.com:job-id:cruise:1");
    }
}
