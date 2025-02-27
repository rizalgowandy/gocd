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

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

public class ArtifactMd5ChecksumsTest {

    private File file;

    @TempDir
    public File temporaryFolder;

    @BeforeEach
    public void setUp() throws IOException {
        file = File.createTempFile("test", null, temporaryFolder);
    }

    @Test
    public void shouldReturnTrueIfTheChecksumFileContainsAGivenPath() {
        Properties properties = new Properties();
        properties.setProperty("first/path", "md5");
        ArtifactMd5Checksums artifactMd5Checksums = new ArtifactMd5Checksums(properties);
        assertThat(artifactMd5Checksums.md5For("first/path")).isEqualTo("md5");
    }

    @Test
    public void shouldReturnNullIfTheChecksumFileDoesNotContainsAGivenPath() {
        Properties properties = new Properties();
        properties.setProperty("first/path", "md5");
        ArtifactMd5Checksums artifactMd5Checksums = new ArtifactMd5Checksums(properties);
        assertThat(artifactMd5Checksums.md5For("foo")).isNull();
    }

    @Test
    public void shouldLoadThePropertiesFromTheGivenFile() throws IOException {
        FileUtils.writeStringToFile(file, "first/path:md5=", UTF_8);
        ArtifactMd5Checksums artifactMd5Checksums = new ArtifactMd5Checksums(file);
        assertThat(artifactMd5Checksums.md5For("first/path")).isEqualTo("md5=");
    }

    @Test
    public void shouldThrowAnExceptionIfTheLoadingFails() {
        try {
            file.delete();
            new ArtifactMd5Checksums(file);
            fail("Should have failed because of an invalid properites file");
        } catch (RuntimeException e) {
            assertThat(e.getCause()).isInstanceOf(IOException.class);
            assertThat(e.getMessage()).isEqualTo(String.format("[Checksum Verification] Could not load the MD5 from the checksum file '%s'", file));
        }
    }
}
