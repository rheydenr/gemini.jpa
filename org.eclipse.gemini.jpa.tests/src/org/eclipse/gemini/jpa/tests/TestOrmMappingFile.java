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

import model.xmlmapped.SimpleEntity;

import org.junit.*;

/**
 * Test class for an entity mapped in orm.xml
 * 
 * @author mkeith
 */
public class TestOrmMappingFile extends JpaTest {
        
    public static final String TEST_NAME = "TestOrmMappingFile";
    public static final String PERSISTENCE_UNIT_UNDER_TEST = "XmlMapped";

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

    public EntityManagerFactory getEmf() { return emf; }

    public String getTestPersistenceUnitName() { return PERSISTENCE_UNIT_UNDER_TEST; }

    public Object newObject() {
        SimpleEntity a = new SimpleEntity();
        a.setId(10);
        a.setSimpleString("11");
        return a;
    }
    
    public Object findObject() {
        EntityManager em = emf.createEntityManager();
        Object obj = em.find(SimpleEntity.class, 1);
        em.close();
        return obj;
    }

    public Object queryObjects() {
        EntityManager em = emf.createEntityManager();
        List<?> result = em.createQuery("SELECT a FROM SimpleEntity a").getResultList();
        assert(result.size() == 1);
        em.close();
        return result;
    }
}
