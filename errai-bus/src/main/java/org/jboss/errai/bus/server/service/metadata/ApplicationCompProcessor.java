/*
 * Copyright 2009 JBoss, a divison Red Hat, Inc
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
package org.jboss.errai.bus.server.service.metadata;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import org.jboss.errai.bus.client.api.Message;
import org.jboss.errai.bus.client.api.ResourceProvider;
import org.jboss.errai.bus.client.framework.MessageBus;
import org.jboss.errai.bus.client.framework.RequestDispatcher;
import org.jboss.errai.bus.server.ErraiBootstrapFailure;
import org.jboss.errai.bus.server.annotations.ApplicationComponent;
import org.jboss.errai.bus.server.annotations.Service;
import org.jboss.errai.bus.server.io.MethodEndpointCallback;
import org.jboss.errai.bus.server.service.ErraiServiceConfiguratorImpl;
import org.jboss.errai.bus.server.service.bootstrap.BootstrapContext;
import org.jboss.errai.bus.server.service.bootstrap.GuiceProviderProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

/**
 * @author: Heiko Braun <hbraun@redhat.com>
 * @date: Aug 3, 2010
 */
public class ApplicationCompProcessor implements MetaDataProcessor {
    private Logger log = LoggerFactory.getLogger(ApplicationCompProcessor.class);

    public void process(final BootstrapContext context, MetaDataScanner reflections) {
        final ErraiServiceConfiguratorImpl config = (ErraiServiceConfiguratorImpl) context.getConfig();
        final Set<Class<?>> components = reflections.getTypesAnnotatedWith(ApplicationComponent.class);

        for (Class<?> loadClass : components) {
            log.info("discovered application component: " + loadClass.getName());

            try {
                Object inst = Guice.createInjector(new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(MessageBus.class).toInstance(context.getBus());
                        bind(RequestDispatcher.class).toInstance(context.getService().getDispatcher());

                        // Add any extension bindings.
                        for (Map.Entry<Class<?>, ResourceProvider> entry : config.getExtensionBindings().entrySet()) {
                            bind(entry.getKey()).toProvider(new GuiceProviderProxy(entry.getValue()));
                        }
                    }
                }).getInstance(loadClass);


                for (Method m : loadClass.getMethods()) {
                    Class[] parmTypes = m.getParameterTypes();

                    if (m.isAnnotationPresent(Service.class)) {
                        if (parmTypes.length != 1)
                            throw new ErraiBootstrapFailure("wrong number of method arguments for service endpoint: " + m.getName() + ": " + parmTypes.length);

                        if (!Message.class.isAssignableFrom(parmTypes[0]))
                            throw new ErraiBootstrapFailure("attempt to declare service handler on illegal type: " + parmTypes[0].getName());

                        Annotation annotation = m.getAnnotation(Service.class);

                        String svcName = ((Service) annotation).value().equals("") ? m.getName() : ((Service) annotation).value();

                        context.getBus().subscribe(svcName, new MethodEndpointCallback(inst, m));
                    } else {
                        int i = 0;
                        for (Annotation[] annotations : m.getParameterAnnotations()) {
                            Class parmType = parmTypes[i++];

                            for (Annotation annotation : annotations) {
                                if (annotation instanceof Service) {
                                    if (!Message.class.isAssignableFrom(parmType))
                                        throw new ErraiBootstrapFailure("attempt to declare service handler on illegal type: " + parmType.getName());

                                    if (parmTypes.length != 1)
                                        throw new ErraiBootstrapFailure("wrong number of method arguments for service endpoint: " + m.getName() + ": " + parmTypes.length);

                                    String svcName = ((Service) annotation).value().equals("") ? m.getName() : ((Service) annotation).value();

                                    context.getBus().subscribe(svcName, new MethodEndpointCallback(inst, m));
                                }
                            }
                        }
                    }
                }

            }
            catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }
}
