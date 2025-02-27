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

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.config.update.CreatePipelineConfigsCommand;
import com.thoughtworks.go.config.update.DeletePipelineConfigsCommand;
import com.thoughtworks.go.config.update.UpdatePipelineConfigsCommand;
import com.thoughtworks.go.config.validation.GoConfigValidity;
import com.thoughtworks.go.domain.PipelineGroups;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.responses.GoConfigOperationalResponse;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.util.ConfigElementImplementationRegistryMother;
import com.thoughtworks.go.util.ReflectionUtil;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PipelineConfigsServiceTest {

    @Mock
    private GoConfigService goConfigService;
    @Mock
    private SecurityService securityService;
    @Mock
    private EntityHashingService entityHashingService;

    @Mock
    private GoConfigService.XmlPartialSaver<Object> groupSaver;

    private PipelineConfigsService service;
    private Username validUser;
    private HttpLocalizedOperationResult result;

    @BeforeEach
    public void setUp() {
        ConfigCache configCache = new ConfigCache();
        validUser = new Username(new CaseInsensitiveString("validUser"));
        service = new PipelineConfigsService(configCache, ConfigElementImplementationRegistryMother.withNoPlugins(), goConfigService, securityService, entityHashingService);
        result = new HttpLocalizedOperationResult();

        ReflectionUtil.setField(new BasicCruiseConfig(), "md5", "md5");
    }

    @Test
    public void shouldReturnXmlForGivenGroup_onGetXml() {
        CruiseConfig cruiseConfig = new BasicCruiseConfig();
        String groupName = "group_name";
        new GoConfigMother().addPipelineWithGroup(cruiseConfig, groupName, "pipeline_name", "stage_name", "job_name");
        when(goConfigService.getConfigForEditing()).thenReturn(cruiseConfig);
        when(securityService.isUserAdminOfGroup(validUser.getUsername(), groupName)).thenReturn(true);
        String actualXml = service.getXml(groupName, validUser, result);
        String expectedXml = "<pipelines group=\"group_name\">\n  <pipeline name=\"pipeline_name\">\n    <materials>\n      <svn url=\"file:///tmp/foo\" />\n    </materials>\n    <stage name=\"stage_name\">\n      <jobs>\n        <job name=\"job_name\">\n          <tasks>\n            <ant />\n          </tasks>\n        </job>\n      </jobs>\n    </stage>\n  </pipeline>\n</pipelines>";
        assertThat(actualXml).isEqualTo(expectedXml);
        assertThat(result.isSuccessful()).isTrue();
        verify(goConfigService, times(1)).getConfigForEditing();
    }

    @Test
    public void shouldThrowExceptionWhenTheGroupIsNotFound_onGetXml() {
        String groupName = "non-existent-group_name";
        when(securityService.isUserAdminOfGroup(validUser.getUsername(), groupName)).thenThrow(new RecordNotFoundException(EntityType.PipelineGroup, groupName));

        service.getXml(groupName, validUser, result);

        assertThat(result.httpCode()).isEqualTo(404);
        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.message()).isEqualTo(EntityType.PipelineGroup.notFoundMessage(groupName));
        verify(securityService, times(1)).isUserAdminOfGroup(validUser.getUsername(), groupName);
        verify(goConfigService, never()).getConfigForEditing();

    }

    @Test
    public void shouldReturnUnauthorizedResultWhenUserIsNotAuthorizedToViewGroup_onGetXml() {
        String groupName = "some-secret-group";
        Username invalidUser = new Username(new CaseInsensitiveString("invalidUser"));
        when(securityService.isUserAdminOfGroup(invalidUser.getUsername(), groupName)).thenReturn(false);

        String actual = service.getXml(groupName, invalidUser, result);

        assertThat(actual).isNull();
        assertThat(result.httpCode()).isEqualTo(403);
        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.message()).isEqualTo(EntityType.PipelineGroup.forbiddenToEdit(groupName, invalidUser.getUsername()));
        verify(goConfigService, never()).getConfigForEditing();
        verify(securityService, times(1)).isUserAdminOfGroup(invalidUser.getUsername(), groupName);
    }

    @Test
    public void shouldUpdateXmlAndReturnPipelineConfigsIfUserHasEditPermissionsForTheGroupAndUpdateWasSuccessful() throws Exception {
        final String groupName = "group_name";
        when(securityService.isUserAdminOfGroup(validUser.getUsername(), groupName)).thenReturn(true);
        String md5 = "md5";
        when(goConfigService.groupSaver(groupName)).thenReturn(groupSaver);
        String updatedPartial = groupXml();
        when(groupSaver.saveXml(updatedPartial, md5)).thenReturn(GoConfigValidity.valid());

        GoConfigOperationalResponse<PipelineConfigs> actual = service.updateXml(groupName, updatedPartial, md5, validUser, result);
        PipelineConfigs configs = actual.getConfigElement();
        GoConfigValidity validity = actual.getValidity();

        assertThat(configs).isNotNull();
        assertThat(configs.getGroup()).isEqualTo("renamed_group_name");
        assertThat(result.httpCode()).isEqualTo(200);
        assertThat(result.isSuccessful()).isTrue();
        assertThat(validity.isValid()).isTrue();
        verify(groupSaver).saveXml(updatedPartial, md5);
    }

    @Test
    public void shouldReturnUnsuccessfulResultWhenXmlIsInvalid_onUpdateXml() throws Exception {
        String errorMessage = "Can not parse xml.";
        final String groupName = "group_name";
        when(securityService.isUserAdminOfGroup(validUser.getUsername(), groupName)).thenReturn(true);
        String md5 = "md5";
        when(goConfigService.groupSaver(groupName)).thenReturn(groupSaver);
        String updatedPartial = "foobar";
        when(groupSaver.saveXml(updatedPartial, md5)).thenReturn(GoConfigValidity.invalid(errorMessage));

        GoConfigOperationalResponse<PipelineConfigs> actual = service.updateXml(groupName, updatedPartial, md5, validUser, result);
        PipelineConfigs configs = actual.getConfigElement();
        GoConfigValidity validity = actual.getValidity();

        assertThat(configs).isNull();
        assertThat(result.httpCode()).isEqualTo(500);
        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.message()).isEqualTo("Failed to update group 'group_name'. Can not parse xml.");
        assertThat(validity.isValid()).isFalse();
        verify(securityService, times(1)).isUserAdminOfGroup(validUser.getUsername(), groupName);
    }

    @Test
    public void shouldReturnUnsuccessfulResultWhenTheGroupIsNotFound_onUpdateXml() throws Exception {
        String groupName = "non-existent-group_name";
        when(securityService.isUserAdminOfGroup(validUser.getUsername(), groupName)).thenThrow(new RecordNotFoundException(EntityType.PipelineGroup, groupName));

        GoConfigOperationalResponse<PipelineConfigs> actual = service.updateXml(groupName, "", "md5", validUser, result);
        PipelineConfigs configs = actual.getConfigElement();
        GoConfigValidity validity = actual.getValidity();

        assertThat(configs).isNull();
        assertThat(result.httpCode()).isEqualTo(404);
        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.message()).isEqualTo(EntityType.PipelineGroup.notFoundMessage(groupName));
        assertThat(validity.isValid()).isTrue();
        verify(securityService, times(1)).isUserAdminOfGroup(validUser.getUsername(), groupName);
    }

    @Test
    public void shouldReturnUnauthorizedResultWhenUserIsNotAuthorizedToViewGroup_onUpdateXml() throws Exception {
        String groupName = "some-secret-group";
        Username invalidUser = new Username(new CaseInsensitiveString("invalidUser"));
        when(securityService.isUserAdminOfGroup(invalidUser.getUsername(), groupName)).thenReturn(false);

        GoConfigOperationalResponse<PipelineConfigs> actual = service.updateXml(groupName, "", "md5", invalidUser, result);
        PipelineConfigs configElement = actual.getConfigElement();
        GoConfigValidity validity = actual.getValidity();

        assertThat(configElement).isNull();
        assertThat(result.httpCode()).isEqualTo(403);
        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.message()).isEqualTo(EntityType.PipelineGroup.forbiddenToEdit(groupName, invalidUser.getUsername()));
        assertThat(validity.isValid()).isTrue();
        verify(securityService, times(1)).isUserAdminOfGroup(invalidUser.getUsername(), groupName);
    }

    @Test
    public void shouldSetSuccessMessageOnSuccessfulUpdate() throws Exception {
        String groupName = "renamed_group_name";
        String md5 = "md5";
        when(securityService.isUserAdminOfGroup(validUser.getUsername(), groupName)).thenReturn(true);
        when(goConfigService.groupSaver(groupName)).thenReturn(groupSaver);
        when(groupSaver.saveXml(groupXml(), md5)).thenReturn(GoConfigValidity.valid(ConfigSaveState.UPDATED));

        GoConfigOperationalResponse<PipelineConfigs> actual = service.updateXml(groupName, groupXml(), md5, validUser, result);
        GoConfigValidity validity = actual.getValidity();

        assertThat(result.message()).isEqualTo("Saved configuration successfully.");
        assertThat(validity.isValid()).isTrue();
    }

    @Test
    public void shouldSetSuccessMessageOnSuccessfulMerge() throws Exception {
        String groupName = "renamed_group_name";
        when(securityService.isUserAdminOfGroup(validUser.getUsername(), groupName)).thenReturn(true);
        when(goConfigService.groupSaver(groupName)).thenReturn(groupSaver);
        when(groupSaver.saveXml(groupXml(), "md5")).thenReturn(GoConfigValidity.valid(ConfigSaveState.MERGED));

        GoConfigOperationalResponse<PipelineConfigs> actual = service.updateXml(groupName, groupXml(), "md5", validUser, result);
        GoConfigValidity validity = actual.getValidity();

        assertThat(result.message()).isEqualTo(LocalizedMessage.composite("Saved configuration successfully.", "The configuration was modified by someone else, but your changes were merged successfully."));
        assertThat(validity.isValid()).isTrue();
    }

    @Test
    public void shouldThrowUpWithDifferentMessageForMergeExceptions() throws Exception {
        String groupName = "renamed_group_name";
        when(securityService.isUserAdminOfGroup(validUser.getUsername(), groupName)).thenReturn(true);
        when(goConfigService.groupSaver(groupName)).thenReturn(groupSaver);
        when(groupSaver.saveXml(null, "md5")).thenReturn(GoConfigValidity.mergeConflict("some error"));

        GoConfigOperationalResponse<PipelineConfigs> actual = service.updateXml(groupName, null, "md5", validUser, result);
        PipelineConfigs configElement = actual.getConfigElement();
        GoConfigValidity validity = actual.getValidity();

        assertThat(configElement).isNull();
        assertThat(result.isSuccessful()).isFalse();

        GoConfigValidity.InvalidGoConfig invalidGoConfig = (GoConfigValidity.InvalidGoConfig) validity;
        assertThat(invalidGoConfig.isValid()).isFalse();
        assertThat(invalidGoConfig.isMergeConflict()).isTrue();
        assertThat(result.message()).isEqualTo("Someone has modified the configuration and your changes are in conflict. Please review, amend and retry.");
    }

    @Test
    public void shouldThrowUpWithDifferentMessageForPostMergeValidationExceptions() throws Exception {
        String groupName = "renamed_group_name";
        when(securityService.isUserAdminOfGroup(validUser.getUsername(), groupName)).thenReturn(true);
        when(goConfigService.groupSaver(groupName)).thenReturn(groupSaver);
        when(groupSaver.saveXml(null, "md5")).thenReturn(GoConfigValidity.mergePostValidationError("some error"));

        GoConfigOperationalResponse<PipelineConfigs> actual = service.updateXml(groupName, null, "md5", validUser, result);
        PipelineConfigs configElement = actual.getConfigElement();
        GoConfigValidity validity = actual.getValidity();

        assertThat(configElement).isNull();
        assertThat(result.isSuccessful()).isFalse();
        assertThat(validity.isValid()).isFalse();

        GoConfigValidity.InvalidGoConfig invalidGoConfig = (GoConfigValidity.InvalidGoConfig) validity;
        assertThat(invalidGoConfig.isPostValidationError()).isTrue();
        assertThat(result.message()).isEqualTo("Someone has modified the configuration and your changes are in conflict. Please review, amend and retry.");
    }

    @Test
    public void shouldGetPipelineGroupsForUser() {
        PipelineConfig pipelineInGroup1 = new PipelineConfig();
        PipelineConfigs group1 = new BasicPipelineConfigs(pipelineInGroup1);
        group1.setGroup("group1");
        PipelineConfig pipelineInGroup2 = new PipelineConfig();
        PipelineConfigs group2 = new BasicPipelineConfigs(pipelineInGroup2);
        group2.setGroup("group2");
        when(goConfigService.groups()).thenReturn(new PipelineGroups(group1, group2));
        String user = "looser";
        when(securityService.hasViewPermissionForGroup(user, "group1")).thenReturn(true);
        when(securityService.hasViewPermissionForGroup(user, "group2")).thenReturn(false);

        List<PipelineConfigs> gotPipelineGroups = service.getGroupsForUser(user);

        verify(goConfigService, never()).getAllPipelinesForEditInGroup("group1");
        assertThat(gotPipelineGroups).isEqualTo(List.of(group1));
    }

    @Test
    public void shouldInvokeUpdateConfigCommand_updateGroupAuthorization() {
        Authorization authorization = new Authorization(new OperationConfig(new AdminUser(validUser.getUsername())));
        PipelineConfigs pipelineConfigs = new BasicPipelineConfigs("group", authorization);

        when(entityHashingService.hashForEntity(pipelineConfigs)).thenReturn("digest");
        service.updateGroup(validUser, pipelineConfigs, pipelineConfigs, result);

        @SuppressWarnings("unchecked") ArgumentCaptor<EntityConfigUpdateCommand<?>> commandCaptor = ArgumentCaptor.forClass(EntityConfigUpdateCommand.class);
        verify(goConfigService).updateConfig(commandCaptor.capture(), eq(validUser));
        UpdatePipelineConfigsCommand command = (UpdatePipelineConfigsCommand) commandCaptor.getValue();

        assertThat((Object) ReflectionUtil.getField(command, "oldPipelineGroup")).isEqualTo(pipelineConfigs);
        assertThat((Object) ReflectionUtil.getField(command, "newPipelineGroup")).isEqualTo(pipelineConfigs);
        assertThat((Object) ReflectionUtil.getField(command, "digest")).isEqualTo("digest");
        assertThat((Object) ReflectionUtil.getField(command, "result")).isEqualTo(result);
        assertThat((Object) ReflectionUtil.getField(command, "currentUser")).isEqualTo(validUser);
        assertThat((Object) ReflectionUtil.getField(command, "entityHashingService")).isEqualTo(entityHashingService);
        assertThat((Object) ReflectionUtil.getField(command, "securityService")).isEqualTo(securityService);
    }

    @Test
    public void shouldReturnUpdatedPipelineConfigs_whenSuccessful_updateGroupAuthorization() {
        Authorization authorization = new Authorization(new OperationConfig(new AdminUser(validUser.getUsername())));
        PipelineConfigs pipelineConfigs = new BasicPipelineConfigs("group", authorization);
        doAnswer(invocation -> {
            UpdatePipelineConfigsCommand command = invocation.getArgument(0);
            ReflectionUtil.setField(command, "preprocessedPipelineConfigs", pipelineConfigs);
            return null;
        }).when(goConfigService).updateConfig(any(), eq(validUser));

        PipelineConfigs updatedPipelineConfigs = service.updateGroup(validUser, pipelineConfigs, pipelineConfigs, result);

        assertThat(updatedPipelineConfigs.getAuthorization()).isEqualTo(authorization);
        assertThat(updatedPipelineConfigs.getGroup()).isEqualTo("group");
    }

    @Test
    public void shouldReturnUnprocessableEntity_whenConfigInvalid_updateGroupAuthorization() {
        Authorization authorization = new Authorization(new OperationConfig(new AdminUser(validUser.getUsername())));
        PipelineConfigs pipelineConfigs = new BasicPipelineConfigs("group", authorization);
        doThrow(new GoConfigInvalidException(null, "error message")).when(goConfigService).updateConfig(any(), eq(validUser));

        service.updateGroup(validUser, pipelineConfigs, pipelineConfigs, result);

        assertThat(result.httpCode()).isEqualTo(HttpStatus.SC_UNPROCESSABLE_ENTITY);
        assertThat(result.message()).isEqualTo("Validations failed for pipelines 'group'. Error(s): [error message]. Please correct and resubmit.");
    }

    @Test
    public void shouldReturnInternalServerError_whenExceptionThrown_updateGroupAuthorization() {
        Authorization authorization = new Authorization(new OperationConfig(new AdminUser(validUser.getUsername())));
        PipelineConfigs pipelineConfigs = new BasicPipelineConfigs("group", authorization);
        doThrow(new RuntimeException("server error")).when(goConfigService).updateConfig(any(), eq(validUser));

        service.updateGroup(validUser, pipelineConfigs, pipelineConfigs, result);

        assertThat(result.httpCode()).isEqualTo(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        assertThat(result.message()).isEqualTo("Save failed. server error");
    }

    @Test
    public void shouldInvokeDeleteConfigCommand_deleteGroup() {
        Authorization authorization = new Authorization(new OperationConfig(new AdminUser(validUser.getUsername())));
        PipelineConfigs pipelineConfigs = new BasicPipelineConfigs("group", authorization);

        service.deleteGroup(validUser, pipelineConfigs, result);

        @SuppressWarnings("unchecked") ArgumentCaptor<EntityConfigUpdateCommand<?>> commandCaptor = ArgumentCaptor.forClass(EntityConfigUpdateCommand.class);
        verify(goConfigService).updateConfig(commandCaptor.capture(), eq(validUser));
        DeletePipelineConfigsCommand command = (DeletePipelineConfigsCommand) commandCaptor.getValue();

        assertThat((Object) ReflectionUtil.getField(command, "group")).isEqualTo(pipelineConfigs);
        assertThat((Object) ReflectionUtil.getField(command, "result")).isEqualTo(result);
        assertThat((Object) ReflectionUtil.getField(command, "currentUser")).isEqualTo(validUser);
        assertThat((Object) ReflectionUtil.getField(command, "securityService")).isEqualTo(securityService);
    }

    @Test
    public void shouldReturnUnprocessableEntity_whenConfigInvalid_deleteGroup() {
        Authorization authorization = new Authorization(new OperationConfig(new AdminUser(validUser.getUsername())));
        PipelineConfigs pipelineConfigs = new BasicPipelineConfigs("group", authorization);
        doThrow(new GoConfigInvalidException(null, "error message")).when(goConfigService).updateConfig(any(), eq(validUser));

        service.deleteGroup(validUser, pipelineConfigs, result);

        assertThat(result.httpCode()).isEqualTo(HttpStatus.SC_UNPROCESSABLE_ENTITY);
        assertThat(result.message()).isEqualTo("Validations failed for pipelines 'group'. Error(s): [error message]. Please correct and resubmit.");
    }

    @Test
    public void shouldReturnInternalServerError_whenExceptionThrown_deleteGroup() {
        Authorization authorization = new Authorization(new OperationConfig(new AdminUser(validUser.getUsername())));
        PipelineConfigs pipelineConfigs = new BasicPipelineConfigs("group", authorization);
        doThrow(new RuntimeException("server error")).when(goConfigService).updateConfig(any(), eq(validUser));

        service.deleteGroup(validUser, pipelineConfigs, result);

        assertThat(result.httpCode()).isEqualTo(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        assertThat(result.message()).isEqualTo("Save failed. server error");
    }

    @Test
    public void shouldInvokeCreateConfigCommand_createGroup() {
        Authorization authorization = new Authorization(new OperationConfig(new AdminUser(validUser.getUsername())));
        PipelineConfigs pipelineConfigs = new BasicPipelineConfigs("group", authorization);

        service.createGroup(validUser, pipelineConfigs, result);

        @SuppressWarnings("unchecked") ArgumentCaptor<EntityConfigUpdateCommand<?>> commandCaptor = ArgumentCaptor.forClass(EntityConfigUpdateCommand.class);
        verify(goConfigService).updateConfig(commandCaptor.capture(), eq(validUser));
        CreatePipelineConfigsCommand command = (CreatePipelineConfigsCommand) commandCaptor.getValue();

        assertThat((Object) ReflectionUtil.getField(command, "pipelineConfigs")).isEqualTo(pipelineConfigs);
        assertThat((Object) ReflectionUtil.getField(command, "result")).isEqualTo(result);
        assertThat((Object) ReflectionUtil.getField(command, "currentUser")).isEqualTo(validUser);
        assertThat((Object) ReflectionUtil.getField(command, "securityService")).isEqualTo(securityService);
    }

    @Test
    public void shouldReturnUnprocessableEntity_whenConfigInvalid_createGroup() {
        Authorization authorization = new Authorization(new OperationConfig(new AdminUser(validUser.getUsername())));
        PipelineConfigs pipelineConfigs = new BasicPipelineConfigs("group", authorization);
        doThrow(new GoConfigInvalidException(null, "error message")).when(goConfigService).updateConfig(any(), eq(validUser));

        service.createGroup(validUser, pipelineConfigs, result);

        assertThat(result.httpCode()).isEqualTo(HttpStatus.SC_UNPROCESSABLE_ENTITY);
        assertThat(result.message()).isEqualTo("Validations failed for pipelines 'group'. Error(s): [error message]. Please correct and resubmit.");
    }

    @Test
    public void shouldReturnInternalServerError_whenExceptionThrown_createGroup() {
        Authorization authorization = new Authorization(new OperationConfig(new AdminUser(validUser.getUsername())));
        PipelineConfigs pipelineConfigs = new BasicPipelineConfigs("group", authorization);
        doThrow(new RuntimeException("server error")).when(goConfigService).updateConfig(any(), eq(validUser));

        service.createGroup(validUser, pipelineConfigs, result);

        assertThat(result.httpCode()).isEqualTo(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        assertThat(result.message()).isEqualTo("Save failed. server error");
    }

    private String groupXml() {
        return """
                <pipelines group="renamed_group_name">
                  <pipeline name="new_name">
                    <materials>
                      <svn url="file:///tmp/foo" />
                    </materials>
                    <stage name="stage_name">
                      <jobs>
                        <job name="job_name" />
                      </jobs>
                    </stage>
                  </pipeline>
                </pipelines>""";
    }

}
