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
package org.eclipse.gemini.jpa.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

import org.eclipse.gemini.jpa.PUnitInfo;

import static org.eclipse.gemini.jpa.GeminiUtil.*;

/**
 * Dynamic proxy class to proxy the EMF service
 */
@SuppressWarnings({"rawtypes"})
public class EMFServiceProxyHandler implements InvocationHandler, ServiceFactory {
    
    PUnitInfo pUnitInfo;
    
    public EMFServiceProxyHandler(PUnitInfo pUnitInfo) { this.pUnitInfo = pUnitInfo; }
    
    /*=========================*/
    /* InvocationProxy methods */
    /*=========================*/
    
    // Will only get calls for the methods on the EntityManagerFactory interface
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        debug("EMFProxy invocation on method ", method.getName());

        /* NOTE: Allow close() to pass through in order for users to close the EMF. */
        /* This means that any user that closes an EMF will cause it to be closed   */
        /* for all other service references of the EMF service.                     */
        
        // Invoke these methods on the actual proxy (not the object it's proxying)
        if (method.getName().equals("hashCode"))
            return this.hashCode();
        if (method.getName().equals("toString"))
            return this.toString();
        
        
        /*===========================================================================*/
        /* ** NOTE: What if the provider supports multiple EMFs for the same punit?  */
        /*          Should we ignore the cache and just call the provider each time? */
        /*          Not right now.                                                   */
        /*===========================================================================*/

        // Get the EMF (setting a new one if one does not already exist)
        EntityManagerFactory emf = syncGetEMFAndSetIfAbsent(false, 
                                                            new HashMap<String,Object>());
        // Invoke the EMF method that was called
        Object result = method.invoke(emf, args);
        
        // If the operation was to close the EMF then chuck our reference away
        if (!emf.isOpen()) {
            syncUnsetEMF();
        }
        return result;
    }

    /*========================*/
    /* ServiceFactory methods */
    /*========================*/

    public Object getService(Bundle b, ServiceRegistration serviceReg) {
        // TODO Track client bundles that use this service and clean up if they leave
        return this;
    }
    
    public void ungetService(Bundle b, ServiceRegistration serviceReg, Object obj) {
        // EMF is shared, leave as is until the p-unit or the provider goes away
        // and the service is unregistered
    }
    
    /*================*/
    /* Helper methods */
    /*================*/

    // Return the EMF from the shared PUnitInfo, or null if none exists there
    public EntityManagerFactory syncGetEMFAndSetIfAbsent(boolean setByBuilderService,
                                                         Map<String,Object> props) {
        if (pUnitInfo.getEmf() == null) {
            synchronized(pUnitInfo) {
                // Check again while holding the mutex
                if (pUnitInfo.getEmf() == null) {
                    pUnitInfo.setEmf( this.createEMF(props) );
                    pUnitInfo.setEmfSetByBuilderService(setByBuilderService);
                }
            }
        }
        return pUnitInfo.getEmf();
    }

    // Set the EMF to null if one exists. Return the one that was there.
    public EntityManagerFactory syncUnsetEMF() {
        synchronized(pUnitInfo) {
            EntityManagerFactory emf = pUnitInfo.getEmf(); 
            pUnitInfo.setEmf(null);
            return emf;
        }
    }

    // Use info from the cached pUnitInfo and create a new EMF
    public EntityManagerFactory createEMF(Map<String,Object> props) {
        
        String unitName =  pUnitInfo.getUnitName();
        PersistenceProvider provider = pUnitInfo.getAssignedProvider().getProviderInstance();
        EntityManagerFactory result = provider.createEntityManagerFactory(unitName, props);
        if (result == null)
            fatalError("Proxy could not create EMF " + unitName + " from provider " + provider, null);
        return result;
    }
}        
