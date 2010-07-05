/*
 * Copyright (C) 2010 Oracle Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.eclipse.gemini.jpa.tests;

import java.util.List;
import java.util.Set;

import javax.persistence.*;
import javax.persistence.metamodel.EntityType;

import org.junit.*;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.jpa.EntityManagerFactoryBuilder;

import model.account.*;

/**
 * Test class to test EMF Service from a client
 * 
 * @author mkeith
 */
public abstract class JpaTest {
    
    public static BundleContext context;        

    /* === Methods that *must* be subclassed === */

    public abstract EntityManagerFactory getEmf();

    /* === Methods that *may* be subclassed === */
    
    public Object newObject() {
        Account a = new Account();
        a.setBalance(100.0);
        return a;
    }
    
    public Object findObject() {
        EntityManager em = getEmf().createEntityManager();
        Object obj = em.find(Account.class, 1);
        em.close();
        return obj;
    }

    public Object queryObjects() {
        EntityManager em = getEmf().createEntityManager();
        List<?> result = em.createQuery("SELECT a FROM Account a").getResultList();
        em.close();
        return result;
    }

    /* === Test Methods === */
    
    @Test
    public void testGettingEntityManager() {
        log("testGettingEntityManager");
        EntityManager em = getEmf().createEntityManager();
        log("Got EM - " + em);
    }

    @Test
    public void testPersisting() {
        log("testPersisting");
        EntityManager em = getEmf().createEntityManager();
        Object obj = newObject();
        em.getTransaction().begin();
        log("testPersisting - tx begun");
        try {
            em.persist(obj);
        } catch (Exception e) {
            log("Error calling persist(): ");
            e.printStackTrace(System.out);
        }
        log("testPersisting - tx committing");
        em.getTransaction().commit();
        em.close();
        log("Persisted " + obj);
    }
    
    @Test
    public void testFinding() {
        log("testFinding");
        log("Find returned - " + findObject());
    }

    @Test
    public void testQuerying() {
        log("testQuerying");
        log("Query returned - " + queryObjects());
    }

    @Test
    public void testGettingMetamodel() {
        log("testGettingMetamodel");
        EntityManager em = getEmf().createEntityManager();
        Set<EntityType<?>> s = em.getMetamodel().getEntities();
        for (EntityType<?> et : s) {
            log("Managed Entity name: " + et.getName());
            log("Managed Entity class: " + et.getJavaType());
            log("Classloader: " + et.getJavaType().getClassLoader());
        }
    }
    
    /* === Helper methods === */

    public static EntityManagerFactory lookupEntityManagerFactory(String testName, String puName) {
        String filter = "(osgi.unit.name="+puName+")";
        ServiceReference[] refs = null;
        try {
            refs = context.getServiceReferences(EntityManagerFactory.class.getName(), filter);
        } catch (InvalidSyntaxException isEx) {
            new RuntimeException("Bad filter", isEx);
        }
        slog(testName, "EMF Service refs looked up from registry: " + refs);
        return (refs == null)
            ? null
            : (EntityManagerFactory) context.getService(refs[0]);
    }
    
    static EntityManagerFactoryBuilder lookupEntityManagerFactoryBuilder(String testName, String puName) {
        String filter = "(osgi.unit.name="+puName+")";
        ServiceReference[] refs = null;
        try {
            refs = context.getServiceReferences(EntityManagerFactoryBuilder.class.getName(), filter);
        } catch (InvalidSyntaxException isEx) {
            new RuntimeException("Bad filter", isEx);
        }
        slog(testName, "EMF Builder Service refs looked up from registry: " + refs);
        return (refs == null)
            ? null
            : (EntityManagerFactoryBuilder) context.getService(refs[0]);
    }

    static void slog(String testName, String msg) {
        System.out.println("***** " + testName + " - " + msg);
    }    
    void log(String msg) {
        System.out.println("***** " + this.getClass().getSimpleName() + " - " + msg);
    }    
}
