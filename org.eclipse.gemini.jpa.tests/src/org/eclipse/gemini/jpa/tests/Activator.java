/*
 * Copyright (C) 2010 Oracle Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.eclipse.gemini.jpa.tests;

import model.account.Account;

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

    ServiceTracker emfTracker;
    ServiceTracker emfbTracker;

    BundleContext ctx;
    
    public void start(BundleContext context) throws Exception {
        log("Tests active ");

        ctx = context;
        JpaTest.context = context;
                
        emfTracker = new ServiceTracker(ctx, EntityManagerFactory.class.getName(), this);
        emfbTracker = new ServiceTracker(ctx, EntityManagerFactoryBuilder.class.getName(), this);

        emfTracker.open();
        emfbTracker.open();
        
        // Run tests from tracker when service is online
    }

    public void stop(BundleContext context) throws Exception {
        emfbTracker.close();
        emfTracker.close();
    }

    void runTest(String descr, Class testClass) {
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
            // We have a JPA service - which one is it?
            boolean isEmfService = EntityManagerFactory.class.isInstance(service);

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

    public void removedService(ServiceReference ref, Object service) {
        ctx.ungetService(ref);
    }

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
