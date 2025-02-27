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
package com.thoughtworks.go.domain.valuestreammap;

import com.thoughtworks.go.domain.materials.Modification;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class SCMRevisionTest {
    @Test
    public void shouldGetModificationDetails() {
        Date dateTime = new Date();
        Modification modification = new Modification("user", "comment", "email", dateTime, "r1");
        SCMRevision scmRevision = new SCMRevision(modification);
        assertThat(scmRevision.getComment()).isEqualTo("comment");
        assertThat(scmRevision.getUser()).isEqualTo("user");
        assertThat(scmRevision.getRevisionString()).isEqualTo("r1");
        assertThat(scmRevision.getModifiedTime()).isEqualTo(dateTime);
    }

    @Test
    public void shouldRenderUsernameForDisplay() {
        Modification modification = new Modification(null, "comment", "email", new Date(), "r1");
        SCMRevision scmRevision = new SCMRevision(modification);
        assertThat(scmRevision.getUser()).isEqualTo(modification.getUserDisplayName());
    }
}
