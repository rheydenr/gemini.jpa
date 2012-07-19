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
 *     mkeith - Gemini JPA work 
 *     ssmith - EclipseLink integration
 ******************************************************************************/
package org.eclipse.gemini.jpa.provider;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.ProviderUtil;
import javax.sql.DataSource;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.jdbc.DataSourceFactory;

import static org.osgi.service.jdbc.DataSourceFactory.JDBC_PASSWORD;
import static org.osgi.service.jdbc.DataSourceFactory.JDBC_URL;
import static org.osgi.service.jdbc.DataSourceFactory.JDBC_USER;

import org.eclipse.persistence.config.PersistenceUnitProperties;
import org.eclipse.persistence.internal.jpa.deployment.PersistenceUnitProcessor;
import org.eclipse.persistence.logging.AbstractSessionLog;
import org.eclipse.persistence.logging.DefaultSessionLog;
import org.eclipse.persistence.logging.SessionLog;

import org.eclipse.gemini.jpa.AnchorClassUtil;
import org.eclipse.gemini.jpa.FragmentUtil;
import org.eclipse.gemini.jpa.GeminiSystemProperties;
import org.eclipse.gemini.jpa.GeminiPersistenceUnitProperties;
import org.eclipse.gemini.jpa.GeminiServicesUtil;
import org.eclipse.gemini.jpa.GeminiUtil;
import org.eclipse.gemini.jpa.PUnitInfo;
import org.eclipse.gemini.jpa.PersistenceBundleExtender;
import org.eclipse.gemini.jpa.PlainDriverDataSource;
import org.eclipse.gemini.jpa.classloader.BundleProxyClassLoader;
import org.eclipse.gemini.jpa.classloader.CompositeClassLoader;

import static org.eclipse.gemini.jpa.GeminiUtil.debug;
import static org.eclipse.gemini.jpa.GeminiUtil.debugClassLoader;
import static org.eclipse.gemini.jpa.GeminiUtil.fatalError;
import static org.eclipse.gemini.jpa.GeminiUtil.warning;

//TODO Add substitutability of provider

@SuppressWarnings({"rawtypes","unchecked"})
public class EclipseLinkOSGiProvider implements BundleActivator, 
                                                OSGiJpaProvider,
                                                PersistenceProvider {
    /*==================*/
    /* Static constants */
    /*==================*/
    public static final String PROVIDER_CLASS_NAME = "org.eclipse.persistence.jpa.PersistenceProvider";    
    
    public static final int MAX_EVENT_COLLISION_TRIES = 3;
    
    /*================*/
    /* Provider state */
    /*================*/
    
    /** Provider bundle context */
    BundleContext ctx;

    /** Extender code to find and process persistence unit bundles */
    PersistenceBundleExtender extender;

    /** Services utility code */
    GeminiServicesUtil servicesUtil;
    
    /** Anchor class gen utility */
    AnchorClassUtil anchorUtil;    
    
    /** A native SPI instance of this provider */
    PersistenceProvider eclipseLinkProvider;
    
    /** Map of p-units we have registered */
    Map<String, PUnitInfo> pUnitsByName;
    
    private FileWriter eclipseLinkLog;

    /*=====================*/
    /* Activator functions */
    /*=====================*/
    
    public void start(BundleContext context) throws Exception {
        
        debug("EclipseLinkProvider starting...");

        // Initialize our state
        ctx = context;
        pUnitsByName = Collections.synchronizedMap(new HashMap<String, PUnitInfo>());
        extender = new PersistenceBundleExtender(this);
        anchorUtil = new AnchorClassUtil(GeminiSystemProperties.generateAnchorClasses());
        servicesUtil = new GeminiServicesUtil(this, anchorUtil);
        openEclipseLinkLoggingFile();
        eclipseLinkProvider = new org.eclipse.gemini.jpa.provider.PersistenceProvider();
        PersistenceUnitProcessor.setArchiveFactory(new OSGiArchiveFactoryImpl());
        
        // Register as a provider 
        servicesUtil.registerProviderService();

        // Kick the extender to go looking for persistence bundles
        extender.startListening();
        extender.lookForExistingBundles();
        debug("EclipseLinkProvider started");
    }
    
    public void stop(BundleContext context) throws Exception {

        debug("EclipseLinkProvider stopping...");

        // Take the extender offline and unregister ourselves as a provider
        extender.stopListening();
        servicesUtil.unregisterProviderService();
        
        // Unregister all of the persistence units that we have registered
        List<PUnitInfo> pUnits = new ArrayList<PUnitInfo>(); // Need a new copy
        pUnits.addAll(pUnitsByName.values());
        unregisterPersistenceUnits(pUnits);
        pUnitsByName = null;
        
        // Now unassign all of the persistence units that have been assigned to us
        Map<Bundle,List<PUnitInfo>> pUnitInfos = extender.clearAllPUnitInfos();
        for (Map.Entry<Bundle,List<PUnitInfo>> entry : pUnitInfos.entrySet()) {
            unassignPersistenceUnitsInBundle(entry.getKey(), entry.getValue());
        }
        closeEclipseLinkLoggingFile();
        debug("EclipseLinkProvider stopped");
    }
    
    /*==============================*/
    /* OSGiJpaProvider impl methods */
    /*==============================*/

    // Used to compare against the <provider> element in persistence descriptors
    public String getProviderClassName() { return PROVIDER_CLASS_NAME; }

    // Used to invoke regular JPA createEntityManagerFactory() methods
    public javax.persistence.spi.PersistenceProvider getProviderInstance() { 
        return this;
    }

    public Bundle getBundle() { return ctx.getBundle(); }
    
    public BundleContext getBundleContext() { return ctx; }
    
    /**
     * Assignment happens before resolution. This callback offers the provider a chance to do 
     * anything that must be done before the bundle is resolved.
     * 
     * @param b
     * @param pUnits
     */
    public void assignPersistenceUnitsInBundle(Bundle b, Collection<PUnitInfo> pUnits) {
        //TODO Check state of bundle in assign call
        debug("EclipseLinkProvider assignPersistenceUnitsInBundle: ", b);
        
        if (GeminiSystemProperties.generateFragments()) {
            // Generate a fragment for the p-units
            new FragmentUtil(ctx.getBundle())
                    .generateAndInstallFragment(b, pUnits, anchorUtil);
        }

        // Create a loader that can load from the persistence bundle as well as from the provider bundle
        ClassLoader compositeLoader = compositeLoader(getBundleContext(), b);
        
        // Create and run initializer to process PU and register transformers
        GeminiOSGiInitializer initializer = new GeminiOSGiInitializer();        
        initializer.initializeFromBundle(getBundleContext(), b, compositeLoader, pUnits);
    }

    /**
     * The persistence bundle is resolved. In this callback the provider 
     * must register the persistence unit services in the registry.
     * 
     * @param pUnits Usually, but not always, all in the same bundle
     */
    public void registerPersistenceUnits(Collection<PUnitInfo> pUnits) {
        
        debug("EclipseLinkProvider registerPersistenceUnits: ", pUnits);

        if (pUnits == null) return;

        for (PUnitInfo info : pUnits) {
            String pUnitName = info.getUnitName();
            int attempts = 0;

            if (pUnitsByName.containsKey(pUnitName)) {
                // Shouldn't be in the map. Race condition - 
                // Either the bundle is already being registered or 
                // it's being unregistered because of being stopped 
                PUnitInfo existingInfo = pUnitsByName.get(pUnitName);
                if ((existingInfo != null) && 
                    (existingInfo.getBundle() == info.getBundle())) {
                    // It is the same bundle - move along and assume it will be registered
                    continue;
                }
                while (pUnitsByName.containsKey(pUnitName) && (attempts < MAX_EVENT_COLLISION_TRIES)) {
                    // The previous entry just hasn't been removed yet. Take a short
                    // break and give a chance for the unregister to occur.
                    try { Thread.sleep(1000); } catch (InterruptedException iEx) {}
                    attempts++;
                } 
                if (pUnitsByName.containsKey(pUnitName)) {
                    // It's still there. Take matters into our own hands and force the unregister
                    warning("EclipseLinkProvider forcing unregister of persistence unit: " + info.getUnitName());
                    Collection<PUnitInfo> units = new ArrayList<PUnitInfo>();
                    units.add(info);
                    unregisterPersistenceUnits(units);
                }
            }

            // Keep a local copy of all of the p-units we are registering
            pUnitsByName.put(pUnitName, info); 
            // Do the registering
            servicesUtil.registerEMFServices(info);
        }
    }

    /**
     * In this callback the provider must unregister the persistence unit services 
     * from the registry and clean up any resources.
     * 
     * @param pUnits Usually, but not always, all in the same bundle
     */
    public void unregisterPersistenceUnits(Collection<PUnitInfo> pUnits) {

        debug("EclipseLinkProvider unregisterPersistenceUnits: ", pUnits);

        if (pUnits == null) return;
        
        for (PUnitInfo info : pUnits) {
            servicesUtil.unregisterEMFServices(info);

            // Remove from our local pUnit copy 
            pUnitsByName.remove(info.getUnitName());
        }
    }

    public void unassignPersistenceUnitsInBundle(Bundle b, Collection<PUnitInfo> pUnits) {
        debug("EclipseLinkProvider unassignPersistenceUnitsInBundle: ", b.getSymbolicName());
    }
    
    /*=============================*/
    /* PersistenceProvider methods */
    /*=============================*/
    
    /**
     * Intercept calls to the OSGi EclipseLink JPA provider so we can insert 
     * classloader, data source and descriptor properties that can be used by
     * EclipseLink to sort things out.
     */
    public EntityManagerFactory createEntityManagerFactory(String emName, Map properties) {
        
        debug("EclipseLinkProvider createEMF invoked for p-unit: ", emName);
        debug("Properties map: ", properties);

        PUnitInfo pUnitInfo = pUnitsByName.get(emName);
        if (pUnitInfo == null)
            fatalError("createEntityManagerFactory() called on provider, but provider has not registered the p-unit " + emName, null);

        Map<String,Object> props = new HashMap<String,Object>();
        props.putAll(properties);
        
        // Create a composite loader that loads from the punit bundle and the provider bundle
        CompositeClassLoader compositeLoader = compositeLoader(pUnitInfo);
        // Bug 385170 - If user supplies a classloader then tack it on the front
        if (props.containsKey(PersistenceUnitProperties.CLASSLOADER)) {
            ClassLoader userLoader = (ClassLoader) props.get(PersistenceUnitProperties.CLASSLOADER);
            compositeLoader.getClassLoaders().add(0, userLoader);
        }
        props.put(PersistenceUnitProperties.CLASSLOADER, compositeLoader);

        DataSource ds = acquireDataSource(pUnitInfo, properties);
        if (ds != null) 
            props.put(PersistenceUnitProperties.NON_JTA_DATASOURCE, ds);
        
        props.put(PersistenceUnitProperties.ECLIPSELINK_PERSISTENCE_XML, fullDescriptorPath(pUnitInfo));

        props.put(GeminiOSGiInitializer.OSGI_BUNDLE, pUnitInfo.getBundle());
        
        EntityManagerFactory emf = eclipseLinkProvider.createEntityManagerFactory(emName, props);
        return emf;
    }

    /**
     * NOTE: This method is not supported or tested.
     * An effort is made to try anyway, though, rather than just error out.
     * (Worst that can happen is it doesn't work).
     */
    public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo info, Map properties) {

        String pUnitName = info.getPersistenceUnitName();
        warning("Container JPA not currently supported for p-unit ", pUnitName);
        
        // Can't hurt to go ahead and try, though...
        PUnitInfo pUnitInfo = pUnitsByName.get(pUnitName);
        if (pUnitInfo == null)
            fatalError("createContainerEntityManagerFactory() called on provider, but provider has not registered the p-unit " + pUnitName, null);
        Map<String,Object> props = new HashMap<String,Object>();
        props.putAll(properties);
        props.put(PersistenceUnitProperties.CLASSLOADER, compositeLoader(pUnitInfo));
        DataSource ds = acquireDataSource(pUnitInfo, properties);
        if (ds != null) 
            props.put(PersistenceUnitProperties.NON_JTA_DATASOURCE, ds);
        props.put(PersistenceUnitProperties.ECLIPSELINK_PERSISTENCE_XML, fullDescriptorPath(pUnitInfo));
        props.put(GeminiOSGiInitializer.OSGI_BUNDLE, pUnitInfo.getBundle());

        return eclipseLinkProvider.createContainerEntityManagerFactory(info, props);
    }

    public ProviderUtil getProviderUtil() { 
        debug("EclipseLinkProvider getProviderUtil invoked");
        return eclipseLinkProvider.getProviderUtil(); 
    }

    /*================*/
    /* Helper methods */
    /*================*/

    protected CompositeClassLoader compositeLoader(PUnitInfo pUnitInfo) {
        return compositeLoader(getBundleContext(), pUnitInfo.getBundle());
    }

    protected CompositeClassLoader compositeLoader(BundleContext providerCtx, Bundle pUnitBundle) {
        ClassLoader pUnitLoader = new BundleProxyClassLoader(pUnitBundle);
        debugClassLoader("PUnit bundle proxy loader created: ", pUnitLoader);
        ClassLoader providerLoader = new BundleProxyClassLoader(providerCtx.getBundle());
        debugClassLoader("Provider bundle proxy loader created: ", providerLoader);
        List<ClassLoader> loaders = new ArrayList<ClassLoader>();
        loaders.add(pUnitLoader);
        loaders.add(providerLoader);
        CompositeClassLoader compositeLoader = new CompositeClassLoader(loaders);
        debugClassLoader("Composite loader created: ", compositeLoader);
        return compositeLoader;
    }

    protected DataSource acquireDataSource(PUnitInfo pUnitInfo, Map<?,?> properties) {

        // If an actual data source object was passed in then just return it and 
        // let it be re-added to the properties map by the caller  
        // ### Enhancement for bug 335983 - Contributed by Eduard Bartsch ###
        Object ds = properties.get(PersistenceUnitProperties.NON_JTA_DATASOURCE);
        if (ds instanceof DataSource) {
            return (DataSource) ds;
        }
        
        // Support the case of a provider-connected data source. In this case the provider
        // will be responsible for connecting to a data source (using specified properties)
        // ### Enhancement for bug 369029 - Ideas contributed by Gunnar W ###
        if (properties.containsKey(GeminiPersistenceUnitProperties.PROVIDER_CONNECTED_DATA_SOURCE)) {
            return null;
        }
        
        Driver driver = null;

        // Sort out which named driver we are dealing with
        String driverName = (String)properties.get(GeminiUtil.JPA_JDBC_DRIVER_PROPERTY);
        String driverVersion = (String)properties.get(GeminiUtil.OSGI_JDBC_DRIVER_VERSION_PROPERTY);
        if (driverName == null) {
            driverName = pUnitInfo.getDriverClassName();
            if (driverName == null) {
                // We at least need a driver name
                fatalError("No driver was specified", null);
            } else {
                // No driver was specified in props so take the version from p-unit as well, if it's there
                if (driverVersion == null)
                    driverVersion = pUnitInfo.getDriverVersion();
            }
        }

        // Try using a DSF service if we have one stored away and the one asked for is the same
        ServiceReference dsfRef = pUnitInfo.getDsfService();
        if ((dsfRef != null) && (driverName.equals(pUnitInfo.getDriverClassName()))) {
            debug("Using existing DSF service ref from punit ", pUnitInfo.getUnitName());
            DataSourceFactory dsf = (DataSourceFactory) getBundleContext().getService(dsfRef);
            try {
                // There is no standard way of getting JDBC properties from JPA props
                // (apart from the url/user/pw that are converted using getJdbcProperties)
                driver = dsf.createDriver(null);
            } catch (SQLException sqlEx) {
                // Service was registered but seems to be busted
                fatalError("Could not create data source for " + driverName, sqlEx);
            }
        }
        // If we still have no driver then try doing a dynamic lookup
        if (driver == null) {
            debug("Trying dynamic lookup of DSF for ", driverName, " for p-unit ", pUnitInfo.getUnitName());
            String filter = servicesUtil.filterForDSFLookup(driverName, driverVersion);
            ServiceReference[] dsfRefs = servicesUtil.lookupDSF(pUnitInfo.getBundle().getBundleContext(), filter);
            if (dsfRefs != null) {
                debug("Found DSF, props: ", GeminiUtil.serviceProperties(dsfRefs[0]));
                DataSourceFactory dsf = (DataSourceFactory) getBundleContext().getService(dsfRefs[0]);
                try {
                    driver = dsf.createDriver(null);
                } catch (SQLException sqlEx) {
                    fatalError("Could not create data source for " + driverName, sqlEx);
                }
            }
        }
        // Finally, try loading it locally
        if (driver == null) {
            debug("Trying to load driver ", driverName, " locally from p-unit bundle ", pUnitInfo.getUnitName());
            try {
                Class<?> driverClass = pUnitInfo.getBundle().loadClass(driverName);
                driver = (Driver) driverClass.newInstance();
                debug("JDBC driver ", driverName, " loaded locally from p-unit bundle ", pUnitInfo.getUnitName());
            } catch (Exception ex) {
                fatalError("Could not create data source for " + driverName, ex);
            }
        }
        Properties props = getJdbcProperties(pUnitInfo, properties);
        
        return new PlainDriverDataSource(driver, props);
    }
    
    protected String fullDescriptorPath(PUnitInfo pUnitInfo) {
        return pUnitInfo.getDescriptorInfo().fullDescriptorPath();
    }

    /*
     * Return the current JDBC url, user and password properties. The props set in the
     * XML file (from pUnitInfo) will be overridden by any properties of the same name
     * in the runtime properties Map passed in. The resulting properties will be passed
     * to a JDBC driver.
     */
    protected Properties getJdbcProperties(PUnitInfo pUnitInfo, Map<?,?> properties) {

        Properties props = new Properties();
        
        // Get the 3 driver properties, if they exist (url, user, password)
        debug("EclipseLinkProvider - getJDBCProperties");
        debug("  fromMap: ", properties);
        debug("  fromDescriptor: ", pUnitInfo);

        String url = (String)properties.get(GeminiUtil.JPA_JDBC_URL_PROPERTY);
        if (url == null)
            url = pUnitInfo.getDriverUrl();        
        if (url != null) 
            props.put(JDBC_URL, url);
        
        String user = (String)properties.get(GeminiUtil.JPA_JDBC_USER_PROPERTY);
        if (user == null)
            user = pUnitInfo.getDriverUser();        
        if (user != null) 
            props.put(JDBC_USER, user);

        String pw = (String)properties.get(GeminiUtil.JPA_JDBC_PASSWORD_PROPERTY);
        if (pw == null)
            pw = pUnitInfo.getDriverPassword();        
        if (pw != null) 
            props.put(JDBC_PASSWORD, pw);

        debug("EclipseLinkProvider - getJDBCProperties - returning: ", props);
        return props;
    }

    // EclipseLink-specific logging functions - why are these here?
    public void openEclipseLinkLoggingFile() {
        String loggingFile = System.getProperty(PersistenceUnitProperties.LOGGING_FILE);
        try {
            if (loggingFile != null) {
                eclipseLinkLog = new FileWriter(loggingFile);
                AbstractSessionLog.getLog().setWriter(eclipseLinkLog);
            }
        } catch (IOException e) {
            AbstractSessionLog.getLog().log(SessionLog.WARNING, "cmp_init_default_logging_file_is_invalid",loggingFile,e);
        }
    }
    public void closeEclipseLinkLoggingFile() {
        // Reset to default
        AbstractSessionLog.setLog(new DefaultSessionLog());
        try {
            if (eclipseLinkLog != null) {
                eclipseLinkLog.close();
            }
        } catch (IOException e) {
        }
    }
}