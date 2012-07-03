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

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.jdbc.DataSourceFactory;
import org.osgi.service.jpa.EntityManagerFactoryBuilder;

import org.junit.*;

import org.eclipse.gemini.jpa.test.common.JpaTest;

/**
 * Test class to test looking up EMF Builder Service from a client
 * for a punit that does not a have data source props specified
 * 
 * @author mkeith
 */
@SuppressWarnings({"unchecked","rawtypes"})
public class TestEMFBuilderExternalDataSource extends JpaTest {

    public static final String TEST_NAME = "TestEMFBuilderExternalDataSource";
    public static final String PERSISTENCE_UNIT_UNDER_TEST = "AccountsNoDataSource";

    public static EntityManagerFactory emf;

	@BeforeClass
    public static void classSetUp() {
        slog(TEST_NAME, "In setup");
        EntityManagerFactoryBuilder emfb = lookupEntityManagerFactoryBuilder(TEST_NAME, PERSISTENCE_UNIT_UNDER_TEST);
        DataSource ds = null;
        try {
			ServiceReference[] refs = context.getServiceReferences(
                    DataSourceFactory.class.getName(), "(osgi.jdbc.driver.class=" + JDBC_TEST_DRIVER + ")");
            if (refs == null) {
                throw new RuntimeException("Failed looking up driver in registry");
            }

            DataSourceFactory dsf = (DataSourceFactory)context.getService(refs[0]);        	
        	if (dsf == null) {
        	    throw new RuntimeException("Failed getting svc from DSF svc ref");
        	}
            Properties props = new Properties();        
            props.put(DataSourceFactory.JDBC_URL, JDBC_TEST_URL);
            props.put(DataSourceFactory.JDBC_USER, JDBC_TEST_USER);
            props.put(DataSourceFactory.JDBC_PASSWORD, JDBC_TEST_PASSWORD);
            
            ds = dsf.createDataSource(props);

        } catch (InvalidSyntaxException e) {
            e.printStackTrace();
        } catch (SQLException e) {
			e.printStackTrace();
		}

        Map<String,Object> props = new HashMap<String,Object>();        
        props.put("javax.persistence.nonJtaDataSource", ds);
        
        emf = emfb.createEntityManagerFactory(props);
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

    public boolean needsEmfService() { return false; }
}
