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
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.ProviderUtil;

import org.osgi.framework.BundleContext;

import org.eclipse.persistence.config.PersistenceUnitProperties;
import org.eclipse.persistence.internal.jpa.deployment.PersistenceUnitProcessor;
import org.eclipse.persistence.logging.AbstractSessionLog;
import org.eclipse.persistence.logging.DefaultSessionLog;
import org.eclipse.persistence.logging.SessionLog;

import org.eclipse.gemini.jpa.GeminiManager;
import org.eclipse.gemini.jpa.PUnitInfo;
import org.eclipse.gemini.jpa.classloader.CompositeClassLoader;

import static org.eclipse.gemini.jpa.GeminiUtil.debug;
import static org.eclipse.gemini.jpa.GeminiUtil.fatalError;
import static org.eclipse.gemini.jpa.GeminiUtil.warning;


@SuppressWarnings({"rawtypes","unchecked"})
public class EclipseLinkOSGiProvider implements PersistenceProvider {
    
    /*==================*/
    /* Static constants */
    /*==================*/
    public static final String ECLIPSELINK_PROVIDER_CLASS_NAME = "org.eclipse.persistence.jpa.PersistenceProvider";    
    
    /*============================*/
    /* EclipseLink provider state */
    /*============================*/
    
    /** A native EclipseLink SPI provider instance */
    PersistenceProvider nativeProvider;
    
    /** EclipseLink log file */
    private FileWriter eclipseLinkLog;

    /** Pointer back to the manager */
    GeminiManager mgr;

    /*=========================*/
    /* Initialization/shutdown */
    /*=========================*/

    public void initialize(GeminiManager mgr) throws Exception {
        
        this.mgr = mgr; 
        // If EL logging property was specified then open the log now?
        openEclipseLinkLogFileIfSpecified();
        // Create a native EclipseLink provider
        nativeProvider = new org.eclipse.gemini.jpa.provider.PersistenceProvider();
        // Set the archive factory to be the one we have defined
        PersistenceUnitProcessor.setArchiveFactory(new OSGiArchiveFactoryImpl());        
    }
    
    public void shutdown(BundleContext context) throws Exception {        
        closeEclipseLinkLogFileIfNecessary();
    }
    
    /*=========================*/
    /* Provider-specific methods */
    /*=========================*/
    
    // Used to compare against the <provider> element in persistence descriptors
    public String getProviderClassName() { return ECLIPSELINK_PROVIDER_CLASS_NAME; }

    // Used to invoke regular JPA createEntityManagerFactory() methods
    public javax.persistence.spi.PersistenceProvider getProviderInstance() { 
        return this;
    }
    
    /*=============================*/
    /* PersistenceProvider methods */
    /*=============================*/
    
    /**
     * Intercept calls to the OSGi EclipseLink JPA provider so we can insert 
     * classloader, data source and descriptor properties that can be used by
     * EclipseLink to sort things out.
     */
    @Override
    public EntityManagerFactory createEntityManagerFactory(String emName, Map properties) {
        
        debug("EclipseLinkProvider createEMF invoked for p-unit: ", emName);
        debug("Properties map: ", properties);

        PUnitInfo pUnitInfo = mgr.getPUnitsByName().get(emName);
        if (pUnitInfo == null)
            fatalError("createEntityManagerFactory() called on provider, but punit has not been registered: " + emName, null);

        Map<String,Object> props = new HashMap<String,Object>();
        props.putAll(properties);
        
        // Create a composite loader that loads from the punit bundle and the provider bundle
        CompositeClassLoader compositeLoader = CompositeClassLoader.createCompositeLoader(
                mgr.getBundleContext(), 
                pUnitInfo.getBundle());
        // Bug 385170 - If user supplies a classloader then tack it on the front
        if (props.containsKey(PersistenceUnitProperties.CLASSLOADER)) {
            ClassLoader userLoader = (ClassLoader) props.get(PersistenceUnitProperties.CLASSLOADER);
            compositeLoader.getClassLoaders().add(0, userLoader);
        }
        props.put(PersistenceUnitProperties.CLASSLOADER, compositeLoader);

        DataSource ds = mgr.getDataSourceUtil().acquireDataSource(pUnitInfo, properties);
        if (ds != null) 
            props.put(PersistenceUnitProperties.NON_JTA_DATASOURCE, ds);
        
        props.put(PersistenceUnitProperties.ECLIPSELINK_PERSISTENCE_XML, fullDescriptorPath(pUnitInfo));

        props.put(GeminiOSGiInitializer.OSGI_BUNDLE, pUnitInfo.getBundle());
        
        EntityManagerFactory emf = nativeProvider.createEntityManagerFactory(emName, props);
        return emf;
    }

    /**
     * NOTE: This method is not supported or tested.
     * An effort is made to try anyway, though, rather than just error out.
     * (Worst that can happen is it doesn't work).
     */
    @Override
    public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo info, Map properties) {

        String pUnitName = info.getPersistenceUnitName();
        warning("Container JPA not currently supported for p-unit ", pUnitName);
        
        // Can't hurt to go ahead and try, though...
        PUnitInfo pUnitInfo = mgr.getPUnitsByName().get(pUnitName);
        if (pUnitInfo == null)
            fatalError("createContainerEntityManagerFactory() called on provider, but provider has not registered the p-unit " + pUnitName, null);
        Map<String,Object> props = new HashMap<String,Object>();
        props.putAll(properties);
        props.put(PersistenceUnitProperties.CLASSLOADER, 
                  CompositeClassLoader.createCompositeLoader(mgr.getBundleContext(), 
                                                             pUnitInfo.getBundle()));
        DataSource ds = mgr.getDataSourceUtil().acquireDataSource(pUnitInfo, properties);
        if (ds != null) 
            props.put(PersistenceUnitProperties.NON_JTA_DATASOURCE, ds);
        props.put(PersistenceUnitProperties.ECLIPSELINK_PERSISTENCE_XML, fullDescriptorPath(pUnitInfo));
        props.put(GeminiOSGiInitializer.OSGI_BUNDLE, pUnitInfo.getBundle());

        return nativeProvider.createContainerEntityManagerFactory(info, props);
    }

    @Override
    public ProviderUtil getProviderUtil() { 
        debug("EclipseLinkProvider getProviderUtil invoked");
        return nativeProvider.getProviderUtil(); 
    }

    /*================*/
    /* Helper methods */
    /*================*/
    
    protected String fullDescriptorPath(PUnitInfo pUnitInfo) {
        return pUnitInfo.getDescriptorInfo().fullDescriptorPath();
    }

    // EclipseLink-specific logging functions (why are these here?)
    protected void openEclipseLinkLogFileIfSpecified() {
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
    protected void closeEclipseLinkLogFileIfNecessary() {
        // Reset to default
        AbstractSessionLog.setLog(new DefaultSessionLog());
        try {
            if (eclipseLinkLog != null) {
                eclipseLinkLog.close();
            }
        } catch (IOException e) {}
    }
}