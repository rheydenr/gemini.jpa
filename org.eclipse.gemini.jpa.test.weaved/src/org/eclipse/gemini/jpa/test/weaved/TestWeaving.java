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

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.junit.*;

import model.weaved.WeavedEntity;

/**
 * Test class to test weaving of JPA provider
 * 
 * @author mkeith
 */
public class TestWeaving extends JpaTest {
    
    public static final String TEST_NAME = "TestWeaving";
    public static final String PERSISTENCE_UNIT_UNDER_TEST = "Weaved";

    public static EntityManagerFactory emf;

    /* === Test Methods === */

    @BeforeClass
    public static void classSetUp() {
        slog(TEST_NAME, "In setup");
        emf = lookupEntityManagerFactory(TEST_NAME, PERSISTENCE_UNIT_UNDER_TEST);                
        slog(TEST_NAME, "Got EMF - " + emf);        
    }

    @AfterClass
    public static void classCleanUp() throws Exception {
        if (emf != null) {
            emf.close();
            emf = null;
        }
    }

    /* === Additional test methods === */
    @Test
    public void testWovenClass() {
        log("testWovenClass");
        WeavedEntity entity = new WeavedEntity();
        boolean weaved = false;

        Class<?>[] clsArray = entity.getClass().getInterfaces();
        for ( Class<?> cls : clsArray) {
            if (cls.toString().equals("org.eclipse.persistence.internal.weaving.PersistenceWeaved")) {
                weaved = true;
                break;
            }
        }
        assert(weaved) ;
    }
    
    /* === Subclassed methods === */

    // Dues to Lazy EMF creation, and the fact that we are never actually invoking  
    // methods on the EMF, the real underlying provider EMF is never really created
    public boolean needsDsfService() { return false; }

    public EntityManagerFactory getEmf() { return emf; }

    public String getTestPersistenceUnitName() { return PERSISTENCE_UNIT_UNDER_TEST; }

    public Object newObject() {
        WeavedEntity a = new WeavedEntity();
        a.setId(100);
        return a;
    }
    
    public Object findObject() {
        EntityManager em = getEmf().createEntityManager();
        Object obj = em.find(WeavedEntity.class, 100);        
        em.close();
        return obj;
    }

    public Object queryObjects() {
        EntityManager em = getEmf().createEntityManager();
        List<?> result = em.createQuery("SELECT a FROM WeavedEntity a").getResultList();
        em.close();
        return result;
    }        
}
