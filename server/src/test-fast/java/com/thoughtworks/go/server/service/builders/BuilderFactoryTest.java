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
package com.thoughtworks.go.server.service.builders;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.pluggabletask.PluggableTask;
import com.thoughtworks.go.domain.KillAllChildProcessTask;
import com.thoughtworks.go.domain.NullTask;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.Task;
import com.thoughtworks.go.domain.builder.Builder;
import com.thoughtworks.go.helper.PipelineMother;
import com.thoughtworks.go.helper.StageMother;
import com.thoughtworks.go.plugin.access.artifact.ArtifactExtension;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskExtension;
import com.thoughtworks.go.plugin.infra.PluginRequestProcessorRegistry;
import com.thoughtworks.go.server.service.UpstreamPipelineResolver;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import com.thoughtworks.go.work.DefaultGoPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.nio.charset.Charset;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BuilderFactoryTest {

    private static final AntTaskBuilder antTaskBuilder = mock(AntTaskBuilder.class);
    private static final ExecTaskBuilder execTaskBuilder = mock(ExecTaskBuilder.class);
    private static final NantTaskBuilder nantTaskBuilder = mock(NantTaskBuilder.class);
    private static final RakeTaskBuilder rakeTaskBuilder = mock(RakeTaskBuilder.class);
    private static final KillAllChildProcessTaskBuilder killAllChildProcessTaskBuilder = mock(KillAllChildProcessTaskBuilder.class);
    private static final FetchTaskBuilder fetchTaskBuilder = mock(FetchTaskBuilder.class);
    private static final NullTaskBuilder nullTaskBuilder = mock(NullTaskBuilder.class);
    private static final PluggableTaskBuilderCreator pluggableTaskBuilderCreator = mock(PluggableTaskBuilderCreator.class);

    private UpstreamPipelineResolver pipelineResolver;
    private BuilderFactory builderFactory;

    private static class TaskArguments implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    Arguments.of(new AntTask(), antTaskBuilder),
                    Arguments.of(new ExecTask(), execTaskBuilder),
                    Arguments.of(new NantTask(), nantTaskBuilder),
                    Arguments.of(new RakeTask(), rakeTaskBuilder),
                    Arguments.of(new FetchTask(), fetchTaskBuilder),
                    Arguments.of(new NullTask(), nullTaskBuilder),
                    Arguments.of(new PluggableTask(), pluggableTaskBuilderCreator),
                    Arguments.of(new KillAllChildProcessTask(), killAllChildProcessTaskBuilder));
        }
    }

    @BeforeEach
    public void setUp() {
        pipelineResolver = mock(UpstreamPipelineResolver.class);
        builderFactory = new BuilderFactory(antTaskBuilder, execTaskBuilder, nantTaskBuilder, rakeTaskBuilder, pluggableTaskBuilderCreator, killAllChildProcessTaskBuilder, fetchTaskBuilder, nullTaskBuilder);
    }

    @ParameterizedTest
    @ArgumentsSource(TaskArguments.class)
    public void shouldCreateABuilderUsingTheCorrectTaskBuilderForATask(Task task, TaskBuilder<Task> taskBuilder)  {
        assertBuilderForTask(task, taskBuilder);
    }

    @Test
    public void shouldFailIfCalledWithSomeRandomTypeOfTask()  {
        Task task = someRandomNonStandardTask();

        try {
            Pipeline pipeline = PipelineMother.pipeline("pipeline1", StageMother.custom("stage1"));
            builderFactory.builderFor(task, pipeline, pipelineResolver);
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).isEqualTo("Unexpected type of task: " + task.getClass());
        }
    }

    @Test
    public void shouldCreateABuilderForEachTypeOfTaskWhichExists()  {
        Pipeline pipeline = PipelineMother.pipeline("pipeline1", StageMother.custom("stage1"));
        AntTask antTask = new AntTask();
        NantTask nantTask = new NantTask();
        RakeTask rakeTask = new RakeTask();
        PluggableTask pluggableTask = new PluggableTask();

        Builder expectedBuilderForAntTask = myFakeBuilder();
        Builder expectedBuilderForNantTask = myFakeBuilder();
        Builder expectedBuilderForRakeTask = myFakeBuilder();
        Builder expectedBuilderForPluggableTask = myFakeBuilder();

        when(antTaskBuilder.createBuilder(builderFactory, antTask, pipeline, pipelineResolver)).thenReturn(expectedBuilderForAntTask);
        when(nantTaskBuilder.createBuilder(builderFactory, nantTask, pipeline, pipelineResolver)).thenReturn(expectedBuilderForNantTask);
        when(rakeTaskBuilder.createBuilder(builderFactory, rakeTask, pipeline, pipelineResolver)).thenReturn(expectedBuilderForRakeTask);
        when(pluggableTaskBuilderCreator.createBuilder(builderFactory, pluggableTask, pipeline, pipelineResolver)).thenReturn(expectedBuilderForPluggableTask);

        List<Builder> builders = builderFactory.buildersForTasks(pipeline, List.of(new Task[]{antTask, nantTask, rakeTask, pluggableTask}), pipelineResolver);

        assertThat(builders.size()).isEqualTo(4);
        assertThat(builders.get(0)).isEqualTo(expectedBuilderForAntTask);
        assertThat(builders.get(1)).isEqualTo(expectedBuilderForNantTask);
        assertThat(builders.get(2)).isEqualTo(expectedBuilderForRakeTask);
        assertThat(builders.get(3)).isEqualTo(expectedBuilderForPluggableTask);
    }

    private void assertBuilderForTask(Task task, TaskBuilder<Task> expectedBuilderToBeUsed) {
        Pipeline pipeline = PipelineMother.pipeline("pipeline1", StageMother.custom("stage1"));

        Builder expectedBuilder = myFakeBuilder();
        when(expectedBuilderToBeUsed.createBuilder(builderFactory, task, pipeline, pipelineResolver)).thenReturn(expectedBuilder);

        Builder builder = builderFactory.builderFor(task, pipeline, pipelineResolver);

        assertThat(builder).isEqualTo(expectedBuilder);
    }

    private Builder myFakeBuilder() {
        return new Builder(null, null, null) {
            @Override
            public void build(DefaultGoPublisher publisher, EnvironmentVariableContext environmentVariableContext, TaskExtension taskExtension, ArtifactExtension artifactExtension, PluginRequestProcessorRegistry pluginRequestProcessorRegistry, Charset consoleLogCharset) {
            }

            @Override
            public String toString() {
                return "A fake builder " + super.toString();
            }
        };
    }

    private BuildTask someRandomNonStandardTask() {
        return new BuildTask() {

            @Override
            public String getTaskType() {
                return "build";
            }

            @Override
            public String getTypeForDisplay() {
                return null;
            }

            @Override
            public String command() {
                return null;
            }

            @Override
            public String arguments() {
                return null;
            }
        };
    }


}
