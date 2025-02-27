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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.SecurityAuthConfig;
import com.thoughtworks.go.config.SecurityConfig;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.config.update.SecurityAuthConfigCreateCommand;
import com.thoughtworks.go.config.update.SecurityAuthConfigDeleteCommand;
import com.thoughtworks.go.config.update.SecurityAuthConfigUpdateCommand;
import com.thoughtworks.go.domain.config.ConfigurationKey;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.config.ConfigurationValue;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationExtension;
import com.thoughtworks.go.plugin.domain.common.ValidationError;
import com.thoughtworks.go.plugin.domain.common.ValidationResult;
import com.thoughtworks.go.plugin.domain.common.VerifyConnectionResponse;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother.create;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

public class SecurityAuthConfigServiceTest {

    private AuthorizationExtension extension;
    private EntityHashingService hashingService;
    private GoConfigService goConfigService;
    private SecurityAuthConfigService securityAuthConfigService;

    @BeforeEach
    public void setUp() {
        extension = mock(AuthorizationExtension.class);
        hashingService = mock(EntityHashingService.class);
        goConfigService = mock(GoConfigService.class);
        securityAuthConfigService = new SecurityAuthConfigService(goConfigService, hashingService, extension);
    }

    @Test
    public void verifyConnection_shouldSendSuccessResponseOnSuccessfulVerification() {
        VerifyConnectionResponse success = new VerifyConnectionResponse("success", "Connection check passed", new ValidationResult());
        SecurityAuthConfig ldap = new SecurityAuthConfig("ldap", "cd.go.ldap");

        when(extension.verifyConnection("cd.go.ldap", ldap.getConfigurationAsMap(true))).thenReturn(success);

        VerifyConnectionResponse response = securityAuthConfigService.verifyConnection(ldap);

        assertThat(response).isEqualTo(success);
    }

    @Test
    public void verifyConnection_shouldFailForAInvalidAuthConfig() {
        SecurityAuthConfig ldap = new SecurityAuthConfig("ldap", "cd.go.ldap",
                new ConfigurationProperty(new ConfigurationKey("username"), new ConfigurationValue()));
        ValidationResult validationResult = new ValidationResult();
        validationResult.addError(new ValidationError("password", "Password cannot be blank"));
        validationResult.addError(new ValidationError("username", "Username cannot be blank"));

        VerifyConnectionResponse validationFailed = new VerifyConnectionResponse("validation-failed", "Connection check passed", validationResult);

        when(extension.verifyConnection("cd.go.ldap", ldap.getConfigurationAsMap(true))).thenReturn(validationFailed);

        VerifyConnectionResponse response = securityAuthConfigService.verifyConnection(ldap);

        assertThat(response).isEqualTo(validationFailed);
        assertThat(ldap.getProperty("username").errors().get("username").get(0)).isEqualTo("Username cannot be blank");
        assertThat(ldap.getProperty("password").errors().get("password").get(0)).isEqualTo("Password cannot be blank");
    }

    @Test
    public void verifyConnection_shouldSendConnectionFailedResponseOnUnSuccessfulVerification() {
        VerifyConnectionResponse success = new VerifyConnectionResponse("failure", "Connection check failed", new ValidationResult());
        SecurityAuthConfig ldap = new SecurityAuthConfig("ldap", "cd.go.ldap");

        when(extension.verifyConnection("cd.go.ldap", ldap.getConfigurationAsMap(true))).thenReturn(success);

        VerifyConnectionResponse response = securityAuthConfigService.verifyConnection(ldap);

        assertThat(response).isEqualTo(success);
    }

    @Test
    public void verifyConnection_shouldFailInAbsenceOfPlugin() {
        SecurityAuthConfig ldap = new SecurityAuthConfig("ldap", "cd.go.ldap");

        when(extension.verifyConnection("cd.go.ldap", ldap.getConfigurationAsMap(true))).thenThrow(new RecordNotFoundException(""));

        VerifyConnectionResponse response = securityAuthConfigService.verifyConnection(ldap);

        assertThat(response).isEqualTo(new VerifyConnectionResponse("failure", "Unable to verify connection, missing plugin: cd.go.ldap",
                new com.thoughtworks.go.plugin.domain.common.ValidationResult()));
    }

    @Test
    public void shouldAddSecurityAuthConfigToConfig() {
        SecurityAuthConfig securityAuthConfig = new SecurityAuthConfig("ldap", "cd.go.ldap");

        Username username = new Username("username");
        securityAuthConfigService.create(username, securityAuthConfig, new HttpLocalizedOperationResult());

        verify(goConfigService).updateConfig(any(SecurityAuthConfigCreateCommand.class), eq(username));
    }

    @Test
    public void shouldPerformPluginValidationsBeforeAddingSecurityAuthConfig() {
        SecurityAuthConfig securityAuthConfig = new SecurityAuthConfig("ldap", "cd.go.ldap", create("key", false, "value"));

        Username username = new Username("username");
        securityAuthConfigService.create(username, securityAuthConfig, new HttpLocalizedOperationResult());

        verify(extension).validateAuthConfig(securityAuthConfig.getPluginId(), securityAuthConfig.getConfigurationAsMap(true));
    }

    @Test
    public void shouldNotPerformPluginValidationsWhenDeletingSecurityAuthConfig() {
        SecurityAuthConfig securityAuthConfig = new SecurityAuthConfig("ldap", "cd.go.ldap", create("key", false, "value"));

        Username username = new Username("username");
        securityAuthConfigService.delete(username, securityAuthConfig, new HttpLocalizedOperationResult());

        verify(extension, never()).validateAuthConfig(securityAuthConfig.getPluginId(), securityAuthConfig.getConfigurationAsMap(true));
    }

    @Test
    public void shouldAddPluginNotFoundErrorOnConfigForANonExistentPluginIdWhileCreating() {
        SecurityAuthConfig securityAuthConfig = new SecurityAuthConfig("some-id", "non-existent-plugin", create("key", false, "value"));

        Username username = new Username("username");
        when(extension.validateAuthConfig(securityAuthConfig.getPluginId(), securityAuthConfig.getConfigurationAsMap(true))).thenThrow(new RecordNotFoundException("some error"));

        securityAuthConfigService.create(username, securityAuthConfig, new HttpLocalizedOperationResult());

        assertThat(securityAuthConfig.errors().isEmpty()).isFalse();
        assertThat(securityAuthConfig.errors().on("pluginId")).isEqualTo("Plugin with id `non-existent-plugin` is not found.");
    }

    @Test
    public void shouldUpdateExistingSecurityAuthConfigInConfig() {
        SecurityAuthConfig securityAuthConfig = new SecurityAuthConfig("ldap", "cd.go.ldap");

        Username username = new Username("username");
        securityAuthConfigService.update(username, "md5", securityAuthConfig, new HttpLocalizedOperationResult());

        verify(goConfigService).updateConfig(any(SecurityAuthConfigUpdateCommand.class), eq(username));
    }

    @Test
    public void shouldPerformPluginValidationsBeforeUpdatingSecurityAuthConfig() {
        SecurityAuthConfig securityAuthConfig = new SecurityAuthConfig("ldap", "cd.go.ldap", create("key", false, "value"));

        Username username = new Username("username");
        securityAuthConfigService.update(username, "md5", securityAuthConfig, new HttpLocalizedOperationResult());

        verify(extension).validateAuthConfig(securityAuthConfig.getPluginId(), securityAuthConfig.getConfigurationAsMap(true));
    }

    @Test
    public void shouldAddPluginNotFoundErrorOnConfigForANonExistentPluginId() {
        SecurityAuthConfig securityAuthConfig = new SecurityAuthConfig("some-id", "non-existent-plugin", create("key", false, "value"));

        Username username = new Username("username");
        when(extension.validateAuthConfig(securityAuthConfig.getPluginId(), securityAuthConfig.getConfigurationAsMap(true))).thenThrow(new RecordNotFoundException("plugin not found"));

        securityAuthConfigService.update(username, "md5", securityAuthConfig, new HttpLocalizedOperationResult());

        assertThat(securityAuthConfig.errors().isEmpty()).isFalse();
        assertThat(securityAuthConfig.errors().on("pluginId")).isEqualTo("Plugin with id `non-existent-plugin` is not found.");
    }

    @Test
    public void shouldDeleteExistingSecurityAuthConfigInConfig() {
        SecurityAuthConfig securityAuthConfig = new SecurityAuthConfig("ldap", "cd.go.ldap");

        Username username = new Username("username");
        securityAuthConfigService.delete(username, securityAuthConfig, new HttpLocalizedOperationResult());

        verify(goConfigService).updateConfig(any(SecurityAuthConfigDeleteCommand.class), eq(username));
    }

    @Test
    public void shouldGetSecurityAuthConfigByGivenId() {
        SecurityAuthConfig authConfig = new SecurityAuthConfig("ldap", "cd.go.ldap");
        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.securityAuthConfigs().add(authConfig);
        when(goConfigService.security()).thenReturn(securityConfig);

        assertThat(securityAuthConfigService.findProfile("ldap")).isEqualTo(authConfig);
    }

    @Test
    public void shouldGetNullIfSecurityAuthConfigByGivenIdIsNotPresent() {
        when(goConfigService.security()).thenReturn(new SecurityConfig());

        assertNull(securityAuthConfigService.findProfile("ldap"));
    }

    @Test
    public void shouldReturnAnEmptyMapForAuthConfigsIfNonePresent() {
        when(goConfigService.security()).thenReturn(new SecurityConfig());

        assertThat(securityAuthConfigService.listAll().isEmpty()).isTrue();
    }

    @Test
    public void shouldReturnAMapOfSecurityAuthConfigs() {
        SecurityAuthConfig authConfig = new SecurityAuthConfig("ldap", "cd.go.ldap");
        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.securityAuthConfigs().add(authConfig);
        when(goConfigService.security()).thenReturn(securityConfig);

        Map<String, SecurityAuthConfig> expectedMap = new HashMap<>();
        expectedMap.put("ldap", authConfig);

        Map<String, SecurityAuthConfig> authConfigMap = securityAuthConfigService.listAll();
        assertThat(authConfigMap.size()).isEqualTo(1);
        assertThat(authConfigMap).isEqualTo(expectedMap);
    }
}
