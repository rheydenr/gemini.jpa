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
 *     ssmith - EclipseLink integration
 *     mkeith - rework to use weaving hooks
 ******************************************************************************/
package org.eclipse.gemini.jpa.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceUnitInfo;

import org.eclipse.gemini.jpa.GeminiUtil;
import org.eclipse.gemini.jpa.PUnitInfo;
import org.eclipse.gemini.jpa.weaving.WeavingHookTransformer;
import org.eclipse.persistence.internal.jpa.deployment.JPAInitializer;
import org.eclipse.persistence.internal.jpa.deployment.PersistenceUnitProcessor;
import org.eclipse.persistence.jpa.Archive;
import org.eclipse.persistence.logging.AbstractSessionLog;
import org.eclipse.persistence.logging.SessionLog;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import org.osgi.framework.hooks.weaving.WeavingHook;

@SuppressWarnings("rawtypes")
public class GeminiOSGiInitializer extends JPAInitializer {
    
    public static final String OSGI_BUNDLE = "org.eclipse.gemini.jpa.bundle";
    private static final String OSGI_CONTEXT = "org.eclipse.gemini.jpa.context";

    private boolean weavingSupported = true; 
   
    /**
     * Constructor used when registering bundles.
     */
    public GeminiOSGiInitializer() {
    }

    /** 
     * Constructor used by PersistenceProvider$PersistenceInitializationHelper
     * @param loader
     */
    GeminiOSGiInitializer(ClassLoader loader) {
        this.initializationClassloader = loader;
    }

    /**
     * Indicates whether puName uniquely defines the persistence unit.
     */
    @Override
    public boolean isPersistenceUnitUniquelyDefinedByName() {
        // TODO isPersistenceUnitUniquelyDefinedByName should be false
        // but PU creation fails for embedded PUs
        return true;
    }
    
    /**
     * A bundle is being stopped or becoming in some way unavailable.
     * Undeploy the bundle's persistence units and remove all references
     * to it.
     * @param bundle
     */
    public void unregisterBundle(Bundle bundle, Collection<PUnitInfo> pUnits) {
        //TODO: unregisterBundle(Bundle bundle, Collection<PUnitInfo> pUnits)
    }
    
    /**
     * Initialize the p-units in the bundle passed in (there may be multiple p-units).
     * 
     * @param context Provider bundle context
     * @param bundle The pUnit bundle
     * @param bundleLoader Used to load resources and classes
     * @param pUnits Collection of pUnits found in the bundle
     */
    public void initializeFromBundle(BundleContext context, 
                                     Bundle bundle, 
                                     ClassLoader bundleLoader, 
                                     Collection<PUnitInfo> pUnits) {
        this.initializationClassloader = bundleLoader;
        
        // Get all the unique archives in which the p-units are stored
        List<Archive> pars = new ArrayList<Archive>();
        Set<String> archivePaths = new HashSet<String>();
        for (PUnitInfo pUnitInfo : pUnits) {
            String pUnitDescPath = pUnitInfo.getDescriptorInfo().fullDescriptorPath();
            if (!archivePaths.contains(pUnitDescPath)){
                pars.addAll(PersistenceUnitProcessor.findPersistenceArchives(bundleLoader, pUnitDescPath));
                archivePaths.add(pUnitDescPath);
            }
        }
        // Create a properties map with the bundle and context so they
        // are available when defining a transformer.
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(OSGI_BUNDLE, bundle);
        properties.put(OSGI_CONTEXT, context);
        
        // Initialize all of the archives
        try {
            for (Archive archive: pars) {
                AbstractSessionLog.getLog().log(SessionLog.FINER, "cmp_init_initialize", archive);
                // This will call us back via #registerTransformer() method
                initPersistenceUnits(archive, properties);
            }
        } finally {
            for (Archive archive: pars) {
                archive.close();
            }
        }
    }

    /**
     * Check whether weaving is possible and update the properties and variable as appropriate
     * 
     * @param properties The list of properties to check for weaving and update if weaving is not needed
     */
    @Override
    public void checkWeaving(Map properties){}
    
    /**
     * This should not be used in OSGi
     */
    protected ClassLoader createTempLoader(Collection col, boolean shouldOverrideLoadClassForCollectionMembers) {
        return Thread.currentThread().getContextClassLoader();
    }

    /**
	 * In OSGi we don't need a temp loader so use the loader built for the bundle.
	 */
    @Override
	protected ClassLoader createTempLoader(Collection col) {
	    return this.initializationClassloader;
	}

    /**
     * Return the classloader of the bundle
     */
    public ClassLoader getBundleClassLoader(){
        return initializationClassloader;
    }
    
    /**
     * Override the parent impl to do nothing since in Gemini the initialization 
     * happens in the #initializeFromBundle() above, called from 
     * EclipseLinkOSGiProvider#assignPersistenceUnitsInBundle()
     */
    @Override    
    public void initialize(Map m) {}
    
    
    /**
     * Register a weaving hook for a given persistence unit.
     * Note that if multiple p-units exist within a bundle a
     * hook will be registered for each one of them. 
     * Shared classes could be a problem...
     * 
     * @param transformer Native EclipseLink transformer
     * @param persistenceUnitInfo Metadata describing the p-unit
     * @param properties Additional config and state to assist with weaving
     */
    @Override
    public void registerTransformer(ClassTransformer transformer, PersistenceUnitInfo persistenceUnitInfo, Map properties) {
        GeminiUtil.debugWeaving("GeminiInitializer.registerTransformer - ", persistenceUnitInfo.getPersistenceUnitName());
        if (weavingSupported) {
            // Get the persistence bundle containing the p-unit we are registering the transformer for
            Bundle bundle = (Bundle) properties.get(OSGI_BUNDLE);
            if (bundle == null){
                GeminiUtil.debugWeaving("No Bundle property, not registering Weaving Hook");
                return;
            }
            // Get the bundle context that we should use when registering the weaving hook
            BundleContext context = (BundleContext) properties.get(OSGI_CONTEXT);
            if (context == null){
                GeminiUtil.debugWeaving("No BundleContext property, not registering Weaving Hook");
                return;
            }
            if (transformer != null) {
                WeavingHook weaver = new WeavingHookTransformer(transformer, bundle.getSymbolicName(), bundle.getVersion());
                GeminiUtil.debugWeaving("Registering Weaving Hook for p-unit ", persistenceUnitInfo.getPersistenceUnitName());
                context.registerService(WeavingHook.class.getName(), weaver, new Hashtable<String,Object>(0));
            } else {
                GeminiUtil.debugWeaving("Null Transformer passed into registerTransformer");
            }
        } else {
            GeminiUtil.fatalError("Attempt to create a transformer when weaving not supported!", null);
        }
    }
}
