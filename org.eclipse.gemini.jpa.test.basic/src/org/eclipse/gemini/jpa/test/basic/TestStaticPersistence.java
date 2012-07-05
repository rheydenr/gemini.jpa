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

import javax.persistence.Persistence;
import javax.persistence.EntityManagerFactory;

import org.junit.*;
import org.osgi.framework.BundleContext;

/**
 * Test class to test static Persistence class using OSGi JPA services
 * 
 * @author mkeith
 */
public class TestStaticPersistence extends AccountTest {
    
    public static final String TEST_NAME = "TestStaticPersistence";
    public static final String PERSISTENCE_UNIT_UNDER_TEST = "Accounts";

    public static EntityManagerFactory emf;
    public static BundleContext ctx;

    /* === Test Methods === */

    @BeforeClass
    public static void classSetUp() {
        slog(TEST_NAME, "In setup");
        emf = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_UNDER_TEST);
        slog(TEST_NAME, "Got EMF - " + emf);
    }

    @AfterClass
    public static void classCleanUp() throws Exception {
        if (emf != null) {
            emf.close();
            emf = null;
        }
    }

    /* === Subclassed methods === */

    public EntityManagerFactory getEmf() { return emf; }

    public String getTestPersistenceUnitName() { return PERSISTENCE_UNIT_UNDER_TEST; }
}
