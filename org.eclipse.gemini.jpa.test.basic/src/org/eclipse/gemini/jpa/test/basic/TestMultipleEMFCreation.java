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
 *     Eduard Bartsch - initial test class
 ******************************************************************************/
package org.eclipse.gemini.jpa.test.basic;


import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import model.account.Account;

import org.osgi.framework.BundleContext;
import org.osgi.service.jpa.EntityManagerFactoryBuilder;

import org.junit.*;

/**
 * Test class to test creating multiple EMFs from a single builder
 * 
 * @author Eduard Bartsch
 */
public class TestMultipleEMFCreation extends AccountTest {
        
    public static final String TEST_NAME = "TestMultipleEMFCreation";
    public static final String PERSISTENCE_UNIT_UNDER_TEST = "AccountsNoDataSource";

    protected static EntityManagerFactory emf1, emf2;
    public static BundleContext ctx;

    /* === Test Methods === */

    @BeforeClass
    public static void classSetUp() {
        sdebug(TEST_NAME, "In setup");
        EntityManagerFactoryBuilder emfb = lookupEntityManagerFactoryBuilder(TEST_NAME, PERSISTENCE_UNIT_UNDER_TEST, ctx);   
        sdebug(TEST_NAME, "Got EMFB - " + emfb);

        Map<String,Object> props = defaultProps();
        
        // Props for EMF 1
        props.put("javax.persistence.jdbc.url","jdbc:derby://localhost:1527/firstDB;create=true");
        props.put("eclipselink.session-name", "db1");

        // Create EMF 1
        sdebug(TEST_NAME, "EMF-1 props" + props);
        emf1 = emfb.createEntityManagerFactory(props);
        sdebug(TEST_NAME, "Got EMF-1 - " + emf1);

        EntityManager em1 = emf1.createEntityManager();
        sdebug(TEST_NAME, "Got EM from EMF-1 - " + em1);

        // Props for EMF 2
        props.put("javax.persistence.jdbc.url","jdbc:derby://localhost:1527/secondDB;create=true");
        props.put("eclipselink.session-name", "db2");

        // Create EMF 2
        sdebug(TEST_NAME, "EMF-2 props" + props);
        EntityManagerFactory emf2 = emfb.createEntityManagerFactory(props);
        sdebug(TEST_NAME, "Got EMF-2 - " + emf2);

        EntityManager em2 = emf2.createEntityManager();
        sdebug(TEST_NAME, "Got EM from EMF-2 - " + em2);

        em1.getTransaction().begin();
        em1.persist(new Account());
        em1.getTransaction().commit();

        List<?> result = em2.createQuery("SELECT a FROM Account a").getResultList();
        Assert.assertEquals(result.size(),0);

        em2.getTransaction().begin();
        em2.persist(new Account());
        em2.getTransaction().commit();

        List<?> result1 = em1.createQuery("SELECT a FROM Account a").getResultList();
        List<?> result2 = em2.createQuery("SELECT a FROM Account a").getResultList();

        Assert.assertEquals("EMF-1 result assertion", result1.size(),1);
        Assert.assertEquals("EMF-2 result assertion", result2.size(),1);

        em1.close();
        em2.close();
    }

    
    @AfterClass
    public static void classCleanUp() {
        if (emf1 != null) {
            emf1.close();
            emf1 = null;
        }
        if (emf2 != null) {
            emf2.close();
            emf2 = null;
        }
    }
    
    /* === Subclassed methods === */

    public EntityManagerFactory getEmf() { return emf1; }

    public String getTestPersistenceUnitName() { return PERSISTENCE_UNIT_UNDER_TEST; }
    
    public boolean needsEmfService() { return false; }
}
