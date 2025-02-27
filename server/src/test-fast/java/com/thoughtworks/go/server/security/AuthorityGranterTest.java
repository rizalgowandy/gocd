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
package com.thoughtworks.go.server.security;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.SecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AuthorityGranterTest {
    private AuthorityGranter authorityGranter;
    private SecurityService securityService;

    @BeforeEach
    public void setUp() {
        securityService = mock(SecurityService.class);
        authorityGranter = new AuthorityGranter(securityService);
    }

    @Test
    public void shouldGrantTemplateSupervisorRoleToTemplateAdmins() {
        String templateAdmin = "template-admin";
        when(securityService.isAuthorizedToViewAndEditTemplates(new Username(new CaseInsensitiveString(templateAdmin)))).thenReturn(true);
        Set<GrantedAuthority> authorities = authorityGranter.authorities(templateAdmin);
        assertThat(authorities).contains(GoAuthority.ROLE_TEMPLATE_SUPERVISOR.asAuthority());
        assertThat(authorities).doesNotContain(GoAuthority.ROLE_GROUP_SUPERVISOR.asAuthority());
        assertThat(authorities).contains(GoAuthority.ROLE_USER.asAuthority());
    }

    @Test
    public void shouldGrantTemplateViewUserRoleToTemplateViewUsers() {
        String templateViewUser = "templateViewUser";
        when(securityService.isAuthorizedToViewAndEditTemplates(new Username(new CaseInsensitiveString(templateViewUser)))).thenReturn(false);
        when(securityService.isAuthorizedToViewTemplates(new Username(templateViewUser))).thenReturn(true);

        Set<GrantedAuthority> authorities = authorityGranter.authorities(templateViewUser);
        assertThat(authorities).contains(GoAuthority.ROLE_TEMPLATE_VIEW_USER.asAuthority());
        assertThat(authorities).doesNotContain(GoAuthority.ROLE_TEMPLATE_SUPERVISOR.asAuthority());
        assertThat(authorities).doesNotContain(GoAuthority.ROLE_GROUP_SUPERVISOR.asAuthority());
        assertThat(authorities).contains(GoAuthority.ROLE_USER.asAuthority());
    }

    @Test
    public void shouldGrantGroupSupervisorRoleToPipelineGroupAdmins() {
        when(securityService.isUserGroupAdmin(new Username(new CaseInsensitiveString("group-admin")))).thenReturn(true);
        Set<GrantedAuthority> authorities = authorityGranter.authorities("group-admin");
        assertThat(authorities).doesNotContain(GoAuthority.ROLE_SUPERVISOR.asAuthority());
        assertThat(authorities).contains(GoAuthority.ROLE_GROUP_SUPERVISOR.asAuthority());
        assertThat(authorities).contains(GoAuthority.ROLE_USER.asAuthority());
    }

    @Test
    public void shouldGrantSupervisorRoleToUsersWhoAreAdminsAndGroupAdmins() {
        when(securityService.isUserAdmin(new Username(new CaseInsensitiveString("admin")))).thenReturn(true);
        when(securityService.isUserGroupAdmin(new Username(new CaseInsensitiveString("admin")))).thenReturn(true);
        Set<GrantedAuthority> authorities = authorityGranter.authorities("admin");
        assertThat(authorities).contains(GoAuthority.ROLE_SUPERVISOR.asAuthority());
        assertThat(authorities).contains(GoAuthority.ROLE_GROUP_SUPERVISOR.asAuthority());
        assertThat(authorities).contains(GoAuthority.ROLE_USER.asAuthority());
    }

    @Test
    public void shouldGrantRoleUserToUsersWhoAreNotSpecial() {
        when(securityService.isUserAdmin(new Username(new CaseInsensitiveString("admin")))).thenReturn(false);
        when(securityService.isUserGroupAdmin(new Username(new CaseInsensitiveString("admin")))).thenReturn(false);
        Set<GrantedAuthority> authorities = authorityGranter.authorities("admin");
        assertThat(authorities).doesNotContain(GoAuthority.ROLE_SUPERVISOR.asAuthority());
        assertThat(authorities).doesNotContain(GoAuthority.ROLE_GROUP_SUPERVISOR.asAuthority());
        assertThat(authorities).contains(GoAuthority.ROLE_USER.asAuthority());
    }
}
