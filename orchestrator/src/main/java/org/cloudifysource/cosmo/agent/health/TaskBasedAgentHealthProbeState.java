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

package org.cloudifysource.cosmo.agent.health;

import com.google.common.collect.Maps;
import org.cloudifysource.cosmo.TaskConsumerState;

import java.net.URI;
import java.util.Map;

/**
 * A state holder for the {@link TaskBasedAgentHealthProbe}.
 *
 * @author Eitan Yanovsky
 * @since 0.1
 */
public class TaskBasedAgentHealthProbeState extends TaskConsumerState {

    private Map<URI, TaskBasedAgentHealthProbe.ProbeState> probeStateMap = Maps.newHashMap();

    public Map<URI, TaskBasedAgentHealthProbe.ProbeState> getProbeStateMap() {
        return probeStateMap;
    }

    public void setProbeStateMap(Map<URI, TaskBasedAgentHealthProbe.ProbeState> probeStateMap) {
        this.probeStateMap = probeStateMap;
    }
}
