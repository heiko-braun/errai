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

package org.jboss.errai.bus.client.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class BusTools {
    public static final Set<String> RESERVED_SERVICES;

    static {
        Set<String> s = new HashSet<String>();

        s.add("ServerBus");
        s.add("AuthorizationService");
        s.add("AuthenticationService");
        s.add("ServerEchoService");

        s.add("ClientBus");
        s.add("ClientBusErrors");

        RESERVED_SERVICES = Collections.unmodifiableSet(s);
    }

    public static boolean isReservedName(String name) {
        return RESERVED_SERVICES.contains(name);
    }

}
