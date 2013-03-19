/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.cloudifysource.cosmo.mock;

import com.google.common.base.Optional;

import java.net.URI;

/**
 * An interface that allows adding and removing {@link org.cloudifysource.cosmo.TaskConsumer}.
 * This wraps the task consumer with a container that injects tasks for it to consume.
 * @author itaif
 * @since 0.1
 */
public interface TaskConsumerRegistrar {

    /**
     * Registers the task consumer with the specified id.
     */
    void registerTaskConsumer(Object taskConsumer, URI taskConsumerId);

    /**
     * Unregisters a task consumer with the specified id.
     * @return The task consumer object
     */
    Optional<Object> unregisterTaskConsumer(URI taskConsumerId);

}
