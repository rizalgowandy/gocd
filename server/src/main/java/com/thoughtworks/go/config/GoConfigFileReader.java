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
package com.thoughtworks.go.config;

import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;

public class GoConfigFileReader {
    private final SystemEnvironment systemEnvironment;

    public GoConfigFileReader(SystemEnvironment systemEnvironment) {
        this.systemEnvironment = systemEnvironment;
    }

    public String configXml() throws IOException {
        return FileUtils.readFileToString(fileLocation(), UTF_8);
    }

    public File fileLocation() {
        return new File(systemEnvironment.getCruiseConfigFile());
    }
}
