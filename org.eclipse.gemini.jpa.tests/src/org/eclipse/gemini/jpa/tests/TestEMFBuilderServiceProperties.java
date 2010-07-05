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
        Map<String,String> props = new HashMap<String,String>();        
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
