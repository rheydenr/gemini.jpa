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

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import javax.persistence.EntityManagerFactory;

import org.osgi.service.jpa.EntityManagerFactoryBuilder;

/**
 * Activator to start tests when relevant service becomes available
 * 
 * @author mkeith
 */
public class Activator implements BundleActivator, ServiceTrackerCustomizer {

    BundleContext ctx;
    
    ServiceTracker emfTracker;
    ServiceTracker emfbTracker;

    public void start(BundleContext context) throws Exception {
        log("Tests active");

        ctx = context;
        JpaTest.context = context;
                
        emfTracker = new ServiceTracker(ctx, EntityManagerFactory.class.getName(), this);
        emfbTracker = new ServiceTracker(ctx, EntityManagerFactoryBuilder.class.getName(), this);

        emfTracker.open();
        emfbTracker.open();
        
        // Run tests from tracker when service is online
    }

    public void stop(BundleContext context) throws Exception {
        log("Tests stopped");
        emfbTracker.close();
        emfTracker.close();
    }

    void runTest(String descr, Class<?> testClass) {
        log("Running " + descr + ": ");
        
        Result r = JUnitCore.runClasses(testClass);

        log(descr + " results: ");
        logResultStats(r);

        log("Done " + descr);
    }

    /* ServiceTracker methods */

    public Object addingService(ServiceReference ref) {
        Bundle b = ref.getBundle();
        Object service = b.getBundleContext().getService(ref);
        
        String unitName = (String)ref.getProperty(EntityManagerFactoryBuilder.JPA_UNIT_NAME);

        if (unitName != null) {
            // We have a JPA service. Is it an EMF or an EMFBuilder?
            boolean isEmfService = EntityManagerFactory.class.isInstance(service);
            
            // Now ask each test if it should run based on the punit name and whether 
            // the service is an EMF or an EMFBuilder.
            if (TestStaticPersistence.shouldRun(unitName, isEmfService))
                runTest("JPA Static Persistence tests", TestStaticPersistence.class);
            if (TestEMFService.shouldRun(unitName, isEmfService))
                runTest("JPA EMF Service tests", TestEMFService.class);
            if (TestEMFBuilderService.shouldRun(unitName, isEmfService))
                runTest("JPA EMF Builder Service tests", TestEMFBuilderService.class);
            if (TestEMFBuilderServiceProperties.shouldRun(unitName, isEmfService))
                runTest("JPA EMF Builder Service Properties tests", TestEMFBuilderServiceProperties.class);
            if (TestEmbeddedPUnit.shouldRun(unitName, isEmfService))
                runTest("JPA Embedded PUnit tests", TestEmbeddedPUnit.class);
        }
        return service;
    }

    public void modifiedService(ServiceReference ref, Object service) {}

    public void removedService(ServiceReference ref, Object service) {}

    void logResultStats(Result r) {
        log("Result: " + 
                " runs=" + r.getRunCount() + 
                " failures=" + r.getFailureCount() +
                " ignore=" + r.getIgnoreCount());        
        log("Failures: " + r.getFailures());
        for (Failure f : r.getFailures())
            log("--- Failure: \n" + f.getTrace());
    }
    
    static void log(String msg) {
        System.out.println("===== " + msg);
    }    
}
