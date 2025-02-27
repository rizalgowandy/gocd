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
package com.thoughtworks.go.server.materials.postcommit.git;

import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.util.command.UrlArgument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class GitPostCommitHookImplementerTest {

    private GitPostCommitHookImplementer implementer;

    @BeforeEach
    public void setUp() {
        implementer = new GitPostCommitHookImplementer();
    }

    @Test
    public void shouldReturnListOfMaterialMatchingThePayloadURL() {
        GitMaterial material1 = mock(GitMaterial.class);
        when(material1.getUrlArgument()).thenReturn(new UrlArgument("https://other_repo.local.git"));
        GitMaterial material2 = mock(GitMaterial.class);
        when(material2.getUrlArgument()).thenReturn(new UrlArgument("https://other_repo.local.git"));
        GitMaterial material3 = mock(GitMaterial.class);
        when(material3.getUrlArgument()).thenReturn(new UrlArgument("https://machine.local.git"));
        GitMaterial material4 = mock(GitMaterial.class);
        when(material4.getUrlArgument()).thenReturn(new UrlArgument("https://machine.local.git"));
        Set<Material> materials = Set.of(material1, material2, material3, material4);
        Map<String, String> params = new HashMap<>();
        params.put(GitPostCommitHookImplementer.REPO_URL_PARAM_KEY, "https://machine.local.git");

        Set<Material> actual = implementer.prune(materials, params);

        assertThat(actual.size()).isEqualTo(2);
        assertThat(actual).contains(material3);
        assertThat(actual).contains(material4);

        verify(material1).getUrlArgument();
        verify(material2).getUrlArgument();
        verify(material3).getUrlArgument();
        verify(material4).getUrlArgument();
    }

    @Test
    public void shouldQueryOnlyGitMaterialsWhilePruning() {
        SvnMaterial material1 = mock(SvnMaterial.class);
        Set<Material> materials = Set.of(material1);
        Map<String, String> params = new HashMap<>();
        params.put(GitPostCommitHookImplementer.REPO_URL_PARAM_KEY, "https://machine.local.git");

        Set<Material> actual = implementer.prune(materials, params);

        assertThat(actual.size()).isEqualTo(0);

        verifyNoMoreInteractions(material1);
    }

    @Test
    public void shouldReturnEmptyListIfParamHasNoValueForRepoURL() {
        GitMaterial material1 = mock(GitMaterial.class);
        when(material1.getUrlArgument()).thenReturn(new UrlArgument("https://machine.local.git"));
        Set<Material> materials = Set.of(material1);
        Map<String, String> params = new HashMap<>();
        params.put(GitPostCommitHookImplementer.REPO_URL_PARAM_KEY, "");

        Set<Material> actual = implementer.prune(materials, params);

        assertThat(actual.size()).isEqualTo(0);

        verifyNoMoreInteractions(material1);
    }

    @Test
    public void shouldReturnEmptyListIfParamIsMissingForRepoURL() {
        GitMaterial material1 = mock(GitMaterial.class);
        when(material1.getUrlArgument()).thenReturn(new UrlArgument("https://machine.local.git"));
        Set<Material> materials = Set.of(material1);

        Set<Material> actual = implementer.prune(materials, new HashMap<>());

        assertThat(actual.size()).isEqualTo(0);

        verifyNoMoreInteractions(material1);
    }

    @Test
    public void shouldReturnTrueWhenURLIsAnExactMatch() {
        boolean isEqual = implementer.isUrlEqual("http://repo-url.git", new GitMaterial("http://repo-url.git"));
        assertThat(isEqual).isTrue();
    }

    @Test
    public void shouldReturnTrueWhenBasicAuthIsProvidedInURL() {
        boolean isEqual = implementer.isUrlEqual("http://repo-url.git", new GitMaterial("http://user:passW)rD@repo-url.git"));
        assertThat(isEqual).isTrue();
    }

    @Test
    public void shouldReturnTrueWhenBasicAuthWithoutPasswordIsProvidedInURL() {
        boolean isEqual = implementer.isUrlEqual("http://repo-url.git", new GitMaterial("http://user:@repo-url.git"));
        assertThat(isEqual).isTrue();
    }

    @Test
    public void shouldReturnTrueWhenBasicAuthWithOnlyUsernameIsProvidedInURL() {
        boolean isEqual = implementer.isUrlEqual("http://repo-url.git", new GitMaterial("http://user@repo-url.git"));
        assertThat(isEqual).isTrue();
    }

    @Test
    public void shouldReturnFalseWhenProtocolIsDifferent() {
        boolean isEqual = implementer.isUrlEqual("http://repo-url.git", new GitMaterial("https://repo-url.git"));
        assertThat(isEqual).isFalse();
    }

    @Test
    public void shouldReturnFalseWhenNoValidatorCouldParseUrl() {
        boolean isEqual = implementer.isUrlEqual("http://repo-url.git", new GitMaterial("something.completely.random"));
        assertThat(isEqual).isFalse();
    }

    @Test
    public void shouldReturnFalseUpWheNoProtocolIsGiven() {
        boolean isEqual = implementer.isUrlEqual("http://repo-url.git", new GitMaterial("repo-url.git"));
        assertThat(isEqual).isFalse();
    }

    @Test
    public void shouldReturnFalseForEmptyURLField() {
        boolean isEqual = implementer.isUrlEqual("http://repo-url.git", new GitMaterial("http://"));
        assertThat(isEqual).isFalse();
    }

    @Test
    public void shouldReturnFalseForEmptyURLFieldWithAuth() {
        boolean isEqual = implementer.isUrlEqual("http://repo-url.git", new GitMaterial("http://user:password@"));
        assertThat(isEqual).isFalse();
    }

    @Test
    public void shouldMatchFileBasedAccessWithoutAuth() {
        boolean isEqual = implementer.isUrlEqual("/tmp/foo/repo-git", new GitMaterial("/tmp/foo/repo-git"));
        assertThat(isEqual).isTrue();
    }
}
