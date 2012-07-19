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
package org.eclipse.gemini.jpa.test.common;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.*;
import javax.persistence.metamodel.EntityType;

import org.junit.*;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.jpa.EntityManagerFactoryBuilder;

/**
 * Abstract test class for JPA EMF Services
 * 
 * @author mkeith
 */
@SuppressWarnings({"unchecked","rawtypes"})
public abstract class JpaTest {
    
    // Define the JDBC access details. At some point we should probably externalize this...
    public static final String JDBC_TEST_DRIVER = "org.apache.derby.jdbc.ClientDriver";
    public static final String JDBC_TEST_URL = "jdbc:derby://localhost:1527/accountDB;create=true";
    public static final String JDBC_TEST_USER = "app";
    public static final String JDBC_TEST_PASSWORD = "app";

    // Put the JDBC access config in the default props
    public static Map<String,Object> defaultProps() {
        Map<String,Object> props = new HashMap<String,Object>();        
        props.put("javax.persistence.jdbc.driver", JDBC_TEST_DRIVER);
        props.put("javax.persistence.jdbc.url", JDBC_TEST_URL);
        props.put("javax.persistence.jdbc.user", JDBC_TEST_USER);
        props.put("javax.persistence.jdbc.password", JDBC_TEST_PASSWORD);
        return props;
    }
    
    public static boolean debug = true;
    
    /*==============================*/
    /* Methods *must* be subclassed */
    /*==============================*/

    public abstract EntityManagerFactory getEmf();

    public abstract String getTestPersistenceUnitName();
    
	public abstract Object newObject();
    
    public abstract Object findObject(); 
	
	public abstract String queryString();
	
    /*=============================*/
    /* Methods *may* be subclassed */
    /*=============================*/

    public boolean needsEmfService() { return true; }

    public boolean needsDsfService() { return true; }

    public String testName() {
        return this.getClass().getSimpleName();
    }

	public Object queryObjects() {
        EntityManager em = getEmf().createEntityManager();
        List<?> result = em.createQuery(this.queryString()).getResultList();
        em.close();
        return result;
    }

    /*==============*/
    /* Test Methods */
    /*==============*/
    
    @Test
    public void testGettingEntityManager() {
        debug("testGettingEntityManager");
        EntityManager em = getEmf().createEntityManager();
        debug("Got EM - " + em);
    }

    @Test
    public void testPersisting() {
        debug("testPersisting");
        EntityManager em = getEmf().createEntityManager();
        Object obj = newObject();
        em.getTransaction().begin();
        debug("testPersisting - tx begun");
        try {
            em.persist(obj);
        } catch (Exception e) {
            log("Error calling persist(): ");
            e.printStackTrace(System.out);
        }
        debug("testPersisting - tx committing");
        em.getTransaction().commit();
        em.close();
        debug("Persisted " + obj);
    }
    
    @Test
    public void testFinding() {
        debug("testFinding");
        debug("Find returned - " + findObject());
    }

    @Test
    public void testQuerying() {
        debug("testQuerying");
        debug("Query returned - " + queryObjects());
    }

    @Test
    public void testGettingMetamodel() {
        debug("testGettingMetamodel");
        EntityManager em = getEmf().createEntityManager();
        Set<EntityType<?>> s = em.getMetamodel().getEntities();
        for (EntityType<?> et : s) {
            debug("Managed Entity name: " + et.getName());
            debug("Managed Entity class: " + et.getJavaType());
            debug("Classloader: " + et.getJavaType().getClassLoader());
        }
    }
    
    /*================*/
    /* Helper methods */
    /*================*/

    public static boolean getDebug() { return debug; }
    
    public static EntityManagerFactory lookupEntityManagerFactory(String testName, String puName, BundleContext ctx) {
        String filter = "(osgi.unit.name="+puName+")";
        ServiceReference[] refs = null;
        try {
            refs = ctx.getServiceReferences(EntityManagerFactory.class.getName(), filter);
        } catch (InvalidSyntaxException isEx) {
            new RuntimeException("Bad filter", isEx);
        }
        sdebug(testName, "EMF Service refs looked up from registry: " + refs);
        return (refs == null)
            ? null
            : (EntityManagerFactory) ctx.getService(refs[0]);
    }
    
    public static EntityManagerFactoryBuilder lookupEntityManagerFactoryBuilder(String testName, String puName, BundleContext ctx) {
        String filter = "(osgi.unit.name="+puName+")";
        ServiceReference[] refs = null;
        try {
            refs = ctx.getServiceReferences(EntityManagerFactoryBuilder.class.getName(), filter);
        } catch (InvalidSyntaxException isEx) {
            new RuntimeException("Bad filter", isEx);
        }
        sdebug(testName, "EMF Builder Service refs looked up from registry: " + refs);
        return (refs == null)
            ? null
            : (EntityManagerFactoryBuilder) ctx.getService(refs[0]);
    }

    public static void sdebug(String msg) { if (getDebug()) slog(msg); }
    public static void sdebug(String testName, String msg) { if (getDebug()) slog(testName, msg); } 
    public static void slog(String msg) { out("***** - ", msg); }
    public static void slog(String testName, String msg) { out("***** ",testName," - ",msg); }
    public static void out(String... msgs) {
        for (String msg : msgs) 
            System.out.print(msg);
        System.out.println();
    }
    
    public void debug(String msg) { if (getDebug()) log(msg); }
    public void log(String msg) { out("***** ", this.getClass().getSimpleName(), " - ", msg); }

    // Method used during testing to obtain Gemini p-unit configuration information
    public Map<String,Object> getPUnitConfigInfo() {
        return (Map<String,Object>) getEmf().getProperties().get("PUnitInfo");
    }
}
