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
package org.eclipse.gemini.jpa.test.common;

import java.util.HashMap;
import java.util.HashSet;
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

/**
 * Activator to start tests when relevant service becomes available
 * Test bundles subclass this activator to inherit test behavior
 * 
 * @author mkeith
 */
@SuppressWarnings({"unchecked","rawtypes"})
public class TestActivator implements BundleActivator, ServiceTrackerCustomizer {

    BundleContext ctx;
    
    ServiceTracker emfTracker;
    ServiceTracker emfbTracker;
    ServiceTracker dsfTracker;
    
    // Map of test class to test instance
    Map<Class<? extends JpaTest>,JpaTest> testClasses = new HashMap<Class<? extends JpaTest>,JpaTest>();    
	
    /*==========================================*/
    /* Subclasses *must* override these methods */
    /*==========================================*/

    public void setBundleContext(BundleContext ctx) {
        // This method must set the context in a static on each test case in the test bundle
    }
    public String getTestGroupName() { return "Test activator must implement getTestGroupName()"; }
    public String getTestPackage() { return "Test activator must implement getTestPackage()"; }

    public String[] getTestClasses() { 
        String[] s = { "Test activator must implement getTestClasses()" };
        return s;
    }
	
    /*===================*/
    /* Lifecycle methods */
    /*===================*/
	
    public void start(BundleContext context) throws Exception {
        // Method is called within the context of an inheriting test bundle
        sdebug(getTestGroupName(), "test group starting");
        ctx = context;
        this.setBundleContext(context);
                
        emfTracker = new ServiceTracker(ctx, EntityManagerFactory.class.getName(), this);
        emfbTracker = new ServiceTracker(ctx, EntityManagerFactoryBuilder.class.getName(), this);
        dsfTracker = new ServiceTracker(ctx, DataSourceFactory.class.getName(), this);
        
        // Register the tests to run if we have not already registered this group
        if (! TestState.isGroupRegistered(this.getTestGroupName())) {
            TestState.registerGroup(this.getTestGroupName());
            for (String testName : this.getTestClasses()) {
                TestState.addTest(testName);
            }
        }
        // Now [re]load the untested classes in this bundle since we have just been started (and may have been refreshed)
        for (String testName : TestState.getIncompletedTests()) {
            String fqTestName = this.getTestPackage() + "." + testName;
            try {
                Class<? extends JpaTest> testClass = (Class<? extends JpaTest>)
                            ctx.getBundle().loadClass(fqTestName);
                // Create an instance of the class
                JpaTest test = null;
                test = testClass.newInstance();
                // Store the instance against the class
                testClasses.put(testClass, test);
            } catch (ClassNotFoundException cnfEx) {
                slog("***************** Failed trying to load test class " + fqTestName + ": " + cnfEx);
            }   
        }
        // Tracker event will cause tests to run when service is online
        emfTracker.open();
        emfbTracker.open();
        dsfTracker.open();
    }

    public void stop(BundleContext context) throws Exception {
        sdebug(getTestGroupName(), "test group stopping");
        emfbTracker.close();
        emfTracker.close();
        dsfTracker.close();
    }

    /*======================*/
    /* Utility test methods */
    /*======================*/
	
    private JpaTest testInstance(Class<? extends JpaTest> cls) {
        return testClasses.get(cls);
    }
    
    boolean shouldRun(Class<? extends JpaTest> testClass, String unitName, boolean isEMFService) {
        JpaTest test = testClasses.get(testClass);
        if (test == null) 
            return false;        
        
        return !TestState.isTested(testClass.getSimpleName())
               && test.getTestPersistenceUnitName().equals(unitName)
               && (!(test.needsEmfService() ^ isEMFService));
    }
    
    void runTest(Class<? extends JpaTest> testClass) {
        String testName = testClass.getSimpleName();
        TestState.testStarted(testName);
        sdebug(testName, "running");
        // Invoke JUnit to run the test
        Result r = JUnitCore.runClasses(testClass);
        TestState.testCompleted(testName, r);
        sdebug(testName, "done");
		TestState.getAllTestResults().put(testName, r);
		// Remove from our local list of classes
		testClasses.remove(testClass);

		if (!(TestState.getIncompletedTests().isEmpty())) {
            sdebug(this.getTestGroupName(), "Tests not run yet: " + TestState.getIncompletedTests());
        } else {
            // If no more tests to run, print out a summary
            logGroupSummary();
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
            sdebug("Service added", "DataSourceFactory for " + driverClassName);
			TestState.setDsfOnline(true);
            // Now go through the pending dsf queue and run the tests
			sdebug("Running queued tests", "");
            for (Class cls : TestState.dsfQueuedTests) {
                runTest((Class<? extends JpaTest>)cls);
            }
            sdebug("Finished running queued tests", "");
            TestState.dsfQueuedTests.clear();
        } else {
            // Check if it is JPA service (EMF or EMFB)
            String unitName = (String)ref.getProperty(EntityManagerFactoryBuilder.JPA_UNIT_NAME);
            if (unitName != null) {
                // We have a JPA service. Is it an EMF or an EMFBuilder?
                boolean isEmfService = EntityManagerFactory.class.isInstance(service);                
                sdebug("Service added", "name=" + unitName + " EMF" + (isEmfService ? "" : "Builder"));

                // Now ask each test if it should run based on the punit name and whether 
                // the service is an EMF or an EMFBuilder. Note that more than one test 
                // may run on the same EMF/B service.
                Set<Class<? extends JpaTest>> classes = new HashSet<Class<? extends JpaTest>>();
                classes.addAll(testClasses.keySet());
                for (Class<? extends JpaTest> cls : classes) {
                    
                    // See if the test would run (ignoring whether DSF is online)
                    if (shouldRun(cls, unitName, isEmfService)) {
                        
                        // If the test needs the DSF service but the service is not online  
                        // then put the test in the pending DSF queue.
                        if (testInstance(cls).needsDsfService() && !TestState.isDsfOnline()) {
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

    /*=================*/
    /* Logging methods */
    /*=================*/

    void logGroupSummary() {
        slog("-----------------------------------------------------"); 
        slog("---------- " + getTestGroupName() + " tests complete." + " ----------"); 
        slog("-----------------------------------------------------"); 
        Set<Map.Entry<String,Result>> results = TestState.getAllTestResults().entrySet();
        for (Map.Entry<String,Result> entry : results) {
            slog("Test: " + entry.getKey()); 
            logResultStats(entry.getValue()); 
        }
    }
    void logResultStats(Result r) {
        slog("Result: " + 
                " runs=" + r.getRunCount() + 
                " failures=" + r.getFailureCount() +
                " ignore=" + r.getIgnoreCount());        
        slog("Failures: " + r.getFailures());
        for (Failure f : r.getFailures())
            slog("--- Failure: \n" + f.getTrace());
    }
    
    static void slog(String msg) {
        JpaTest.slog(msg);
    }    
    static void sdebug(String testName, String msg) {
        JpaTest.sdebug(testName, msg);
    }    
}
