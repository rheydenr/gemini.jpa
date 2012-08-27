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
package org.eclipse.gemini.jpa.test.configadmin.gen;

import java.util.Dictionary;
import java.util.Hashtable;

import org.eclipse.gemini.jpa.test.common.JpaTest;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleActivator;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.Configuration;
import org.osgi.util.tracker.ServiceTracker;

import static org.eclipse.gemini.jpa.test.common.JpaTest.*;

/**
 * Activator to generate config when ConfigAdmin service becomes available
 * 
 * @author mkeith
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class Activator implements BundleActivator {

    // for configadmin.pu
    public static final String PERSISTENCE_UNIT_UNDER_TEST = "ConfigAdminAccounts";
    public static final String PERSISTENCE_UNIT_BSN_UNDER_TEST = "org.eclipse.gemini.jpa.test.configadmin.pu";

    // for configadmin.pu2
    public static final String PERSISTENCE_UNIT2_UNDER_TEST = "ConfigAdminAccounts2";

	ServiceTracker configAdminTracker;
    ConfigurationAdmin configAdmin;
	
    /*===================*/
    /* Lifecycle methods */
    /*===================*/
	
    public void start(BundleContext context) throws Exception {
        // Spawn off a thread to get config admin service and create configs
        final BundleContext ctx = context;
        (new Thread() {
            public void run() {
                ServiceTracker configAdminTracker = new ServiceTracker(ctx, ConfigurationAdmin.class, null);
                configAdminTracker.open();
                JpaTest.sdebug("Waiting for config admin service...");
                do {
                    configAdmin = (ConfigurationAdmin)configAdminTracker.getService();
                    if (configAdmin == null)
                        try { Thread.sleep(3000); }
                        catch (Exception e) {}
                } while (configAdmin == null);
                JpaTest.sdebug("Config Admin service acquired");
                createSingleConfig();
                createIncrementalConfig();
            }
        }).start();
    }

    public void stop(BundleContext context) throws Exception {
        configAdminTracker.close();
    }
	
    protected void createSingleConfig() {
        try {
            Configuration[] c = configAdmin.listConfigurations("(service.factoryPid=gemini.jpa.punit)");
            sdebug("ConfigGen: Existing configs: " + ((c != null) ? c.length : ""));
            Configuration config = configAdmin.createFactoryConfiguration("gemini.jpa.punit", null);
            if (config.getProperties() == null) {
                sdebug("ConfigGen creating standalone config...");
                Dictionary props = new Hashtable();
                props.put("gemini.jpa.punit.name", PERSISTENCE_UNIT_UNDER_TEST);
                props.put("gemini.jpa.punit.bsn", PERSISTENCE_UNIT_BSN_UNDER_TEST);
                props.put("gemini.jpa.punit.classes", 
                          "model.ca.Account, model.ca.Customer, model.ca.Transaction, model.ca.TxOperation");
                props.put("javax.persistence.jdbc.driver", "org.apache.derby.jdbc.ClientDriver");
                props.put("javax.persistence.jdbc.url", "jdbc:derby://localhost:1527/accountDB;create=true");
                props.put("javax.persistence.jdbc.user", "app");
                props.put("javax.persistence.jdbc.password", "app");
                config.update(props);
                sdebug("Test standalone config created, pid=", config.getPid());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }
    protected void createIncrementalConfig() {
        try {
            Configuration[] c = configAdmin.listConfigurations("(service.factoryPid=gemini.jpa.punit)");
            sdebug("ConfigGen: Existing configs: " + ((c != null) ? c.length : ""));
            Configuration config = configAdmin.createFactoryConfiguration("gemini.jpa.punit", null);
            if (config.getProperties() == null) {
                sdebug("ConfigGen creating incremental config...");
                Dictionary props = new Hashtable();
                props.put("gemini.jpa.punit.name", PERSISTENCE_UNIT2_UNDER_TEST);
                props.put("javax.persistence.jdbc.driver", "org.apache.derby.jdbc.ClientDriver");
                props.put("javax.persistence.jdbc.url", "jdbc:derby://localhost:1527/accountDB;create=true");
                props.put("javax.persistence.jdbc.user", "app");
                props.put("javax.persistence.jdbc.password", "app");
                config.update(props);
                sdebug("Test incremental config created, pid=", config.getPid());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }
}