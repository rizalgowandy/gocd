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
package com.thoughtworks.go.server.messaging;


import com.thoughtworks.go.plugin.access.common.settings.GoPluginExtension;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.messaging.activemq.JMSMessageListenerAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.jms.JMSException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PluginMessageQueueHandlerTest {
    private static final String PLUGIN_ID = "plugin-1";
    private static final String QUEUE_NAME_PREFIX = "queue";
    @Mock
    private GoPluginExtension extension;
    @Mock
    private MessagingService<GoMessage> messaging;
    @Mock
    private JMSMessageListenerAdapter<GoMessage> listenerAdapter;
    @Captor
    private ArgumentCaptor<GoMessageListener<GoMessage>> argumentCaptor;

    private PluginMessageQueueHandler<FooMessage> handler;
    private MyQueueFactory queueFactory;


    @BeforeEach
    public void setUp() {
        queueFactory = new MyQueueFactory();
        handler = new PluginMessageQueueHandler<>(extension, messaging, mock(PluginManager.class), queueFactory) {};
    }

    @Test
    public void shouldCreateListenerWhenAPluginLoadsUp() {
        String pluginId = PLUGIN_ID;
        String queueName = QUEUE_NAME_PREFIX + pluginId;
        when(extension.canHandlePlugin(pluginId)).thenReturn(true);
        when(messaging.addQueueListener(eq(queueName), any())).thenReturn(listenerAdapter);
        handler.pluginLoaded(GoPluginDescriptor.builder().id(pluginId).build());

        assertThat(handler.queues.containsKey(pluginId)).isTrue();
        assertThat(handler.queues.get(pluginId).listeners.containsKey(pluginId)).isTrue();
        List<JMSMessageListenerAdapter<FooMessage>> listeners = handler.queues.get(pluginId).listeners.get(pluginId);
        assertThat(listeners.size()).isEqualTo(10);
        verify(messaging, times(10)).addQueueListener(eq(queueName), argumentCaptor.capture());
        assertThat(argumentCaptor.getValue()).isNotNull();
    }

    @Test
    public void shouldRemoveListenerWhenAPluginIsUnloaded() throws JMSException {
        String pluginId = PLUGIN_ID;
        String queueName = QUEUE_NAME_PREFIX + pluginId;
        when(extension.canHandlePlugin(pluginId)).thenReturn(true);
        when(messaging.addQueueListener(eq(queueName), any())).thenReturn(listenerAdapter);
        GoPluginDescriptor pluginDescriptor = GoPluginDescriptor.builder().id(pluginId).build();

        handler.pluginLoaded(pluginDescriptor);
        handler.pluginUnLoaded(pluginDescriptor);

        assertThat(handler.queues.containsKey(pluginId)).isFalse();
        verify(listenerAdapter, times(10)).stop();
        verify(messaging, times(1)).removeQueue(queueName);
    }

    @Test
    public void shouldIgnoreOtherPluginTypesDuringLoadAndUnload() {
        String pluginId = PLUGIN_ID;
        String queueName = QUEUE_NAME_PREFIX + pluginId;
        when(extension.canHandlePlugin(pluginId)).thenReturn(false);
        GoPluginDescriptor pluginDescriptor = GoPluginDescriptor.builder().id(pluginId).build();

        handler.pluginLoaded(pluginDescriptor);
        handler.pluginUnLoaded(pluginDescriptor);

        assertThat(handler.queues.containsKey(pluginId)).isFalse();
        verify(messaging, never()).removeQueue(queueName);
        verify(messaging, never()).addQueueListener(any(String.class), any());
    }

    private class MyQueueFactory implements QueueFactory<FooMessage> {
        @Override
        public PluginAwareMessageQueue<FooMessage> create(GoPluginDescriptor pluginDescriptor) {
            return new PluginAwareMessageQueue<>(messaging, PLUGIN_ID, QUEUE_NAME_PREFIX + pluginDescriptor.id(), 10, new MyListenerFactory());
        }
    }

    private static class MyListenerFactory implements ListenerFactory<FooMessage> {
        @SuppressWarnings("unchecked")
        @Override
        public GoMessageListener<FooMessage> create() {
            return mock(GoMessageListener.class);
        }
    }

    private static class FooMessage implements PluginAwareMessage {
        @Override
        public String pluginId() {
            return PLUGIN_ID;
        }
    }
}
