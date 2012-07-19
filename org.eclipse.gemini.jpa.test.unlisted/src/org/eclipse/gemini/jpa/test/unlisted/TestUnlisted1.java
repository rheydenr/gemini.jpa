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
package org.eclipse.gemini.jpa.test.unlisted;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.junit.*;

import org.osgi.framework.BundleContext;

import model.unlisted.*;

import org.eclipse.gemini.jpa.test.common.JpaTest;

/**
 * Test class to test unlisted entities
 * 
 * @author mkeith
 */
public class TestUnlisted1 extends JpaTest {
    
    public static final String TEST_NAME = "TestUnlisted1";
    public static final String PERSISTENCE_UNIT_UNDER_TEST = "Unlisted";

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

    public boolean needsDsfService() { return false; }

    public EntityManagerFactory getEmf() { return emf; }

    public String getTestPersistenceUnitName() { return PERSISTENCE_UNIT_UNDER_TEST; }

    public Object newObject() {
        UnlistedEntity1 a = new UnlistedEntity1();
        a.setId(100);
        return a;
    }
    
    public Object findObject() {
        EntityManager em = getEmf().createEntityManager();
        Object obj = em.find(UnlistedEntity1.class, 100);        
        em.close();
        return obj;
    }

    public String queryString() {
        return "SELECT a FROM UnlistedEntity1 a";
    }        
}
