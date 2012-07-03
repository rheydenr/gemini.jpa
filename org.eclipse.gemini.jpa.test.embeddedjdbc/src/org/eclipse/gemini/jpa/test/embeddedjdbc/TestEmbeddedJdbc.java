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

import java.util.List;

import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityManager;

import model.embeddedjdbc.EmptyEntity;

import org.junit.*;

import org.eclipse.gemini.jpa.test.common.JpaTest;

/**
 * Test class to test persistence without using DBAccess
 *  
 * @author mkeith
 */
public class TestEmbeddedJdbc extends JpaTest {
        
    public static final String TEST_NAME = "TestEmbeddedJdbc";
    public static final String PERSISTENCE_UNIT_UNDER_TEST = "EmbeddedJDBC";

    protected static EntityManagerFactory emf;
    
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
    
    public boolean needsDsfService() { return false; }

    public EntityManagerFactory getEmf() { return emf; }

    public String getTestPersistenceUnitName() { return PERSISTENCE_UNIT_UNDER_TEST; }

    public Object newObject() {
        EmptyEntity a = new EmptyEntity();
        a.setId(1);
        a.setData("data");
        return a;
    }

    public Object findObject() {
        EntityManager em = emf.createEntityManager();
        Object obj = em.find(EmptyEntity.class, 1);
        em.close();
        return obj;
    }

    public Object queryObjects() {
        EntityManager em = emf.createEntityManager();
        List<?> result = em.createQuery("SELECT a FROM EmptyEntity a").getResultList();
        em.close();
        return result;
    }
}
