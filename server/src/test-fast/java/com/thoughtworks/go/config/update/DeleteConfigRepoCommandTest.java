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
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.thoughtworks.go.helper.MaterialConfigsMother.git;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(MockitoExtension.class)
public class DeleteConfigRepoCommandTest {
    private Username currentUser;
    private BasicCruiseConfig cruiseConfig;
    private ConfigRepoConfig configRepo;
    private String repoId;
    private HttpLocalizedOperationResult result;

    @Mock
    private SecurityService securityService;

    @BeforeEach
    public void setup() {
        currentUser = new Username(new CaseInsensitiveString("user"));
        cruiseConfig = new GoConfigMother().defaultCruiseConfig();
        repoId = "repo-1";
        configRepo = ConfigRepoConfig.createConfigRepoConfig(git("http://foo.git", "master"), "plugin-id", repoId);
        result = new HttpLocalizedOperationResult();
        cruiseConfig.getConfigRepos().add(configRepo);
    }

    @Test
    public void shouldDeleteTheSpecifiedConfigRepo() {
        DeleteConfigRepoCommand command = new DeleteConfigRepoCommand(repoId);
        assertNotNull(cruiseConfig.getConfigRepos().getConfigRepo(repoId));
        command.update(cruiseConfig);
        assertNull(cruiseConfig.getConfigRepos().getConfigRepo(repoId));
    }

    @Test
    public void shouldNotContinueWhenConfigRepoNoLongerExists() {
        cruiseConfig.getConfigRepos().remove(0);
        DeleteConfigRepoCommand command = new DeleteConfigRepoCommand(repoId);
        assertThat(command.canContinue(cruiseConfig)).isFalse();
    }
}
