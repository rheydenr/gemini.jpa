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
 *     mkeith - Gemini JPA JNDI test conditions generator 
 ******************************************************************************/
package org.eclipse.gemini.jpa.test.jndi.gen;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Properties;

import javax.sql.DataSource;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.jdbc.DataSourceFactory;

import org.eclipse.gemini.jpa.test.common.JpaTest;

import static org.osgi.service.jndi.JNDIConstants.JNDI_SERVICENAME;

/**
 * Activator to register a data source
 * 
 * @author mkeith
 */
public class Activator implements BundleActivator {

    // This string must match what is in the non-jta-data-source entry 
    //  in the persistence descriptor
    public static String JNDI_TEST_DS_NAME = "testDS";
    ServiceRegistration<DataSource> dsSvc;
                    
    public void start(BundleContext context) throws Exception {

        JpaTest.slog("JNDI test gen starting");

        /* Register a data source as a service for the test to look up */

        // Look up the DSF
        ServiceReference<?>[] refs = null;
        while ((refs == null) || (refs.length == 0)) {
            Thread.sleep(500);
            refs = context.getAllServiceReferences(
            	     DataSourceFactory.class.getName(), 
                     "(" + DataSourceFactory.OSGI_JDBC_DRIVER_CLASS + "=" + JpaTest.JDBC_TEST_DRIVER + ")");
        }
        // Create a data source 
        DataSourceFactory dsf = (DataSourceFactory) context.getService(refs[0]);
        Properties props = new Properties();
        props.put(DataSourceFactory.JDBC_URL, JpaTest.JDBC_TEST_URL);
        props.put(DataSourceFactory.JDBC_USER, JpaTest.JDBC_TEST_USER);
        props.put(DataSourceFactory.JDBC_PASSWORD, JpaTest.JDBC_TEST_PASSWORD);
        DataSource ds = dsf.createDataSource(props);
        JpaTest.slog("DS: " + ds);
        
        // Register the DS
        Dictionary<String,String> serviceProps = new Hashtable<String,String>();        
        serviceProps.put(JNDI_SERVICENAME, JNDI_TEST_DS_NAME);
        dsSvc = context.registerService(
                DataSource.class, ds, serviceProps);
        JpaTest.slog("Successfully registered DS");
    }
    
    public void stop(BundleContext context) throws Exception {
        JpaTest.slog("JNDI test gen stopping");
        dsSvc.unregister();
    }
}