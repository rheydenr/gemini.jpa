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
 *     Mike Keith - Initial test class
 ******************************************************************************/
package org.eclipse.gemini.jpa.test.mongo;


import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityManager;

import org.osgi.framework.BundleContext;
import org.osgi.service.jpa.EntityManagerFactoryBuilder;

import model.mongo.Account;

// import org.osgi.framework.BundleContext;

import org.junit.*;

import org.eclipse.gemini.jpa.test.common.JpaTest;

/**
 * Test class to test Mongo
 * 
 * @author mkeith
 */
public class TestMongo extends JpaTest {
        
    public static final String TEST_NAME = "TestMongo";
    public static final String PERSISTENCE_UNIT_UNDER_TEST = "Mongo";

    protected static EntityManagerFactoryBuilder emfb;
    protected static EntityManagerFactory emf;
    protected static BundleContext ctx;

    static Account acct;
    
    /* === Test Methods === */

    @BeforeClass
    public static void classSetUp() {
        sdebug(TEST_NAME, "In setup");
        emfb = lookupEntityManagerFactoryBuilder(TEST_NAME, PERSISTENCE_UNIT_UNDER_TEST, ctx);
        Map<String,Object> props = new HashMap<String,Object>();        
        props.put("gemini.jpa.providerConnectedDataSource", true);
        emf = emfb.createEntityManagerFactory(props);
        sdebug(TEST_NAME, "Got EMFB - " + emfb);
    }

    @AfterClass
    public static void classCleanUp() {
        if (emf != null) {
            emf.close();
            emf = null;
        }
    }

    @Test
    public void testConnecting() {
        debug("testConnecting creating EM ");
        EntityManager em = emf.createEntityManager();
        debug("Closing EM");
        em.close();
    }
        
    /* === Subclassed methods === */

    public EntityManagerFactory getEmf() { return emf; }

    public String getTestPersistenceUnitName() { return PERSISTENCE_UNIT_UNDER_TEST; }
    
    public boolean needsEmfService() { return false; }
    
    public Object newObject() {
        // Assume serialized testing, so no concurrency issues
        if (acct == null) 
            acct = new Account();
        return acct;
    }

    public Object findObject() { 
        if (acct == null)
            log("testFinding() called before testPersisting() - skipping find test!");
        EntityManager em = getEmf().createEntityManager();
        Object obj = em.find(Account.class, acct.getId()); 
        em.close();
        return obj;
    }    

    public String queryString() {
        return "SELECT a FROM Account a";
    }
    
    /* === Helper Methods === */

    public Account getAccount() {
        // Assume serialized testing, so no concurrency issues
        if (acct == null) 
            acct = new Account();
        return acct;
    }
}
