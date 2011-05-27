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

import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import test.TestState;

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

    boolean shouldRun(Class<? extends JpaTest> testClass, String unitName, boolean isEMFService) {
        JpaTest test = null;
        try { test = testClass.newInstance(); 
        } catch (Exception ex) { throw new RuntimeException(ex); }
        return !TestState.isTested(testClass.getSimpleName()) 
               && test.getTestPersistenceUnitName().equals(unitName)
               && (!(test.needsEmfService() ^ isEMFService));
    }
    
    void runTest(Class<? extends JpaTest> testClass) {
        String testName = testClass.getSimpleName();
        TestState.startTest(testName);
        log("Running " + testName + ": ");
        Result r = JUnitCore.runClasses(testClass);

        log(testName + " results: ");
        logResultStats(r);
        TestState.completedTest(testName,r);
            
        log("Done " + testName);

        Set<String> incompleteTests = TestState.getIncompletedTests();
        if (!incompleteTests.isEmpty()) {
            System.out.println("------------------- Tests not run yet: " + incompleteTests);
        } else {
            // If no more tests to run, print out a summary
            System.out.println("-----------------------------------------------------"); 
            System.out.println("------------------- Test Summary --------------------"); 
            System.out.println("-----------------------------------------------------"); 
            Set<Map.Entry<String,Result>> results = TestState.getAllTestResults().entrySet();
            for (Map.Entry<String,Result> entry : results) {
                System.out.println("Test: " + entry.getKey()); 
                logResultStats(entry.getValue()); 
            }
        }
    }

    /* ServiceTracker methods */

    public Object addingService(ServiceReference ref) {
        Bundle b = ref.getBundle();
        Object service = b.getBundleContext().getService(ref);
        
        String unitName = (String)ref.getProperty(EntityManagerFactoryBuilder.JPA_UNIT_NAME);

        if (unitName != null) {
            // We have a JPA service. Is it an EMF or an EMFBuilder?
            boolean isEmfService = EntityManagerFactory.class.isInstance(service);
            
            log("Service added **** name=" + unitName + " EMF=" + isEmfService);
            
            // Now ask each test if it should run based on the punit name and whether 
            // the service is an EMF or an EMFBuilder. Note that more than one test 
            // may run on the same EMF/B service.
            
            if (shouldRun(TestStaticPersistence.class, unitName, isEmfService))
                runTest(TestStaticPersistence.class);
            if (shouldRun(TestEMFService.class, unitName, isEmfService))
                runTest(TestEMFService.class);
            if (shouldRun(TestEMFBuilderService.class, unitName, isEmfService))
                runTest(TestEMFBuilderService.class);
            if (shouldRun(TestEMFBuilderServiceProperties.class, unitName, isEmfService))
                runTest(TestEMFBuilderServiceProperties.class);
            if (shouldRun(TestEMFBuilderExternalDataSource.class, unitName, isEmfService))
                runTest(TestEMFBuilderExternalDataSource.class);
            if (shouldRun(TestEmbeddedPUnit.class, unitName, isEmfService))
                runTest(TestEmbeddedPUnit.class);
            if (shouldRun(TestOrmMappingFile.class, unitName, isEmfService))
                runTest(TestOrmMappingFile.class);
            if (shouldRun(TestMappingFileElement.class, unitName, isEmfService))
                runTest(TestMappingFileElement.class);
            if (shouldRun(TestEmptyPersistence.class, unitName, isEmfService))
                runTest(TestEmptyPersistence.class);
            if (shouldRun(TestEmptyPersistenceWithProps.class, unitName, isEmfService))
                runTest(TestEmptyPersistenceWithProps.class);
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
