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
 *******************************************************************************/

package org.cloudifysource.cosmo.resource.messages;


import com.google.common.base.Objects;

/**
 * A message sent from a ruote workflow participant to a cloud resource manager.
 *
 * @author Idan Moyal
 * @since 0.1
 */
public class CloudResourceMessage {

    private String id;
    private String action;

    public CloudResourceMessage() {
    }

    public CloudResourceMessage(String id, String action) {
        this.id = id;
        this.action = action;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(getClass()).add("id", id).add("action", action).toString();
    }
}