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
package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.SecretConfig;
import com.thoughtworks.go.config.SecretConfigs;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.domain.config.ConfigurationKey;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.config.ConfigurationValue;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.thoughtworks.go.i18n.LocalizedMessage.forbiddenToEdit;
import static com.thoughtworks.go.serverhealth.HealthStateType.forbidden;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RuleAwarePluginProfileCommandTest {
    private Username currentUser;
    private GoConfigService goConfigService;
    private BasicCruiseConfig cruiseConfig;

    @BeforeEach
    public void setUp() {
        currentUser = new Username("bob");
        goConfigService = mock(GoConfigService.class);
        cruiseConfig = GoConfigMother.defaultCruiseConfig();
    }

    @Test
    public void shouldNotContinueWithConfigSaveIfUserIsUnauthorized() {
        SecretConfig securityAuthConfig = new SecretConfig("ldap", "cd.go.ldap");
        when(goConfigService.isUserAdmin(currentUser)).thenReturn(false);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        RuleAwarePluginProfileCommand<SecretConfig, SecretConfigs> command = new StubSecretConfigCommand(goConfigService, securityAuthConfig, currentUser, result);
        assertThat(cruiseConfig.server().security().securityAuthConfigs().find("foo")).isNull();

        assertThat(command.canContinue(cruiseConfig)).isFalse();
        assertThat(result.message()).isEqualTo("Unauthorized to edit.");
    }

    @Test
    public void shouldContinueWithConfigSaveIfUserIsAuthorized() {
        SecretConfig securityAuthConfig = new SecretConfig("ldap", "cd.go.ldap");
        when(goConfigService.isUserAdmin(currentUser)).thenReturn(true);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        RuleAwarePluginProfileCommand<SecretConfig, SecretConfigs> command = new StubSecretConfigCommand(goConfigService, securityAuthConfig, currentUser, result);
        assertThat(cruiseConfig.server().security().securityAuthConfigs().find("ldap")).isNull();

        assertThat(command.canContinue(cruiseConfig)).isTrue();
        assertThat(result.httpCode()).isEqualTo(200);
    }

    @Test
    public void shouldValidateIfSecretConfigIdIsNull() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        SecretConfig securityAuthConfig = new SecretConfig(null, "some-plugin", new ConfigurationProperty(new ConfigurationKey("key"), new ConfigurationValue("value")));
        cruiseConfig.getSecretConfigs().add(securityAuthConfig);

        RuleAwarePluginProfileCommand<SecretConfig, SecretConfigs> command = new StubSecretConfigCommand(goConfigService, securityAuthConfig, currentUser, result);
        assertThatThrownBy(() -> command.isValid(cruiseConfig))
                .hasMessageContaining(EntityType.ElasticProfile.idCannotBeBlank());
    }

    @Test
    public void shouldContinueWithConfigSaveIfUserIsAdmin() {
        SecretConfig securityAuthConfig = new SecretConfig("ldap", "cd.go.ldap");

        when(goConfigService.isUserAdmin(currentUser)).thenReturn(true);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        RuleAwarePluginProfileCommand<SecretConfig, SecretConfigs> command = new StubSecretConfigCommand(goConfigService, securityAuthConfig, currentUser, result);
        assertThat(cruiseConfig.server().security().securityAuthConfigs().find("ldap")).isNull();

        assertThat(command.canContinue(cruiseConfig)).isTrue();
    }

    private class StubSecretConfigCommand extends RuleAwarePluginProfileCommand<SecretConfig, SecretConfigs> {

        public StubSecretConfigCommand(GoConfigService goConfigService, SecretConfig profile, Username currentUser, LocalizedOperationResult result) {
            super(goConfigService, profile, currentUser, result);
        }

        @Override
        public void update(CruiseConfig preprocessedConfig) {

        }

        @Override
        public boolean isValid(CruiseConfig preprocessedConfig) {
            return isValidForCreateOrUpdate(preprocessedConfig);
        }

        @Override
        protected SecretConfigs getPluginProfiles(CruiseConfig preprocessedConfig) {
            return null;
        }

        @Override
        public ValidationResult validateUsingExtension(String pluginId, Map<String, String> configuration) {
            return null;
        }

        @Override
        protected EntityType getObjectDescriptor() {
            return EntityType.ElasticProfile;
        }

        @Override
        protected final boolean isAuthorized() {
            if (goConfigService.isUserAdmin(currentUser)) {
                return true;
            }
            result.forbidden(forbiddenToEdit(), forbidden());
            return false;
        }
    }
}
