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

import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManagerFactory;

import org.osgi.service.jpa.EntityManagerFactoryBuilder;

import org.junit.*;

/**
 * Test class to test looking up EMF Builder Service from a client
 * for a punit that does not a have data source props specified
 * 
 * @author mkeith
 */
public class TestEMFBuilderServiceProperties extends JpaTest {

    public static final String TEST_NAME = "TestEMFBuilderServiceProperties";
    public static final String PERSISTENCE_UNIT_UNDER_TEST = "AccountsNoDataSource";

    public static EntityManagerFactory emf;

    public static boolean shouldRun(String unitName, boolean isEMF) {
        return PERSISTENCE_UNIT_UNDER_TEST.equals(unitName) && !isEMF;
    }

    @BeforeClass
    public static void classSetUp() {
        slog(TEST_NAME, "In setup");
        EntityManagerFactoryBuilder emfb = lookupEntityManagerFactoryBuilder(TEST_NAME, PERSISTENCE_UNIT_UNDER_TEST);
        Map<String,Object> props = new HashMap<String,Object>();        
        props.put("javax.persistence.jdbc.driver", "org.apache.derby.jdbc.ClientDriver");
        props.put("javax.persistence.jdbc.url", "jdbc:derby://localhost:1527/accountDB;create=true");
        props.put("javax.persistence.jdbc.user", "app");
        props.put("javax.persistence.jdbc.password", "app");
        emf = emfb.createEntityManagerFactory(props);
        slog(TEST_NAME, "Got EMF - " + emf);
    }

    @AfterClass
    public static void classCleanUp() {
        if (emf != null) {
            emf.close();
            emf = null;
        }
    }

    /* === Subclassed methods === */

    public EntityManagerFactory getEmf() { return emf; }
}
