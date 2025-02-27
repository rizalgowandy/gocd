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
package com.thoughtworks.go.config.security.users;

import com.thoughtworks.go.config.PluginRoleConfig;
import com.thoughtworks.go.config.PluginRoleUsersStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AllowedUsersTest {
    private PluginRoleUsersStore pluginRoleUsersStore;

    @BeforeEach
    public void setUp() {
        pluginRoleUsersStore = PluginRoleUsersStore.instance();
    }

    @AfterEach
    public void tearDown() {
        pluginRoleUsersStore.clearAll();
    }

    @Test
    public void shouldCheckViewPermissionsInACaseInsensitiveWay() {
        AllowedUsers users = new AllowedUsers(Set.of("USER1", "user2", "User3", "AnoTherUsEr"), Collections.emptySet());

        assertThat(users.contains("user1")).isTrue();
        assertThat(users.contains("USER1")).isTrue();
        assertThat(users.contains("User1")).isTrue();
        assertThat(users.contains("USER2")).isTrue();
        assertThat(users.contains("uSEr3")).isTrue();
        assertThat(users.contains("anotheruser")).isTrue();
        assertThat(users.contains("NON-EXISTENT-USER")).isFalse();
    }

    @Test
    public void usersShouldHaveViewPermissionIfTheyBelongToAllowedPluginRoles() {
        PluginRoleConfig admin = new PluginRoleConfig("go_admins", "ldap");

        pluginRoleUsersStore.assignRole("foo", admin);

        AllowedUsers users = new AllowedUsers(Collections.emptySet(), Set.of(admin));

        assertTrue(users.contains("FOO"));
        assertTrue(users.contains("foo"));
        assertFalse(users.contains("bar"));
    }
}
