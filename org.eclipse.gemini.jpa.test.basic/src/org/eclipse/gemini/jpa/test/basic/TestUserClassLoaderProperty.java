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
package org.eclipse.gemini.jpa.test.basic;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManagerFactory;

import org.osgi.framework.BundleContext;
import org.osgi.service.jpa.EntityManagerFactoryBuilder;

import org.eclipse.gemini.jpa.test.common.JpaTest;

import org.junit.*;

/**
 * Test class to test an app passing in a classloader
 * 
 * @author mkeith
 */
public class TestUserClassLoaderProperty extends AccountTest {
    
    public static final String TEST_NAME = "TestAppClassLoader";
    public static final String PERSISTENCE_UNIT_UNDER_TEST = "Accounts";

    protected static EntityManagerFactory emf;
    public static BundleContext ctx;

    /* === Test Methods === */

    @BeforeClass
    public static void classSetUp() {
        sdebug(TEST_NAME, "In setup");
        EntityManagerFactoryBuilder emfb = lookupEntityManagerFactoryBuilder(TEST_NAME, PERSISTENCE_UNIT_UNDER_TEST, ctx);
        Map<String,Object> props = new HashMap<String,Object>();
        props.put("eclipselink.classloader", new TestLoader());
        emf = emfb.createEntityManagerFactory(props);
        sdebug(TEST_NAME, "Got EMF - " + emf);
    }

    @AfterClass
    public static void classCleanUp() {
        if (emf != null) {
            emf.close();
            emf = null;
        }
    }
    
    /* === Test methods === */
    @Test
    public void testLoaderUsed() {
        // The EMF has already been created by the setup 
        // method so the loader should already have been used
        assert(TestLoader.loaderUsed);    
    }
    
    /* === Subclassed methods === */

    public EntityManagerFactory getEmf() { return emf; }

    public String getTestPersistenceUnitName() { return PERSISTENCE_UNIT_UNDER_TEST; }

    public boolean needsEmfService() { return false; }
    
    /* === Inner Loader class used for test === */
    public static class TestLoader extends ClassLoader {
        public static boolean loaderUsed = false;
        
        public Class<?> loadClass(String cls) throws ClassNotFoundException  {
            JpaTest.sdebug(TEST_NAME, "TestLoader.loadClass() on class " + cls);
            loaderUsed = true;
            return super.loadClass(cls);
        }
    }
}
