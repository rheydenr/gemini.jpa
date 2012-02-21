/*******************************************************************************
 * Copyright (c) 2010 Oracle.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution. 
 * The Eclipse Public License is available at
 *     http://www.eclipse.org/legal/epl-v10.html
 * and the Apache License v2.0 is available at 
 *     http://www.opensource.org/licenses/apache2.0.php.
 * You may elect to redistribute this code under either of these licenses.
 *
 * Contributors:
 *     ssmith - EclipseLink integration
 ******************************************************************************/
package org.eclipse.gemini.jpa.provider;

import java.util.Map;

import org.eclipse.persistence.internal.jpa.deployment.JPAInitializer;

@SuppressWarnings({"rawtypes"})
public class PersistenceProvider extends org.eclipse.persistence.jpa.PersistenceProvider {
    
    public JPAInitializer createInitializer(final ClassLoader classLoader, Map m) {
        return new GeminiOSGiInitializer(classLoader);
     }
    
    /**
     * Return JPAInitializer corresponding to the passed classLoader.
     * @param classLoader
     * @param m
     * @return
     */
    public JPAInitializer getInitializer(String emName, Map m){
        ClassLoader classLoader = getClassLoader(emName, m);
        return createInitializer(classLoader, m);
    }
}

