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
package org.eclipse.gemini.jpa.test.multiemf;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.junit.*;

import org.osgi.framework.BundleContext;
import org.osgi.service.jpa.EntityManagerFactoryBuilder;

import model.multiemf.Entity2;

import org.eclipse.gemini.jpa.test.common.JpaTest;

/**
 * Test class to test multiple EMF creation when PU had NO pre-existing 
 * defined datasource, etc.
 * 
 * @author mkeith
 */
public class TestMultiEMFNewProps extends JpaTest {
    
    public static final String TEST_NAME = "TestMultiEMFNewProps";
    public static final String PERSISTENCE_UNIT_UNDER_TEST = "MultiEMFNoProps";

    public static final String NEW_URL1 = "jdbc:derby://localhost:1527/firstDB;create=true";
    public static final String NEW_URL2 = "jdbc:derby://localhost:1527/secondDB;create=true";
    public static final String NEW_DRIVER_VERSION2 = "4.0";

	public static EntityManagerFactory emf1, emf2;
    public static EntityManagerFactoryBuilder emfb;
    public static BundleContext ctx;

    // The instance-specific EMF used by a given test
    public EntityManagerFactory emf;
    
    /* === Test Methods === */

    @BeforeClass
    public static void classSetUp() {
        sdebug(TEST_NAME, "In setup");
        emfb = lookupEntityManagerFactoryBuilder(TEST_NAME, PERSISTENCE_UNIT_UNDER_TEST, ctx);
        sdebug(TEST_NAME, "Got EMFBuilder - " + emfb);
    }

    @AfterClass
    public static void classCleanUp() throws Exception {
        if (emf1 != null) {
            emf1.close();
            emf1 = null;
        }
        if (emf2 != null) {
            emf2.close();
            emf2 = null;
        }
        emfb = null;
    }

    protected static Map<String,Object> buildProperties1() {
        Map<String,Object> props = new HashMap<String,Object>();
        return props;
    }

    protected static Map<String,Object> buildProperties2() {
        Map<String,Object> props = new HashMap<String,Object>();
        return props;
    }

    /* === Overridden test methods === */
    
    // Override these so we don't inherit any tests.
    // Manually run the test methods after we set the correct EMF
    @Override
    public void testGettingEntityManager() { debug("overriddenTestGettingEntityManager - noop"); }
    @Override
    public void testPersisting() { debug("overriddenTestPersisting - noop"); }
    @Override
    public void testFinding() { debug("overriddenTestFinding - noop"); }
    @Override
    public void testQuerying() { debug("overriddenTestQuerying - noop"); }
    @Override
    public void testGettingMetamodel() { debug("overriddenTestGettingMetamodel - noop"); }
    
    /* === Additional test methods === */
    @Test
    public void testNewEmf1() {
        debug("testEmf1");
        emf1 = emfb.createEntityManagerFactory(newProperties1());
        assert(emf1 != null);
        this.emf = emf1; 
        super.testGettingEntityManager();
        super.testPersisting();
        super.testFinding();
        super.testQuerying();
        super.testGettingMetamodel();
        this.emf = null;
    }
    
    @Test
    public void testNewEmf2() {
        debug("testNewEmf2");
        emf2 = emfb.createEntityManagerFactory(newProperties2());
        assert(emf2 != null);
        emf = emf2;
        super.testGettingEntityManager();
        super.testPersisting();
        super.testFinding();
        super.testQuerying();
        super.testGettingMetamodel();
    }
    
    protected Map<String,Object> newProperties1() {
        HashMap<String,Object> props = new HashMap<String, Object>();
        props.putAll(defaultProps());
        props.put("eclipselink.session-name","newSession1");
        props.put("javax.persistence.jdbc.url", NEW_URL1);
        return props;
    }

    protected Map<String,Object> newProperties2() {
        HashMap<String,Object> props = new HashMap<String, Object>();
        props.putAll(defaultProps());
        props.put("eclipselink.session-name","newSession2");
        props.put("osgi.jdbc.driver.version", NEW_DRIVER_VERSION2);
        props.put("javax.persistence.jdbc.url", NEW_URL2);
        return props;
    }    
    
    /* === Additional test methods === */
    @Test
    public void testEMF1() {
        debug("testEMF1");
        emf = emf1;
        testGettingEntityManager();
        testPersisting();
        testFinding();
        testQuerying();
        testGettingMetamodel();
    }

    @Test
    public void testEMF2() {
        debug("testEMF2");
        emf = emf2;
        testGettingEntityManager();
        testPersisting();
        testFinding();
        testQuerying();
        testGettingMetamodel();
    }

    /* === Subclassed methods === */

    @Override 
	public boolean needsDsfService() { return false; }

    @Override 
	public boolean needsEmfService() { return false; }

    @Override 
    public EntityManagerFactory getEmf() { return emf; }

    @Override 
    public String getTestPersistenceUnitName() { return PERSISTENCE_UNIT_UNDER_TEST; }

    @Override 
    public Object newObject() {
        Entity2 a = new Entity2();
        a.setId(100);
        return a;
    }
    
    @Override 
    public Object findObject() {
        EntityManager em = getEmf().createEntityManager();
        Object obj = em.find(Entity2.class, 100);        
        em.close();
        return obj;
    }

    @Override 
    public String queryString() {
        return "SELECT a FROM Entity2 a";
    }        
}
