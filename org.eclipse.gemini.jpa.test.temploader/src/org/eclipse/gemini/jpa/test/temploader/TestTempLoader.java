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
package org.eclipse.gemini.jpa.test.temploader;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import model.tldr.Person;

import org.junit.*;
import org.osgi.framework.BundleContext;

import org.eclipse.gemini.jpa.test.common.JpaTest;

/**
 * Test class to test looking up EMF Service from a client
 * 
 * @author mkeith
 */
public class TestTempLoader extends JpaTest {
        
    public static final String TEST_NAME = "TestTempLoader";
    public static final String PERSISTENCE_UNIT_UNDER_TEST = "TLAccounts";

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

    /* === Subclassed methods === */

    public EntityManagerFactory getEmf() { return emf; }

    public String getTestPersistenceUnitName() { return PERSISTENCE_UNIT_UNDER_TEST; }
    
    public Object newObject() {
        Person a = new Person();
        return a;
    }
    
    public Object findObject() {
        EntityManager em = getEmf().createEntityManager();
        Person a = em.find(Person.class, 1);
        em.close();
        return a;
    }

    public String queryString() {
        return "SELECT a FROM Person a";
    }
}
