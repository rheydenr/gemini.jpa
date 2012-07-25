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

import static org.eclipse.gemini.jpa.GeminiUtil.debug;
import static org.eclipse.gemini.jpa.GeminiUtil.warning;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import org.eclipse.gemini.jpa.classloader.CompositeClassLoader;
import org.eclipse.gemini.jpa.datasource.DataSourceUtil;
import org.eclipse.gemini.jpa.provider.EclipseLinkOSGiProvider;
import org.eclipse.gemini.jpa.provider.GeminiOSGiInitializer;

public class GeminiManager {
    
    /*==================*/
    /* Static constants */
    /*==================*/

    public static final int MAX_EVENT_COLLISION_TRIES = 3;
    
    /*=====================*/
    /* Global Gemini state */
    /*=====================*/
    
    /** Our bundle context */
    BundleContext ctx;

    /** Map of p-units we have registered */
    Map<String, PUnitInfo> pUnitsByName;

    /** Extender to find and process persistence unit bundles */
    PersistenceBundleExtender extender;

    /** Services utility methods */
    ServicesUtil servicesUtil;
    
    /** DataSource and JDBC utility methods */
    DataSourceUtil dataSourceUtil;
    
    /** Anchor class gen utility methods */
    AnchorClassUtil anchorUtil;    
    
    /** Our wrapper class over the EclipseLink provider */
    EclipseLinkOSGiProvider provider;
    
    /*================*/
    /* Getter/setters */
    /*================*/

    public BundleContext getBundleContext() { return ctx; }
    public void setBundleContext(BundleContext ctx) { this.ctx = ctx; }

    public PersistenceBundleExtender getExtender() { return extender; }
    public void setExtender(PersistenceBundleExtender extender) { this.extender = extender; }

    public ServicesUtil getServicesUtil() { return servicesUtil; }
    public void setServicesUtil(ServicesUtil servicesUtil) { this.servicesUtil = servicesUtil; }

    public DataSourceUtil getDataSourceUtil() { return dataSourceUtil; }
    public void setDataSourceUtil(DataSourceUtil dataSourceUtil) { this.dataSourceUtil = dataSourceUtil; }

    public AnchorClassUtil getAnchorUtil() { return anchorUtil; }
    public void setAnchorUtil(AnchorClassUtil anchorUtil) { this.anchorUtil = anchorUtil; }

    public EclipseLinkOSGiProvider getProvider() { return provider; }
    public void setProvider(EclipseLinkOSGiProvider provider) { this.provider = provider; }
    
    public Map<String, PUnitInfo> getPUnitsByName() { return pUnitsByName; }
    public void setPUnitsByName(Map<String, PUnitInfo> pUnitsByName) { this.pUnitsByName = pUnitsByName; }

    /*=================================*/
    /* Initialization/shutdown methods */
    /*=================================*/

    /** Initialize Gemini JPA and get it running */
    public void startup(BundleContext context) throws Exception {        
        // Initialize our state
        ctx = context;
        pUnitsByName = Collections.synchronizedMap(new HashMap<String, PUnitInfo>());
        
        provider = new EclipseLinkOSGiProvider();
        provider.initialize(this);

        extender = new PersistenceBundleExtender(this);
        dataSourceUtil = new DataSourceUtil(this);
        anchorUtil = new AnchorClassUtil(GeminiSystemProperties.generateAnchorClasses());
        servicesUtil = new ServicesUtil(this);
        
        // Register as a provider 
        servicesUtil.registerProviderService();

        // Kick the extender to go looking for persistence bundles
        extender.startListening();
        extender.lookForExistingBundles();        
        debug("Gemini JPA started");
    }
    
    /** Shut down Gemini JPA */
    public void shutdown(BundleContext context) throws Exception {

        // Take the extender offline and unregister the provider
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
        debug("Gemini JPA stopped");
    }
    
    /*===========================*/
    /* Extender callback methods */
    /*===========================*/
    
    /**
     * Assignment happens before resolution. This callback offers us a chance to do 
     * anything that must be done before the bundle is resolved.
     */
    public void assignPersistenceUnitsInBundle(Bundle b, Collection<PUnitInfo> pUnits) {
        //TODO Check state of bundle in assign call
        debug("Manager.assignPersistenceUnitsInBundle: ", b);
        
        if (GeminiSystemProperties.generateFragments()) {
            // Generate a fragment for the p-units
            new FragmentUtil(getBundle())
                    .generateAndInstallFragment(b, pUnits, getAnchorUtil());
        }

        // Create a loader that can load from the persistence bundle as well as from the provider bundle
        ClassLoader compositeLoader = CompositeClassLoader.createCompositeLoader(getBundleContext(), b);
        
        // Create and run initializer to process PU and register transformers
        GeminiOSGiInitializer initializer = new GeminiOSGiInitializer();        
        initializer.initializeFromBundle(getBundleContext(), b, compositeLoader, pUnits);
    }

    /**
     * The persistence bundle is resolved. We must register the 
     * persistence unit services in the registry.
     * 
     * @param pUnits Usually, but not always, all in the same bundle
     */
    public void registerPersistenceUnits(Collection<PUnitInfo> pUnits) {
        
        debug("Manager.registerPersistenceUnits: ", pUnits);

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
                    warning("Manager forcing unregister of persistence unit: " + info.getUnitName());
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

        debug("Manager.unregisterPersistenceUnits: ", pUnits);

        if (pUnits == null) return;
        
        for (PUnitInfo info : pUnits) {
            servicesUtil.unregisterEMFServices(info);

            // Remove from our local pUnit copy 
            pUnitsByName.remove(info.getUnitName());
        }
    }

    public void unassignPersistenceUnitsInBundle(Bundle b, Collection<PUnitInfo> pUnits) {
        debug("Manager.unassignPersistenceUnitsInBundle: ", b.getSymbolicName());
    }

    /*================*/
    /* Helper methods */
    /*================*/

    public Bundle getBundle() { return ctx.getBundle(); }  
    
}