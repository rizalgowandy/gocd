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
package com.thoughtworks.go.config.materials;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.ConfigSaveValidationContext;
import com.thoughtworks.go.config.PipelineConfigSaveValidationContext;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.packagerepository.*;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static com.thoughtworks.go.helper.MaterialConfigsMother.git;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class PackageMaterialConfigTest {
    @Test
    public void shouldAddErrorIfMaterialDoesNotHaveAPackageId() {
        PackageMaterialConfig packageMaterialConfig = new PackageMaterialConfig();
        packageMaterialConfig.validateConcreteMaterial(new ConfigSaveValidationContext(null, null));

        assertThat(packageMaterialConfig.errors().getAll().size()).isEqualTo(1);
        assertThat(packageMaterialConfig.errors().on(PackageMaterialConfig.PACKAGE_ID)).isEqualTo("Please select a repository and package");
    }

    @Test
    public void shouldAddErrorIfPackageDoesNotExistsForGivenPackageId() {
        PipelineConfigSaveValidationContext configSaveValidationContext = mock(PipelineConfigSaveValidationContext.class);
        when(configSaveValidationContext.findPackageById(anyString())).thenReturn(mock(PackageRepository.class));
        PackageRepository packageRepository = mock(PackageRepository.class);
        when(packageRepository.doesPluginExist()).thenReturn(true);
        PackageMaterialConfig packageMaterialConfig = new PackageMaterialConfig(new CaseInsensitiveString("package-name"), "package-id", PackageDefinitionMother.create("package-id"));
        packageMaterialConfig.getPackageDefinition().setRepository(packageRepository);

        packageMaterialConfig.validateTree(configSaveValidationContext);

        assertThat(packageMaterialConfig.errors().getAll().size()).isEqualTo(1);
        assertThat(packageMaterialConfig.errors().on(PackageMaterialConfig.PACKAGE_ID)).isEqualTo("Could not find plugin for given package id:[package-id].");
    }

    @Test
    public void shouldAddErrorIfPackagePluginDoesNotExistsForGivenPackageId() {
        PipelineConfigSaveValidationContext configSaveValidationContext = mock(PipelineConfigSaveValidationContext.class);
        when(configSaveValidationContext.findPackageById(anyString())).thenReturn(mock(PackageRepository.class));
        PackageRepository packageRepository = mock(PackageRepository.class);
        when(packageRepository.doesPluginExist()).thenReturn(false);
        PackageMaterialConfig packageMaterialConfig = new PackageMaterialConfig(new CaseInsensitiveString("package-name"), "package-id", PackageDefinitionMother.create("package-id"));
        packageMaterialConfig.getPackageDefinition().setRepository(packageRepository);

        packageMaterialConfig.validateTree(configSaveValidationContext);

        assertThat(packageMaterialConfig.errors().getAll().size()).isEqualTo(1);
        assertThat(packageMaterialConfig.errors().on(PackageMaterialConfig.PACKAGE_ID)).isEqualTo("Could not find plugin for given package id:[package-id].");
    }

    @Test
    public void shouldAddErrorIfMaterialNameUniquenessValidationFails() {
        PackageMaterialConfig packageMaterialConfig = new PackageMaterialConfig("package-id");

        Map<CaseInsensitiveString, AbstractMaterialConfig> nameToMaterialMap = new HashMap<>();
        PackageMaterialConfig existingMaterial = new PackageMaterialConfig("package-id");
        nameToMaterialMap.put(new CaseInsensitiveString("package-id"), existingMaterial);
        nameToMaterialMap.put(new CaseInsensitiveString("foo"), git("url"));

        packageMaterialConfig.validateNameUniqueness(nameToMaterialMap);

        assertThat(packageMaterialConfig.errors().getAll().size()).isEqualTo(1);
        assertThat(packageMaterialConfig.errors().on(PackageMaterialConfig.PACKAGE_ID)).isEqualTo("Duplicate package material detected!");
        assertThat(existingMaterial.errors().getAll().size()).isEqualTo(1);
        assertThat(existingMaterial.errors().on(PackageMaterialConfig.PACKAGE_ID)).isEqualTo("Duplicate package material detected!");
        assertThat(nameToMaterialMap.size()).isEqualTo(2);
    }

    @Test
    public void shouldPassMaterialUniquenessIfIfNoDuplicateMaterialFound() {
        PackageMaterialConfig packageMaterialConfig = new PackageMaterialConfig("package-id");

        Map<CaseInsensitiveString, AbstractMaterialConfig> nameToMaterialMap = new HashMap<>();
        nameToMaterialMap.put(new CaseInsensitiveString("repo-name:pkg-name"), new PackageMaterialConfig("package-id-new"));
        nameToMaterialMap.put(new CaseInsensitiveString("foo"), git("url"));

        packageMaterialConfig.validateNameUniqueness(nameToMaterialMap);

        assertThat(packageMaterialConfig.errors().getAll().size()).isEqualTo(0);
        assertThat(nameToMaterialMap.size()).isEqualTo(3);
    }

    @Test
    public void shouldNotAddErrorDuringUniquenessValidationIfMaterialNameIsEmpty() {
        PackageMaterialConfig packageMaterialConfig = new PackageMaterialConfig("");

        Map<CaseInsensitiveString, AbstractMaterialConfig> nameToMaterialMap = new HashMap<>();

        packageMaterialConfig.validateNameUniqueness(nameToMaterialMap);

        assertThat(packageMaterialConfig.errors().getAll().size()).isEqualTo(0);
        assertThat(nameToMaterialMap.size()).isEqualTo(0);
    }

    @Test
    public void shouldSetConfigAttributesForThePackageMaterial() {
        Map<String, String> attributes = new HashMap<>();
        attributes.put(PackageMaterialConfig.PACKAGE_ID, "packageId");

        PackageMaterialConfig packageMaterialConfig = new PackageMaterialConfig();
        packageMaterialConfig.setConfigAttributes(attributes);
        assertThat(packageMaterialConfig.getPackageId()).isEqualTo("packageId");
    }

    @Test
    public void shouldSetPackageIdToNullIfConfigAttributesForThePackageMaterialDoesNotContainPackageId() {
        Map<String, String> attributes = new HashMap<>();
        PackageMaterialConfig packageMaterialConfig = new PackageMaterialConfig("id");
        packageMaterialConfig.setConfigAttributes(attributes);
        assertThat(packageMaterialConfig.getPackageId()).isNull();
    }

    @Test
    public void shouldSetPackageIdAsNullIfPackageDefinitionIsNull() {
        PackageMaterialConfig materialConfig = new PackageMaterialConfig("1");
        materialConfig.setPackageDefinition(null);
        assertThat(materialConfig.getPackageId()).isNull();
        assertThat(materialConfig.getPackageDefinition()).isNull();
    }

    @Test
    public void shouldGetNameFromRepoNameAndPackageName() {
        PackageMaterialConfig materialConfig = new PackageMaterialConfig();
        PackageRepository repository = PackageRepositoryMother.create("repo-id", "repo-name", "pluginid", "version", new Configuration(ConfigurationPropertyMother.create("k1", false, "v1")));
        materialConfig.setPackageDefinition(PackageDefinitionMother.create("p-id", "package-name", new Configuration(ConfigurationPropertyMother.create("k2", false, "v2")), repository));
        assertThat(materialConfig.getName().toString()).isEqualTo("repo-name_package-name");
        materialConfig.setPackageDefinition(null);
        assertThat(materialConfig.getName()).isNull();
    }

    @Test
    public void shouldCheckEquals() {
        PackageRepository repository = PackageRepositoryMother.create("repo-id", "repo-name", "pluginid", "version", new Configuration(ConfigurationPropertyMother.create("k1", false, "v1")));
        PackageDefinition packageDefinition = PackageDefinitionMother.create("p-id", "package-name", new Configuration(ConfigurationPropertyMother.create("k2", false, "v2")), repository);

        PackageMaterialConfig p1 = new PackageMaterialConfig();
        p1.setPackageDefinition(packageDefinition);

        PackageMaterialConfig p2 = new PackageMaterialConfig();
        p2.setPackageDefinition(packageDefinition);
        assertThat(p1.equals(p2)).isTrue();


        p1 = new PackageMaterialConfig();
        p2 = new PackageMaterialConfig();
        assertThat(p1.equals(p2)).isTrue();

        p2.setPackageDefinition(packageDefinition);
        assertThat(p1.equals(p2)).isFalse();

        p1.setPackageDefinition(packageDefinition);
        p2 = new PackageMaterialConfig();
        assertThat(p1.equals(p2)).isFalse();

        assertThat(p1.equals(null)).isFalse();
    }

    @Test
    public void shouldDelegateToPackageDefinitionForAutoUpdate() {
        PackageDefinition packageDefinition = mock(PackageDefinition.class);
        when(packageDefinition.isAutoUpdate()).thenReturn(false);
        PackageMaterialConfig materialConfig = new PackageMaterialConfig(new CaseInsensitiveString("name"), "package-id", packageDefinition);

        assertThat(materialConfig.isAutoUpdate()).isFalse();

        verify(packageDefinition).isAutoUpdate();
    }
}
