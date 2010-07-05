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

import javax.persistence.EntityManagerFactory;

import org.junit.*;

/**
 * Test class to test looking up EMF Service from a client
 * 
 * @author mkeith
 */
public class TestEMFService extends JpaTest {
        
    public static final String TEST_NAME = "TestEMFService";
    public static final String PERSISTENCE_UNIT_UNDER_TEST = "Accounts";

    protected static EntityManagerFactory emf;

    public static boolean shouldRun(String unitName, boolean isEMF) {
        return PERSISTENCE_UNIT_UNDER_TEST.equals(unitName) && isEMF;
    }

    /* === Test Methods === */

    @BeforeClass
    public static void classSetUp() {
        slog(TEST_NAME, "In setup");
        emf = lookupEntityManagerFactory(TEST_NAME, PERSISTENCE_UNIT_UNDER_TEST);
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
