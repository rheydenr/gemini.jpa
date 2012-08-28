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
 *     mkeith - Gemini JPA Configuration Generator sample 
 ******************************************************************************/
package gen;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.Configuration;

/**
 * Gemini JPA sample activator class
 * 
 * @author mkeith
 */
@SuppressWarnings({"rawtypes","unchecked"})
public class Activator implements BundleActivator {

    BundleContext ctx;
    ServiceTracker configAdminTracker;

    public void start(BundleContext context) throws Exception {
        ctx = context;

        configAdminTracker = new ServiceTracker(ctx, ConfigurationAdmin.class.getName(), null);
        configAdminTracker.open();

        new Thread() {
            public void run() { createConfig(); }
        }.start();
    }

    public void stop(BundleContext context) throws Exception {
        configAdminTracker.close();
    }
    
    /*=================*/
    /* Config creation */
    /*=================*/
    
    /* If config admin service is running then create a configuration. */
	/* Otherwise just hang around waiting for it to show up.           */
    protected void createConfig() {
        ConfigurationAdmin configAdmin;        
        do {
            configAdmin = (ConfigurationAdmin)configAdminTracker.getService();
            if (configAdmin == null)
                System.out.println("Gemini JPA Sample Configuration Generator waiting for config service...");
                try { Thread.sleep(3000); } catch (Exception e) {}
        } while (configAdmin == null);
		
        // Config admin service was found, create a config
        try {
            Configuration config = configAdmin.createFactoryConfiguration("gemini.jpa.punit", null);
            Dictionary props = new Hashtable();
            props.put("gemini.jpa.punit.name", "Library");

            props.put("eclipselink.target-database", "Derby");
            props.put("javax.persistence.jdbc.driver", "org.apache.derby.jdbc.ClientDriver");
            props.put("javax.persistence.jdbc.url", "jdbc:derby://localhost:1527/accountDB;create=true");
            props.put("javax.persistence.jdbc.user", "app");
            props.put("javax.persistence.jdbc.password", "app");

            props.put("eclipselink.logging.level", "FINE");
            props.put("eclipselink.logging.timestamp", "false");
            props.put("eclipselink.logging.thread", "false");
            props.put("eclipselink.logging.exceptions", "true");
            props.put("eclipselink.orm.throw.exceptions", "true");
            props.put("eclipselink.jdbc.read-connections.min", "1");
            props.put("eclipselink.jdbc.write-connections.min", "1");
            props.put("eclipselink.ddl-generation", "drop-and-create-tables");
            props.put("eclipselink.weaving", "true");
            
            config.update(props);
            System.out.println("Gemini JPA Sample Configuration Generator created configuration.");
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }    
}