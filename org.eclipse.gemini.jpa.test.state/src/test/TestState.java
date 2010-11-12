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
package test;

import java.util.HashSet;
import java.util.Set;

import java.util.HashMap;
import java.util.Map;

import org.junit.runner.Result;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Test state class used to keep a memory of the tests that get executed
 * in the face of the test cases continually being refreshed
 * 
 * @author mkeith
 */
public class TestState implements BundleActivator {

    static Set<String> incompletedTests = new HashSet<String>();
    static Map<String,Result> completedTests = new HashMap<String,Result>();
    
    static void initTests() {
        incompletedTests = new HashSet<String>();
        completedTests = new HashMap<String,Result>();

        // Tests to run - Comment out tests to disable them.
        
        incompletedTests.add("TestStaticPersistence");
        incompletedTests.add("TestEMFService");
        incompletedTests.add("TestEMFBuilderService");
        incompletedTests.add("TestEMFBuilderServiceProperties");
        incompletedTests.add("TestEmbeddedPUnit");
        incompletedTests.add("TestOrmMappingFile");
        incompletedTests.add("TestMappingFileElement");
        incompletedTests.add("TestEmptyPersistence");
        incompletedTests.add("TestEmptyPersistenceWithProps");
    }
    
    public static void resetTests() { 
        initTests(); 
    }

    public static void startTest(String s) { 
        incompletedTests.remove(s);
    }
    
    public static void completedTest(String s, Result r) { 
        completedTests.put(s, r);
    }

    public static Set<String> getIncompletedTests() { 
        return incompletedTests;
    }

    public static boolean isTested(String s) { 
        return !incompletedTests.contains(s); 
    }

    public static Map<String,Result> getAllTestResults() { 
        return completedTests; 
    }

    public void start(BundleContext context) throws Exception {
        initTests();
        System.out.println("TestState active");
        System.out.println("Tests in run list: ");
        System.out.println(""+incompletedTests);
    }

    public void stop(BundleContext context) throws Exception {}
}
