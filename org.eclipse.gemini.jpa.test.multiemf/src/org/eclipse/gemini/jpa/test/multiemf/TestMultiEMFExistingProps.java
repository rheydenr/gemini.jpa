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

import model.multiemf.Entity1;

import org.eclipse.gemini.jpa.test.common.JpaTest;

/**
 * Test class to test multiple EMF creation when PU had pre-existing 
 * defined datasource, etc.
 * 
 * @author mkeith
 */
public class TestMultiEMFExistingProps extends JpaTest {
    
    public static final String TEST_NAME = "TestMultiEMFExistingProps";
    public static final String PERSISTENCE_UNIT_UNDER_TEST = "MultiEMFExistingProps";

    public static final String NEW_URL = "jdbc:derby://localhost:1527/diffDB;create=true";
    
    public static EntityManagerFactory initialEmf, newEmf;
	public static EntityManagerFactoryBuilder emfb;
    public static BundleContext ctx;

    // The instance-specific EMF used by a given test
    public EntityManagerFactory emf;
    
    /* === Test Methods === */

    @BeforeClass
    public static void classSetUp() {
        sdebug(TEST_NAME, "In setup");
        initialEmf = lookupEntityManagerFactory(TEST_NAME, PERSISTENCE_UNIT_UNDER_TEST, ctx);
        sdebug(TEST_NAME, "Got EMF - " + initialEmf);
        emfb = lookupEntityManagerFactoryBuilder(TEST_NAME, PERSISTENCE_UNIT_UNDER_TEST, ctx);
        sdebug(TEST_NAME, "Got EMFBuilder - " + emfb);
    }

    @AfterClass
    public static void classCleanUp() throws Exception {
        if (initialEmf != null) {
            initialEmf.close();
            initialEmf = null;
        }
        if (newEmf != null) {
            newEmf.close();
            newEmf = null;
        }
        emfb = null;
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
    public void testExistingEmf() {
        debug("testExistingEmf");
        this.emf = initialEmf; 
        super.testGettingEntityManager();
        super.testPersisting();
        super.testFinding();
        super.testQuerying();
        super.testGettingMetamodel();
        this.emf = null; 
    }
    @Test
    public void testCreateNewEmf() {
        debug("testCreateNewEmf");
        newEmf = emfb.createEntityManagerFactory(newProperties());
        assert(newEmf != null);
        emf = newEmf;
        super.testGettingEntityManager();
        super.testPersisting();
        super.testFinding();
        super.testQuerying();
        super.testGettingMetamodel();
    }
    
    protected Map<String,Object> newProperties() {
        HashMap<String,Object> props = new HashMap<String, Object>();
        props.putAll(defaultProps());
        props.put("eclipselink.session-name","newPropsSession");
        props.put("javax.persistence.jdbc.url", NEW_URL);
        return props;
    }
    /* === Subclassed methods === */

    @Override 
    public boolean needsDsfService() { return false; }
    
    // Set this to false to exploit the fact that EMF is created first so if  
    // we demand an EMFB we will ensure we have both EMF and EMFB
    @Override 
    public boolean needsEmfService() { return false; }

    @Override 
    public EntityManagerFactory getEmf() { return emf; }

    @Override 
    public String getTestPersistenceUnitName() { return PERSISTENCE_UNIT_UNDER_TEST; }

    @Override 
    public Object newObject() {
        Entity1 a = new Entity1();
        a.setId(100);
        return a;
    }
    
    @Override 
    public Object findObject() {
        EntityManager em = getEmf().createEntityManager();
        Object obj = em.find(Entity1.class, 100);        
        em.close();
        return obj;
    }

    @Override 
    public String queryString() {
        return "SELECT a FROM Entity1 a";
    }        
}
