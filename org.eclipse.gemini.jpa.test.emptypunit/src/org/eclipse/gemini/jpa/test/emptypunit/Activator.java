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
 *     mkeith - Gemini JPA tests 
 ******************************************************************************/
package org.eclipse.gemini.jpa.test.emptypunit;

import org.eclipse.gemini.jpa.test.common.TestActivator;
import org.osgi.framework.BundleContext;

/**
 * Activator to start tests when relevant service becomes available
 * 
 * @author mkeith
 */
public class Activator extends TestActivator {

    /*====================*/
    /* Overridden methods */
    /*====================*/
    
    String[] classNames = { "TestEmptyPersistence", "TestEmptyPersistenceWithProps" };
    
    public String getTestGroupName() { return "EmptyPUnit"; }
    public String getTestPackage() { return "org.eclipse.gemini.jpa.test.emptypunit"; }
    public String[] getTestClasses() { return classNames; }
    public void setBundleContext(BundleContext ctx) { 
        TestEmptyPersistence.ctx = ctx;
        TestEmptyPersistenceWithProps.ctx = ctx;
    }
}