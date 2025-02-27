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
package com.thoughtworks.go.plugin.access.analytics.V2;

import com.thoughtworks.go.plugin.access.analytics.V2.models.Capabilities;
import com.thoughtworks.go.plugin.access.analytics.V2.models.SupportedAnalytics;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CapabilitiesTest {

    @Test
    public void shouldDeserializeFromJSON() {
        String json = """
                {
                "supported_analytics": [
                  {"type": "dashboard", "id": "abc",  "title": "Title 1"},
                  {"type": "pipeline", "id": "abc",  "title": "Title 2"},
                  {"type": "vsm", "id": "abc",  "title": "Title 3", "required_params": ["param1", "param2"]}
                ]}""";

        Capabilities capabilities = Capabilities.fromJSON(json);

        assertThat(capabilities.getSupportedAnalytics().size()).isEqualTo(3);
        assertThat(capabilities.getSupportedAnalytics().get(0)).isEqualTo(new SupportedAnalytics("dashboard", "abc", "Title 1"));
        assertThat(capabilities.getSupportedAnalytics().get(1)).isEqualTo(new SupportedAnalytics("pipeline", "abc", "Title 2"));
        assertThat(capabilities.getSupportedAnalytics().get(2)).isEqualTo(new SupportedAnalytics("vsm", "abc", "Title 3"));
    }

    @Test
    public void shouldConvertToDomainCapabilities() {
        String json = """
                {
                "supported_analytics": [
                  {"type": "dashboard", "id": "abc",  "title": "Title 1"},
                  {"type": "pipeline", "id": "abc",  "title": "Title 2"},
                  {"type": "vsm", "id": "abc",  "title": "Title 3", "required_params": ["param1", "param2"]}
                ]}""";

        Capabilities capabilities = Capabilities.fromJSON(json);
        com.thoughtworks.go.plugin.domain.analytics.Capabilities domain = capabilities.toCapabilities();

        assertThat(domain.supportedDashboardAnalytics()).contains(new com.thoughtworks.go.plugin.domain.analytics.SupportedAnalytics("dashboard", "abc", "Title 1"));
        assertThat(domain.supportedPipelineAnalytics()).contains(new com.thoughtworks.go.plugin.domain.analytics.SupportedAnalytics("pipeline", "abc", "Title 2"));
        assertThat(domain.supportedVSMAnalytics()).contains(new com.thoughtworks.go.plugin.domain.analytics.SupportedAnalytics("vsm", "abc", "Title 3"));
    }
}
