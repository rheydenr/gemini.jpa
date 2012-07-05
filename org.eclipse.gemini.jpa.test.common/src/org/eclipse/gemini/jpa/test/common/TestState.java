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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import java.util.HashMap;
import java.util.Map;

import org.junit.runner.Result;

/**
 * Test state class used to keep a memory of the tests that get executed
 * in the face of the test cases continually being refreshed.
 * 
 * NOTE: This bundle must not require any other test packages or bundles
 *       so it doesn't get refreshed when test punits get refreshed.
 * 
 * @author mkeith
 */
public class TestState {

    public static String GEMINI_TEST_CLASSES = "GEMINI_TESTS";

    public static boolean dsfOnline = false;
    public static Set<Class<?>> dsfQueuedTests = new HashSet<Class<?>>();

    static Set<String> testGroups = new HashSet<String>();
    static Set<String> incompletedTests = new HashSet<String>();
    static Map<String,Result> completedTests = new HashMap<String,Result>();
    static boolean init = initTests();
    
    static boolean initTests() {
        dsfQueuedTests = new HashSet<Class<?>>();
        testGroups = new HashSet<String>();
        incompletedTests = new HashSet<String>();
        completedTests = new HashMap<String,Result>();

        // If test property provided then just run comma-separated list 
        // of unqualified JpaTest subclasses in org.eclipse.gemini.jpa.tests
        String tests = System.getProperty(GEMINI_TEST_CLASSES, null);
        if (tests != null)
            incompletedTests.addAll(Arrays.asList(tests.split(",")));
        // Otherwise just let the tests add their own 
        return true;
    }
    
    public static boolean isDsfOnline() { return dsfOnline; }
    public static void setDsfOnline(boolean value) { dsfOnline = value; }
    
    public static boolean isGroupRegistered(String group) { 
        return testGroups.contains(group); 
    }

    public static void registerGroup(String group) { 
        testGroups.add(group); 
    }
    
    public static boolean isTested(String testName) { 
        return !incompletedTests.contains(testName); 
    }

    public static void addTest(String testName) { 
        incompletedTests.add(testName); 
    }

    public static void testStarted(String s) { 
        incompletedTests.remove(s);
    }
    
    public static void testCompleted(String s, Result r) { 
        completedTests.put(s, r);
    }

    public static Set<String> getIncompletedTests() { 
        return incompletedTests;
    }

    public static Map<String,Result> getAllTestResults() { 
        return completedTests; 
    }

    public static void resetTests() { 
        initTests(); 
    }
}