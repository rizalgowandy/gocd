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
package com.thoughtworks.go.plugin.api.response;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultGoPluginApiResponseTest {

    @Test
    public void shouldReturnResponseForBadRequest() {
        DefaultGoPluginApiResponse response = DefaultGoPluginApiResponse.badRequest("responseBody");
        assertThat(response.responseCode()).isEqualTo(400);
        assertThat(response.responseBody()).isEqualTo("responseBody");
    }

    @Test
    public void shouldReturnResponseForIncompleteRequest() {
        DefaultGoPluginApiResponse response = DefaultGoPluginApiResponse.incompleteRequest("responseBody");
        assertThat(response.responseCode()).isEqualTo(412);
        assertThat(response.responseBody()).isEqualTo("responseBody");
    }

    @Test
    public void shouldReturnResponseForErrorRequest() {
        DefaultGoPluginApiResponse response = DefaultGoPluginApiResponse.error("responseBody");
        assertThat(response.responseCode()).isEqualTo(500);
        assertThat(response.responseBody()).isEqualTo("responseBody");
    }

    @Test
    public void shouldReturnResponseForSuccessRequest() {
        DefaultGoPluginApiResponse response = DefaultGoPluginApiResponse.success("responseBody");
        assertThat(response.responseCode()).isEqualTo(200);
        assertThat(response.responseBody()).isEqualTo("responseBody");
    }

}
