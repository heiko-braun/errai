/*
 * Copyright 2010 JBoss, a divison Red Hat, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.errai.bus.client.framework;

import org.jboss.errai.bus.client.api.Message;

import java.io.Serializable;

/**
 * This class allows different implementations of Message to be provided based on whether or not you're on the
 * client or server.
 */
public interface MessageProvider extends Serializable {

    /**
     * Gets the appropriate message
     *
     * @return the appropriate message
     */
    public Message get();


    // public ModelAdapter getAdapter();
}
