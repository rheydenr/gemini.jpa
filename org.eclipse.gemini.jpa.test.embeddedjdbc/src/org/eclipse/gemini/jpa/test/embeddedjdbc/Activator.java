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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManagerFactory;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.jdbc.DataSourceFactory;
import org.osgi.service.jpa.EntityManagerFactoryBuilder;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import test.TestState;

/**
 * Activator to start tests when relevant service becomes available
 * 
 * @author mkeith
 */
@SuppressWarnings({"unchecked","rawtypes"})
public class Activator implements BundleActivator, ServiceTrackerCustomizer {

    BundleContext ctx;
    
    ServiceTracker emfTracker;
    ServiceTracker emfbTracker;
    ServiceTracker dsfTracker;
    
    // Map of test class to test instance
    Map<Class<? extends JpaTest>,JpaTest> testClasses = new HashMap<Class<? extends JpaTest>,JpaTest>();    

    public void start(BundleContext context) throws Exception {
        log("Tests active");

        ctx = context;
        JpaTest.context = context;
                
        emfTracker = new ServiceTracker(ctx, EntityManagerFactory.class.getName(), this);
        emfbTracker = new ServiceTracker(ctx, EntityManagerFactoryBuilder.class.getName(), this);
        dsfTracker = new ServiceTracker(ctx, DataSourceFactory.class.getName(), this);

        // Create the set of tests to run (get list from TestState)
        try {
            for (String clsString : TestState.getIncompletedTests()) {
                // Load the class
                Class<? extends JpaTest> testClass = (Class<? extends JpaTest>)
                            ctx.getBundle().loadClass("org.eclipse.gemini.jpa.tests." + clsString);
                // Create an instance of the class
                JpaTest test = null;
                test = testClass.newInstance(); 
                // Store the instance against the class 
                testClasses.put(testClass, test);
            }
            log("TestClasses: " + testClasses.keySet());
        } catch (ClassNotFoundException cnfEx) {
            log("***************** Failed trying to load test class: " + cnfEx);
        }
        // Run tests from tracker when service is online
        emfTracker.open();
        emfbTracker.open();
        dsfTracker.open();
    }

    public void stop(BundleContext context) throws Exception {
        log("Tests stopped");
        emfbTracker.close();
        emfTracker.close();
        dsfTracker.close();
        // Clear the queue since if we are being refreshed we may have new classes on restart
        TestState.dsfQueuedTests.clear();
    }
    
    private JpaTest testInstance(Class<? extends JpaTest> cls) {
        return testClasses.get(cls);
    }
    
    boolean shouldRun(Class<? extends JpaTest> testClass, String unitName, boolean isEMFService) {
        JpaTest test = testClasses.get(testClass);
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

    /*========================*/
    /* ServiceTracker methods */
    /*========================*/

    public Object addingService(ServiceReference ref) {
        Object service = ref.getBundle().getBundleContext().getService(ref);

        // Check to see if it is a DSF (and the Client driver)
        String driverClassName = (String) ref.getProperty(DataSourceFactory.OSGI_JDBC_DRIVER_CLASS);
        if ((driverClassName != null) && (driverClassName.equals(JpaTest.JDBC_TEST_DRIVER))) {
            // Record in the TestState (does not risk getting refreshed) that the DSF is online
            TestState.isDsfOnline = true;
            log("Service added **** DataSourceFactory for " + driverClassName);
            // Now go through the pending dsf queue and run the tests
            log("**** Running queued tests");
            for (Class cls : TestState.dsfQueuedTests) {
                runTest((Class<? extends JpaTest>)cls);
            }
            log("**** Finished running queued tests");
            TestState.dsfQueuedTests.clear();
        } else {
            // Check if it is JPA service (EMF or EMFB)
            String unitName = (String)ref.getProperty(EntityManagerFactoryBuilder.JPA_UNIT_NAME);
            if (unitName != null) {
                // We have a JPA service. Is it an EMF or an EMFBuilder?
                boolean isEmfService = EntityManagerFactory.class.isInstance(service);                
                log("Service added **** name=" + unitName + " EMF" + (isEmfService ? "" : "Builder"));

                // Now ask each test if it should run based on the punit name and whether 
                // the service is an EMF or an EMFBuilder. Note that more than one test 
                // may run on the same EMF/B service.
                for (Class<? extends JpaTest> cls : testClasses.keySet()) {
                    
                    // See if the test would run (ignoring whether DSF is online)
                    if (shouldRun(cls, unitName, isEmfService)) {
                        
                        // If the test needs the DSF service but the service is not online  
                        // then put the test in the pending DSF queue.
                        if (testInstance(cls).needsDsfService() && !TestState.isDsfOnline) {
                            TestState.dsfQueuedTests.add(cls);
                        } else {
                            // We are good to go
                            runTest(cls);
                        }
                    }
                }
            }
        }
        return service;
    }

    public void modifiedService(ServiceReference ref, Object service) {}

    public void removedService(ServiceReference ref, Object service) {}

    /*================*/
    /* Helper methods */
    /*================*/

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
