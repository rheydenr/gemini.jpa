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
package org.eclipse.gemini.jpa.test.jndi;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.junit.*;

import org.osgi.framework.BundleContext;

import model.jndi.*;

import org.eclipse.gemini.jpa.test.common.JpaTest;

/**
 * Test class to test data source being looked up in JNDI
 * 
 * @author mkeith
 */
public class TestJndi1 extends JpaTest {
    
    public static final String TEST_NAME = "TestJndi1";
    public static final String PERSISTENCE_UNIT_UNDER_TEST = "JndiPU";

    public static EntityManagerFactory emf;
    public static BundleContext ctx;

    /* === Test Methods === */

    @BeforeClass
    public static void classSetUp() {
        sdebug(TEST_NAME, "In setup");
        emf = lookupEntityManagerFactory(TEST_NAME, PERSISTENCE_UNIT_UNDER_TEST, ctx);
        sdebug(TEST_NAME, "Got EMF - " + emf);        
    }

    @AfterClass
    public static void classCleanUp() throws Exception {
        if (emf != null) {
            emf.close();
            emf = null;
        }
    }
    
    /* === Subclassed methods === */

    public boolean needsEmfService() { return true; }
    public boolean needsDsfService() { return true; }

    public EntityManagerFactory getEmf() { return emf; }

    public String getTestPersistenceUnitName() { return PERSISTENCE_UNIT_UNDER_TEST; }

    public Object newObject() {
        JndiAccount a = new JndiAccount();
        a.setId(100);
        return a;
    }
    
    public Object findObject() {
        EntityManager em = getEmf().createEntityManager();
        Object obj = em.find(JndiAccount.class, 100);        
        em.close();
        return obj;
    }

    public String queryString() {
        return "SELECT a FROM JndiAccount a";
    }        
}
