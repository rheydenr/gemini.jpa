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
 *     Eduard Bartsch - initial test class
 ******************************************************************************/
package org.eclipse.gemini.jpa.test.driverversion;


import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManagerFactory;

import org.osgi.framework.BundleContext;
import org.osgi.service.jpa.EntityManagerFactoryBuilder;

import org.junit.*;

import org.eclipse.gemini.jpa.test.common.JpaTest;

/**
 * Test class to test being able to specify a dependency on a driver version
 * 
 * @author mkeith
 */
public class TestDriverVersion extends JpaTest {
        
    public static final String TEST_NAME = "TestDriverVersion";
    public static final String PERSISTENCE_UNIT_UNDER_TEST = "DriverVersion3";
    public static final String PUNIT_2 = "UnspecifiedDriverVersion";

    protected static EntityManagerFactoryBuilder emfb1, emfb2;
    protected static EntityManagerFactory emf1, emf2;

	public static BundleContext ctx;
		
    /* === Test Methods === */

    @BeforeClass
    public static void classSetUp() {
        sdebug(TEST_NAME, "In setup");
        emfb1 = lookupEntityManagerFactoryBuilder(TEST_NAME, PERSISTENCE_UNIT_UNDER_TEST, ctx);
        Map<String,Object> props = new HashMap<String,Object>();        
        emf1 = emfb1.createEntityManagerFactory(props);
        sdebug(TEST_NAME, "Got EMF-1 - " + emf1);

        emfb2 = lookupEntityManagerFactoryBuilder(TEST_NAME, PUNIT_2, ctx);
    }

    @AfterClass
    public static void classCleanUp() {
        if (emf1 != null) {
            emf1.close();
            emf1 = null;
        }
        if (emf2 != null) {
            emf2.close();
            emf2 = null;
        }
    }

    @Test
    public void testDriverVersionIncludedInDescriptor() {
        // Don't yet know how to test this apart from checking the logs for DSF properties
        debug("emf1 is " + emf1);
    }

    @Test
    public void testDriverVersionProperty() {
        // Don't yet know how to test this apart from checking the logs for DSF properties
        Map<String,Object> props = defaultProps();        
        props.put("osgi.jdbc.driver.version", "4.0");
        emf2 = emfb2.createEntityManagerFactory(props);
        debug("emf2 is " + emf2);
    }

    public void testPersisting() {
        log("overridden testPersisting");
    }
    
    /* === Subclassed methods === */

    public EntityManagerFactory getEmf() { return emf1; }

    public String getTestPersistenceUnitName() { return PERSISTENCE_UNIT_UNDER_TEST; }
    
    public boolean needsEmfService() { return false; }
    
    public Object newObject() { return null; }
    public Object findObject() { return null; }
    public Object queryObjects() { return null; }
    // Not used
    public String queryString() { return null; }
}
