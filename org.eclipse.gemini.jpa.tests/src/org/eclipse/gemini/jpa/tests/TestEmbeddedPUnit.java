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

import java.util.List;

import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityManager;

import model.embeddedaccount.EmbAccount;

import org.junit.*;

/**
 * Test class to test looking up EMF Service of an embedded persistence unit
 * 
 * @author mkeith
 */
public class TestEmbeddedPUnit extends JpaTest {
        
    public static final String TEST_NAME = "TestEmbeddedPUnit";
    public static final String PERSISTENCE_UNIT_UNDER_TEST = "EmbeddedAccounts";

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

    public Object newObject() {
        EmbAccount a = new EmbAccount();
        a.setBalance(100.0);
        return a;
    }
    
    public Object findObject() {
        EntityManager em = emf.createEntityManager();
        Object obj = em.find(EmbAccount.class, 1);
        em.close();
        return obj;
    }

    public Object queryObjects() {
        EntityManager em = emf.createEntityManager();
        List<?> result = em.createQuery("SELECT a FROM EmbAccount a").getResultList();
        em.close();
        return result;
    }
}
