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
 *     mkeith - Gemini JPA Native tests 
 ******************************************************************************/
package org.eclipse.gemini.jpa.test.nativeapi;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import model.basic.Account;

import org.junit.*;
import org.osgi.framework.BundleContext;

import org.eclipse.persistence.sessions.server.ServerSession;
import org.eclipse.persistence.jpa.JpaEntityManagerFactory;
import org.eclipse.persistence.jpa.JpaEntityManager;

import org.eclipse.gemini.jpa.test.common.JpaTest;

/**
 * Test class to test accessing native EclipseLink functionality from the client
 * 
 * @author mkeith
 */
public class TestNativeEMF extends JpaTest {
        
    public static final String TEST_NAME = "TestNativeEMF";
    public static final String PERSISTENCE_UNIT_UNDER_TEST = "Accounts";

    protected static EntityManagerFactory emf;
    public static BundleContext ctx;

    /* === Test Methods === */

    @BeforeClass
    public static void classSetUp() {
        sdebug(TEST_NAME, "In setup");
        emf = lookupEntityManagerFactory(TEST_NAME, PERSISTENCE_UNIT_UNDER_TEST, ctx);
        sdebug(TEST_NAME, "Got EMF - " + emf);
    }

    @AfterClass
    public static void classCleanUp() {
        if (emf != null) {
            emf.close();
            emf = null;
        }
    }
    
    @Test
    public void testGettingNativeEMF() {
        debug("testGettingNativeEMF");
        
        JpaEntityManagerFactory nativeEmf = getEmf().unwrap(JpaEntityManagerFactory.class);
        debug("Got native EMF - " + nativeEmf);
        ServerSession session = getEmf().unwrap(ServerSession.class);
        debug("Got ServerSession - " + session.getName());
    }

        @Test
    public void testGettingNativeEM() {
        debug("testGettingNativeEM");
        EntityManager em = getEmf().createEntityManager();
        JpaEntityManager nativeEm = em.unwrap(JpaEntityManager.class);
        debug("Got native EM - " + nativeEm);
    }
    
    /* === Subclassed methods === */

    public EntityManagerFactory getEmf() { return emf; }

    public String getTestPersistenceUnitName() { return PERSISTENCE_UNIT_UNDER_TEST; }
    
    // Cloned these from AccountTest in basic tests
    public Object newObject() {
        Account a = new Account();
        a.setBalance(200.0);
        return a;
    }

    public Object findObject() {
        EntityManager em = getEmf().createEntityManager();
        Object obj = em.find(Account.class, 1);
        em.close();
        return obj;
    }

    public String queryString() {
        return "SELECT a FROM Account a";
    }
}
