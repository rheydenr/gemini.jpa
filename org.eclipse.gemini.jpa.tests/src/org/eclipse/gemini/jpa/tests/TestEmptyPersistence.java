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
package org.eclipse.gemini.jpa.tests;

import javax.persistence.EntityManagerFactory;

import org.junit.*;
import org.osgi.service.jpa.EntityManagerFactoryBuilder;

/**
 * Test class to test empty persistence unit using OSGi JPA services
 * 
 * @author mkeith
 */
public class TestEmptyPersistence extends JpaTest {
    
    public static final String TEST_NAME = "TestEmptyPersistenceUnit";
    public static final String PERSISTENCE_UNIT_UNDER_TEST = "Empty1";

    public static EntityManagerFactory emf;

    public static boolean shouldRun(String unitName, boolean isEMF) {
        return PERSISTENCE_UNIT_UNDER_TEST.equals(unitName) && !isEMF;
    }
    
    /* === Test Methods === */

    @BeforeClass
    public static void classSetUp() {
        slog(TEST_NAME, "In setup");
        EntityManagerFactoryBuilder emfb = lookupEntityManagerFactoryBuilder(TEST_NAME, PERSISTENCE_UNIT_UNDER_TEST);
        emf = emfb.createEntityManagerFactory(JpaTest.defaultProps());
        slog(TEST_NAME, "Got EMF - " + emf);
    }

    @AfterClass
    public static void classCleanUp() {
        if (emf != null) {
            emf.close();
            emf = null;
        }
    }

    public void testPersisting() {
        log("overridden testPersisting");
    }

    /* === Subclassed methods === */

    public EntityManagerFactory getEmf() { return emf; }

    public String getTestPersistenceUnitName() { return PERSISTENCE_UNIT_UNDER_TEST; }

    public boolean needsEmfService() { return false; }

    public Object newObject() { return null; }
    public Object findObject() { return null; }
    public Object queryObjects() { return null; }
}
