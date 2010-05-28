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
 ******************************************************************************/
package org.eclipse.gemini.jpa;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManagerFactory;

import org.osgi.framework.Bundle;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

import org.osgi.service.jdbc.DataSourceFactory;
import org.osgi.service.jpa.EntityManagerFactoryBuilder;

import org.eclipse.persistence.internal.jpa.deployment.osgi.BundleProxyClassLoader;

import org.eclipse.gemini.jpa.provider.OSGiJpaProvider;
import org.eclipse.gemini.jpa.proxy.EMFBuilderServiceProxyHandler;
import org.eclipse.gemini.jpa.proxy.EMFServiceProxyHandler;

import static org.eclipse.gemini.jpa.GeminiUtil.*;

/**
 * This class provides functionality to handle service registration of 
 * persistence units and providers, etc. One instance per provider.
 */
public class PersistenceServicesUtil {

    // The provider using this instance
    OSGiJpaProvider osgiJpaProvider;
    
    // Keep this for logging convenience
    String providerClassName;
   
    // PersistenceProvider service
    ServiceRegistration providerService;
    
    
    public PersistenceServicesUtil(OSGiJpaProvider provider) {
        this.osgiJpaProvider = provider;
        this.providerClassName = provider.getProviderClassName();
    }
    
    /*==================*/
    /* Services methods */
    /*==================*/
    
    /**
     * Register the provider as a persistence provider service.
     * The service registration will be stored locally.
     */
    public void registerProviderService() {
        
        debug("GeminiServicesUtil registering provider service for ", providerClassName);

        // Service strings
        String[] serviceNames = { javax.persistence.spi.PersistenceProvider.class.getName() };
        // Get a provider JPA SPI instance 
        javax.persistence.spi.PersistenceProvider persistenceProvider = osgiJpaProvider.getProviderInstance();

        // Store the version of the provider as a service property
        String version = bundleVersion(osgiJpaProvider.getBundle());
        Dictionary<String,String> props = new Hashtable<String,String>();
        props.put("osgi.jpa.provider.version", version);
        props.put("javax.persistence.provider", providerClassName);
        
        // Register the provider service
        providerService = osgiJpaProvider.getBundleContext().registerService(
                serviceNames, persistenceProvider, props);
        debug("GeminiServicesUtil successfully registered provider service for ", providerClassName);
    }    

    /**
     * Unregister the provider service. 
     */
    public void unregisterProviderService() {

        debug("GeminiServicesUtil unregistering provider service for ", providerClassName);
        providerService.unregister();
        providerService = null;
        debug("GeminiServicesUtil successfully unregistered provider service for ", providerClassName);
    }


    /**
     * Register the EMF and EMFBuilder services.
     */
    public void registerEMFServices(PUnitInfo pUnitInfo) {

        if (pUnitInfo.getClasses().isEmpty()) {
            registerEMFServicesWithNoClasses(pUnitInfo);
            return;
        }
        
        debug("GeminiServicesUtil registerEMFServices for ", pUnitInfo.getUnitName());
        Bundle pUnitBundle = pUnitInfo.getBundle();
        
        Map<String, Class<?>> anchorClasses = new HashMap<String, Class<?>>();
        
        ClassLoader pUnitLoader = null;
        // TODO PDE does not seem to be able to install bundles
/*        
        for (String packageName : pUnitInfo.getUniquePackageNames()) {
            String className = formattedPackageString(packageName, '/', '.') + 
                                    osgiJpaProvider.getAnchorClassName();
            try { 
                debug("GeminiServicesUtil loading anchor class ", className);
                Class<?> cls = pUnitBundle.loadClass(className);
                debug("GeminiServicesUtil loaded anchor class ", cls);
                if (pUnitLoader == null) {
                    pUnitLoader = cls.getClassLoader();
                    debug("GeminiServicesUtil pUnit loader ", pUnitLoader);
                }
                anchorClasses.put(className, cls);
            } catch (ClassNotFoundException cnfEx) {
                fatalError("Could not load anchor class ", cnfEx);
            }
        }
*/
        // Create the array of classes
        Collection<Class<?>> classCollection = new ArrayList<Class<?>>();
        classCollection.addAll(anchorClasses.values());
        Class<?> emfClass = null;
        Class<?> emfBuilderClass = null;
        debug("GeminiServicesUtil loading EMF and EMFBuilder classes");
        try {
            emfClass = pUnitInfo.getBundle().loadClass(
                            "javax.persistence.EntityManagerFactory");
            // TODO Make this the pUnit loader when fragment in place
            emfBuilderClass = osgiJpaProvider.getBundle().loadClass(
                            "org.osgi.service.jpa.EntityManagerFactoryBuilder");
        } catch (ClassNotFoundException cnfEx) {
            warning("Could not load EntityManagerFactory / EntityManagerFactoryBuilder classes from bundle", cnfEx);
        }
        if (emfClass != null) 
            classCollection.add(emfClass);
        Class<?>[] classArray = classCollection.toArray(new Class[0]);
        debug("GeminiServicesUtil EMF proxy class array: ", classArray);

        // TODO no classes - how to get the loader?
        // If we still don't have a there are any classes then use one to get the loader
        if ((pUnitLoader == null) && (!pUnitInfo.getClasses().isEmpty())) {
            try { 
                pUnitLoader = pUnitInfo.getBundle().loadClass((String)(pUnitInfo.getClasses().toArray()[0])).getClassLoader();
                debug("GeminiServicesUtil pUnit loader ", pUnitLoader);
            } catch (ClassNotFoundException cnfEx) {
                fatalError("Could not load domain class in p-unit", cnfEx);
            }
        }
        // If still null then use a proxy loader
        if (pUnitLoader == null) {
            pUnitLoader = new BundleProxyClassLoader(pUnitInfo.getBundle());
        }

        // Proxy impl object for EMF service
        EMFServiceProxyHandler emfProxyInstance = new EMFServiceProxyHandler(pUnitInfo);
        Object emfServiceProxy = null ;

        try {
            emfServiceProxy = 
                Proxy.newProxyInstance(pUnitLoader, classArray, emfProxyInstance);
            debug("GeminiServicesUtil created EMF proxy ");
        } catch (Exception e) { 
            fatalError("GeminiServicesUtil - Failed to create proxy for EMF service: ", e); 
        }

        if (emfClass != null) 
            classCollection.remove(emfClass);
        if (emfBuilderClass != null) 
            classCollection.add(emfBuilderClass);
        classArray = classCollection.toArray(new Class[0]);
        debug("GeminiServicesUtil EMFBuilder proxy class array: ", classArray);
        
        // Proxy impl object for EMFBuilder service
        EMFBuilderServiceProxyHandler emfBuilderProxyInstance = 
            new EMFBuilderServiceProxyHandler(pUnitInfo, emfProxyInstance);
        // TODO this is the provider loader, but is not the right one - need to use pUnit bundle loader
        // But need to insert import into manifest of fragment
        Object emfBuilderServiceProxy = null;
        try {
            emfBuilderServiceProxy = 
                Proxy.newProxyInstance(this.getClass().getClassLoader(), classArray, emfBuilderProxyInstance);
            debug("GeminiServicesUtil created EMFBuilder proxy ");
        } catch (Exception e) { 
            fatalError("GeminiServicesUtil - Failed to create proxy for EMFBuilder service: ", e); 
        }

        // Assemble the properties
        Dictionary<String,String> props = new Hashtable<String,String>();
        props.put("osgi.unit.name", pUnitInfo.getUnitName());
        props.put("osgi.unit.version", bundleVersion(pUnitBundle));
        props.put("osgi.unit.provider", providerClassName);
        // Only support one bundle right now
        String bundleId = pUnitBundle.getSymbolicName() + "_" + bundleVersion(pUnitBundle);
        props.put("osgi.managed.bundles", bundleId);
        debug("GeminiServicesUtil EMF[Builder] services props: ", props);

        // Do we create an EMF service?
        if (pUnitInfo.getDriverClassName() != null) {
            if (!trackDataSourceFactory(pUnitInfo)) {
                warning("DataSourceFactory for " + pUnitInfo.getDriverClassName(), " not registered.");
            } else {
                // The data source factory service exists, so create the EMF service
                // Array of class names to register under = (anchor classes + EMF class)
                Set<String> emfServiceClassNameSet = new HashSet<String>();
                emfServiceClassNameSet.addAll(anchorClasses.keySet());
                emfServiceClassNameSet.add(EntityManagerFactory.class.getName());
                String[] emfClassNameArray = emfServiceClassNameSet.toArray(new String[0]);
    
                // Register the EMF service (using p-unit context) and set regn in PUnitInfo
                ServiceRegistration emfService = null;
                try {
                    emfService = pUnitBundle.getBundleContext()
                                   .registerService(emfClassNameArray, emfServiceProxy, props);
                } catch (Exception e) {
                    fatalError("GeminiServicesUtil could not register EMF service for " + pUnitInfo.getUnitName(), e);
                }
                pUnitInfo.setEmfService(emfService);
                debug("GeminiServicesUtil registered EMF service for ", pUnitInfo.getUnitName());
            }
        }
        
        // Array of class names for the builder service
        Set<String> emfbServiceClassNameSet = new HashSet<String>();
        emfbServiceClassNameSet.addAll(anchorClasses.keySet());
        emfbServiceClassNameSet.add(EntityManagerFactoryBuilder.class.getName());
        String[] emfbClassNameArray = emfbServiceClassNameSet.toArray(new String[0]);
        
        //Register the EMFBuilder service and set it in the PUnitInfo
        ServiceRegistration emfBuilderService = null;
        try {
            // TODO Should be registered by p-unit context, not provider context
            emfBuilderService = osgiJpaProvider.getBundleContext()
                    .registerService(emfbClassNameArray, emfBuilderServiceProxy, props);
        } catch (Exception e) {
            fatalError("GeminiServicesUtil could not register EMFBuilder service for " + pUnitInfo.getUnitName(), e);
        }
        pUnitInfo.setEmfBuilderService(emfBuilderService);
        debug("GeminiServicesUtil registered EMFBuilder service for ", pUnitInfo.getUnitName());
    }

    /**
     * Register the EMF and EMFBuilder services without using the extra proxy classes.
     */
    public void registerEMFServicesWithNoClasses(PUnitInfo pUnitInfo) {

        debug("GeminiServicesUtil registerEMFServicesWithNoClasses for ", pUnitInfo.getUnitName());
        Bundle pUnitBundle = pUnitInfo.getBundle();
        
        // Create the array of classes
        Collection<Class<?>> classCollection = new ArrayList<Class<?>>();
        Class<?> emfClass = null;
        Class<?> emfBuilderClass = null;
        try {
            emfClass = osgiJpaProvider.getBundle().loadClass(
                    "javax.persistence.EntityManagerFactory");
            emfBuilderClass = osgiJpaProvider.getBundle().loadClass(
                    "org.osgi.service.jpa.EntityManagerFactoryBuilder");
        } catch (ClassNotFoundException cnfEx) {
            fatalError("Could not load EntityManagerFactory / EntityManagerFactoryBuilder classes from provider", cnfEx);
        }
        classCollection.add(emfClass);
        Class<?>[] classArray = classCollection.toArray(new Class[0]);

        // Proxy impl object for EMF service
        EMFServiceProxyHandler emfProxyInstance = new EMFServiceProxyHandler(pUnitInfo);
        Object emfServiceProxy = null ;
        
        try {
            emfServiceProxy = 
                Proxy.newProxyInstance(emfClass.getClassLoader(), classArray, emfProxyInstance);
            debug("GeminiServicesUtil created EMF proxy ");
        } catch (Throwable e) { 
            fatalError("GeminiServicesUtil - Failed to create proxy for EMF service: ", e); 
        }

        classCollection.remove(emfClass);
        classCollection.add(emfBuilderClass);
        classArray = classCollection.toArray(new Class[0]);
        
        // Proxy impl object for EMFBuilder service
        EMFBuilderServiceProxyHandler emfBuilderProxyInstance = 
            new EMFBuilderServiceProxyHandler(pUnitInfo, emfProxyInstance);
        Object emfBuilderServiceProxy = null;
        try {
            emfBuilderServiceProxy = 
                Proxy.newProxyInstance(this.getClass().getClassLoader(), classArray, emfBuilderProxyInstance);
            debug("GeminiServicesUtil created EMFBuilder proxy ");
        } catch (Throwable e) { 
            fatalError("GeminiServicesUtil - Failed to create proxy for EMFBuilder service: ", e); 
        }

        // Assemble the properties
        Dictionary<String,String> props = new Hashtable<String,String>();
        props.put("osgi.unit.name", pUnitInfo.getUnitName());
        props.put("osgi.unit.version", bundleVersion(pUnitBundle));
        props.put("osgi.unit.provider", providerClassName);
        // Only support one bundle right now
        String bundleId = pUnitBundle.getSymbolicName() + "_" + bundleVersion(pUnitBundle);
        props.put("osgi.managed.bundles", bundleId);
        debug("GeminiServicesUtil EMF[Builder] services props: ", props);

        // Do we create an EMF service?
        if (pUnitInfo.getDriverClassName() != null) {
            if (!trackDataSourceFactory(pUnitInfo)) {
                warning("DataSourceFactory for " + pUnitInfo.getDriverClassName(), " not registered.");
            } else {
                // Register the EMF service (using p-unit context) and set regn in PUnitInfo
                ServiceRegistration emfService = null;
                try {
                    emfService = pUnitBundle.getBundleContext().registerService(
                            new String[] { EntityManagerFactory.class.getName() }, emfServiceProxy, props);
                } catch (Throwable e) {
                    fatalError("GeminiServicesUtil could not register EMF service for " + pUnitInfo.getUnitName(), e);
                }
                pUnitInfo.setEmfService(emfService);
                debug("GeminiServicesUtil registered EMF service for ", pUnitInfo.getUnitName());
            }
        }                
        //Register the EMFBuilder service and set it in the PUnitInfo
        ServiceRegistration emfBuilderService = null;
        try {
            emfBuilderService = pUnitBundle.getBundleContext().registerService(
                    new String[] { EntityManagerFactoryBuilder.class.getName() }, emfBuilderServiceProxy, props);
        } catch (Exception e) {
            fatalError("GeminiServicesUtil could not register EMFBuilder service for " + pUnitInfo.getUnitName(), e);
        }
        pUnitInfo.setEmfBuilderService(emfBuilderService);
        debug("GeminiServicesUtil registered EMFBuilder service for ", pUnitInfo.getUnitName());
    }

    /**
     * Unregister the EMF service if there was an EMF service registered.
     * 
     * @param pUnitInfo
     * 
     * @return EntityManagerFactory the factory backing the service, or null if one didn't exist
     */
    public EntityManagerFactory unregisterEMFService(PUnitInfo pUnitInfo) {

        debug("GeminiServicesUtil unregisterEMFService for ", pUnitInfo.getUnitName());

        // If the tracking service is going then stop it already
        // TODO Take this out when we handle the DSF coming back?
        stopTrackingDataSourceFactory(pUnitInfo);

        // If an EMF service is registered then unregister it
        ServiceRegistration emfService = pUnitInfo.getEmfService();
        if (emfService != null) {
            debug("GeminiServicesUtil unregistering EMF service for ", pUnitInfo.getUnitName());
            try { 
                emfService.unregister(); 
            } catch (Exception e) {
                warning("Error unregistering EMF service: ", e);
            }
            debug("GeminiServicesUtil unregistered EMF service for ", pUnitInfo.getUnitName());
            pUnitInfo.setEmfService(null);
        }

        EMFServiceProxyHandler emfHandler = pUnitInfo.getEmfHandler();
        EntityManagerFactory emf = null;
        if (emfHandler != null) {
            emf = emfHandler.getEMF();
            debug("GeminiServicesUtil EMF service had emf: ", emf);
            emfHandler.setEMF(null);
            pUnitInfo.setEmfHandler(null);
        }
        return emf;
    }

    /**
     * Unregister the EMFBuilder service.
     * 
     * @param pUnitInfo
     * 
     * @return EntityManagerFactory the factory backing the service, or null if one didn't exist yet
     */
    public EntityManagerFactory unregisterEMFBuilderService(PUnitInfo pUnitInfo) {

        debug("GeminiServicesUtil unregisterEMFBuilderService for ", pUnitInfo.getUnitName());

        // Unregister the service
        ServiceRegistration emfBuilderService = pUnitInfo.getEmfBuilderService();
        if (emfBuilderService != null) {
            debug("GeminiServicesUtil unregistering EMFBuilder service for ", pUnitInfo.getUnitName());
            try {
                emfBuilderService.unregister();
            } catch (Exception e) {
                warning("Error unregistering EMFBuilder service: ", e);
            }
            debug("GeminiServicesUtil unregistered EMFBuilder service for ", pUnitInfo.getUnitName());
            pUnitInfo.setEmfBuilderService(null);
        }

        // Save the EMF if it exists and clear out the handler
        EMFBuilderServiceProxyHandler emfBuilderHandler = pUnitInfo.getEmfBuilderHandler();
        EntityManagerFactory emf = null;
        if (emfBuilderHandler != null) {
            emf = emfBuilderHandler.getEMF();
            debug("GeminiServicesUtil EMFBuilder service had emf: ", emf);
            emfBuilderHandler.setEMF(null);
            pUnitInfo.setEmfBuilderHandler(null);
        }
        return emf;
    }    
    
    /*================*/
    /* Helper methods */
    /*================*/
    
    /** 
     * Look up the data source factory service for the specified
     * persistence unit and cause it to be tracked, so that if it gets stopped 
     * then we can be told and remove the dependent EMF service.
     * Return true if the data source factory was registered, false if it wasn't
     */
    public boolean trackDataSourceFactory(PUnitInfo pUnitInfo) {

        debug("GeminiServicesUtil trackDataSourceFactory for p-unit ", pUnitInfo.getUnitName());
        ServiceReference[] dsfRefs = null;

        // See if the data source factory service for the specified driver is registered
        String filter = "(" + DataSourceFactory.JDBC_DRIVER_CLASS + "=" + 
                        pUnitInfo.getDriverClassName() + ")";
        try {
            dsfRefs = pUnitInfo.getBundle().getBundleContext().getServiceReferences(
                DataSourceFactory.class.getName(), filter);
        } catch (InvalidSyntaxException isEx) {
            fatalError("Bad filter syntax (likely because of missing driver class name)", isEx);
        } 
        if (dsfRefs == null)
            return false;
        
        // We found at least one -- track the first ranked one (assuming it will be used)
        // TODO Race condition where service could disappear before being tracked
        debug("GeminiServicesUtil starting tracker for DataSourceFactory ", 
                pUnitInfo.getDriverClassName());
        ServiceTracker tracker = new ServiceTracker(osgiJpaProvider.getBundleContext(), 
                                                    dsfRefs[0],
                                                    new DataSourceTracker(pUnitInfo, this));
        pUnitInfo.setTracker(tracker);
        tracker.open();
        return true;
    }

    /** 
     * Stop tracking the data source factory for the given p-unit
     */
    public void stopTrackingDataSourceFactory(PUnitInfo pUnitInfo) {
        // Clean up the tracker
        debug("GeminiServicesUtil stopTrackingDataSourceFactory ", 
                pUnitInfo.getDriverClassName(), " for p-unit: ", pUnitInfo.getUnitName());
        if (pUnitInfo.getTracker() != null) {
            debug("GeminiServicesUtil stopping tracker for DataSourceFactory ", 
                    pUnitInfo.getDriverClassName());
            pUnitInfo.getTracker().close();
            pUnitInfo.setTracker(null);
        }
    }

    /** 
     * This method will be invoked by the data source factory tracker
     * when the data source factory service comes online.
     */
    public void dataSourceFactoryAdded(PUnitInfo pUnitInfo) {
        debug("GeminiServicesUtil dataSourceFactoryAdded, ", pUnitInfo.getDriverClassName());
        if ((pUnitInfo.getEmfBuilderHandler() != null) && 
                (pUnitInfo.getEmfBuilderHandler().getEMF() != null)) {
            // A factory already exists in the builder so we can't register an EMF service
        } else {
            // The builder has not been used. Just unregister it and go through the entire 
            // registration process again assuming the data source service is present
            debug("GeminiServicesUtil dataSourceFactoryAdded, unregistering builder and reregistering ", pUnitInfo.getDriverClassName());
            unregisterEMFBuilderService(pUnitInfo);
            registerEMFServices(pUnitInfo);
        }
    }

    /** 
     * This method will be invoked by the data source factory tracker
     * when the data source factory service that we rely on disappears.
     */
    public void dataSourceFactoryRemoved(PUnitInfo pUnitInfo) {
        // TODO async handling of data source removal

        debug("GeminiServicesUtil dataSourceFactoryRemoved, ", pUnitInfo.getDriverClassName());
        if ((pUnitInfo.getEmfBuilderHandler() != null) && 
                (pUnitInfo.getEmfBuilderHandler().getEMF() != null)) {
            debug("GeminiServicesUtil builder had an EMF, ", 
                    "unregistering both services for p-unit ", 
                    pUnitInfo.getDriverClassName());
            // Call into the provider to do the unregistration
            Collection<PUnitInfo> pUnits = new ArrayList<PUnitInfo>();
            pUnits.add(pUnitInfo);
            osgiJpaProvider.unregisterPersistenceUnits(pUnits);
        } else {
            // Only unregister the EMF service, and leave the Builder
            debug("GeminiServicesUtil only unregistering EMF service, ", 
                    "leaving Builder service");
            EntityManagerFactory emf = unregisterEMFService(pUnitInfo);           
            if (emf != null) 
                emf.close();
        }
    }
}
