/*
 * Copyright (C) 2010 Oracle Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.eclipse.gemini.jpa.spec;

import javax.persistence.spi.PersistenceProviderResolverHolder;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Activator class for OSGi integration
 * 
 * @author mkeith, tware
 */
public class Activator implements BundleActivator {

    public void start(BundleContext context) throws Exception {
        PersistenceProviderResolverHolder.setPersistenceProviderResolver(new OSGiProviderResolver(context));
        System.out.println("Gemini javax.persistence active");
    }

    public void stop(BundleContext context) throws Exception {
        PersistenceProviderResolverHolder.setPersistenceProviderResolver(null);
    }
}
