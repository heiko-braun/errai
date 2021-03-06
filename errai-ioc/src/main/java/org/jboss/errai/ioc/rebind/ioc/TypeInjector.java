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

package org.jboss.errai.ioc.rebind.ioc;


import com.google.gwt.core.ext.typeinfo.JClassType;

import javax.inject.Singleton;

/**
 * User: christopherbrock
 * Date: 7-Jul-2010
 * Time: 1:00:03 PM
 */
public class TypeInjector extends Injector {
    protected final JClassType type;
    protected boolean injected;
    protected boolean singleTon;
    protected String varName;

    public TypeInjector(JClassType type) {
        this.type = type;
        this.singleTon = type.isAnnotationPresent(Singleton.class)
                || type.isAnnotationPresent(com.google.inject.Singleton.class);
        this.varName = InjectUtil.getNewVarName();
    }

    @Override
    public String getType(InjectionContext injectContext, InjectionPoint injectionPoint) {
        if (isInjected()) {
            if (isSingleton()) {
                return varName;
            } else {
                varName = InjectUtil.getNewVarName();
            }
        }

        ConstructionStrategy cs = InjectUtil.getConstructionStrategy(this, injectContext, injectionPoint);

        String generated = cs.generateConstructor();
        injectContext.getProcessingContext().getWriter().print(generated);

        injected = true;

        return varName;
    }

    @Override
    public String instantiateOnly(InjectionContext injectContext, InjectionPoint injectionPoint) {
        return getType(injectContext, injectionPoint);
    }

    @Override
    public boolean isInjected() {
        return injected;
    }

    @Override
    public boolean isSingleton() {
        return singleTon;
    }

    @Override
    public String getVarName() {
        return varName;
    }

    @Override
    public JClassType getInjectedType() {
        return type;
    }
}
