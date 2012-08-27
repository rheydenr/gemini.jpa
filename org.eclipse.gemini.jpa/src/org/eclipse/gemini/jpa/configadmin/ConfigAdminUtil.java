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
import org.eclipse.gemini.jpa.classloader.CompositeClassLoader;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;

/**
 * This singleton class provides functionality to integrate with the 
 * Config Admin service when available.
 */
@SuppressWarnings({"rawtypes"})
public class ConfigAdminUtil implements ManagedServiceFactory {

    /*==================*/
    /* Static constants */
    /*==================*/
    
    public static String PUNIT_FACTORY_PID = "gemini.jpa.punit";
    public static String SYNTHESIZED_DESC_PATH_PREFIX = "$$$_Synth_PUnit_Desc_";
    
    /*=============*/
    /* Local state */
    /*=============*/

    // Pointer back to the mgr
    GeminiManager mgr;
       
    // Service that gets notified about configs
    ServiceRegistration configListenerService;
    
    // Configs we have been notified of
    // Keyed by punit name
    Map<String,Dictionary> configs;

    // Mapping from generated service pid to punit name of config
    Map<String,String> pUnitNames;

    // Bundles found by the extender that didn't have a descriptor
    Set<String> inLimbo;
    
    
    // Synthesized descriptor count
    int descCount = 0;
    
    /*===================*/
    /* Lifecycle methods */
    /*===================*/
    
    public ConfigAdminUtil(GeminiManager mgr) { this.mgr = mgr; }
    
    /**
     * Register so that if a config admin service is running then we will 
     * be notified if/when a configuration is available
     */
    public void startListening() {
        
        debug("ConfigAdminUtil registering");
        configs = new HashMap<String,Dictionary>();
        // Service strings
        String[] serviceNames = { ManagedServiceFactory.class.getName() };

        // Store the version of the provider as a service property
        Dictionary<String,String> props = new Hashtable<String,String>();
        props.put(Constants.SERVICE_PID, PUNIT_FACTORY_PID);        
        
        // Register the provider service
        configListenerService = mgr.getBundleContext().registerService(serviceNames, this, props);
        debug("ConfigAdminUtil registered");
    }    

    /**
     * Unregister so we will stop receiving config admin notifications 
     */
    public void stopListening() {
        
        debug("ConfigAdminUtil unregistering");
        configListenerService.unregister();
        configs = null;
        debug("ConfigAdminUtil unregistered");
    }
    
    /*===============================*/
    /* ManagedServiceFactory methods */
    /*===============================*/

    @Override
    public void updated(String pid, Dictionary config) throws ConfigurationException {
        debug("ConfigAdminUtil.updated(), pid: ", pid);
        debug("Config contents: ", config);
        if (config == null) 
            return;
        // We have a configuration. Look for persistence unit name
        String unitName = (String) config.get(GeminiPersistenceUnitProperties.PUNIT_NAME);
        if (unitName == null) {
            GeminiUtil.warning("Configuration ignored because it does not contain persistence unit name property ",
                               GeminiPersistenceUnitProperties.PUNIT_NAME);
        }
        // Store against the generated pid so we can find it in case of a deleted() call
        String servicePid = (String) config.get(Constants.SERVICE_PID);
        synchronized (pUnitNames) { pUnitNames.put(servicePid, unitName); }
        // Store the config so it can be found by the extender when the persistence bundle is processed
        synchronized (configs) { configs.put(unitName, config); }

        // See if targeted at persistence bundle with an existing descriptor,
        // or if the bundle it is targeted at has no persistence descriptor
        // If no existing descriptor then create one and store it in classloader
        if (config.get(GeminiPersistenceUnitProperties.PUNIT_BSN) != null) {
            generateAndStashPersistenceDescriptor(config);
        }
        // Refresh the bundle in any case since we have some new config info
        refreshPersistenceUnitIfPresent(unitName);
    }
    
    @Override
    public String getName() { return "Gemini JPA Persistence Unit Config"; }
    
    @Override
    public void deleted(String pid) { 
        debug("ConfigAdminUtil.deleted()", " pid ", pid);
        if (pUnitNames.containsKey(pid)) {
            debug("ConfigAdminUtil.deleted()", "pid ", pid, " was found stored, being removed");
            String unitName = pUnitNames.get(pid);
            pUnitNames.remove(pid);
            Dictionary config = configs.get(unitName);
            if (config != null) {
                configs.remove(unitName);
                refreshPersistenceUnitIfPresent(unitName);
            }
        }
    }

    /*================*/
    /* Helper methods */
    /*================*/

    protected void generateAndStashPersistenceDescriptor(Dictionary config) {

        // Create a unique name
        String descName = SYNTHESIZED_DESC_PATH_PREFIX + descCount++;
        debug("Generating desc ", descName);
        String result = generateDescriptor(config);
        
        // Stash the descriptor in the classloader
        CompositeClassLoader.addSyththesizedDescriptor(descName, result);
    }

    protected String generateDescriptor(Dictionary dict) {

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<persistence version=\"2.0\" \n");
        sb.append("  xmlns=\"http://java.sun.com/xml/ns/persistence\" \n");
        sb.append("  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n");
        sb.append("  xsi:schemaLocation=\"http://java.sun.com/xml/ns/persistence  ");
        sb.append("http://java.sun.com/xml/ns/persistence/persistence_2_0.xsd\"> \n");
        sb.append("\t <persistence-unit name=\"" + dict.get(GeminiPersistenceUnitProperties.PUNIT_NAME));
        sb.append("\" transaction-type=\"RESOURCE_LOCAL\"> \n");
        sb.append("\t\t <provider>org.eclipse.persistence.jpa.PersistenceProvider</provider> \n");
        List<String> clsList = (List<String>)(dict.get(GeminiPersistenceUnitProperties.PUNIT_CLASSES));
        if (clsList != null) {
            for (String clsName : clsList)
                sb.append("\t\t <class>" + clsName + "</class> \n");
        }
        Object exclude = dict.get(GeminiPersistenceUnitProperties.PUNIT_EXCLUDE_UNLISTED_CLASSES);
        if (exclude != null) {
            sb.append("\t\t <exclude-unlisted-classes>");
            sb.append(exclude);
            sb.append("</exclude-unlisted-classes>\n");
        }
        Set<String> keys = leftOverProperties(dict);
        if (!keys.isEmpty()) {
            sb.append("\t\t <properties>");            
            for (String key :  keys) {
                sb.append("\t\t\t <property ");
                sb.append("name=\"" + key + "\" ");
                sb.append("value=\"" + (String)dict.get(key) + "\"");
                sb.append("/>");
            }
            sb.append("\t\t </properties>");
        }
        sb.append("\t </persistence-unit>");
        sb.append("</persistence>");
        String result = sb.toString(); 
        debug("Desc: \n", result);
        return result;
    }

    // Remove all of the reserved config properties and treat the rest 
    // as <property> elements in the descriptor.
    protected Set<String> leftOverProperties(Dictionary dict) {
    
        Set<String> keys = new HashSet<String>();
        Enumeration keysEnum = dict.keys();
        // Dump in all of the config props
        while (keysEnum.hasMoreElements())
            keys.add((String)keysEnum.nextElement());
        // Now remove all the ones we know are not actual <properties>
        keys.remove(GeminiPersistenceUnitProperties.PUNIT_NAME);
        keys.remove(GeminiPersistenceUnitProperties.PUNIT_BSN);
        keys.remove(GeminiPersistenceUnitProperties.PUNIT_CLASSES);
        keys.remove(GeminiPersistenceUnitProperties.PUNIT_EXCLUDE_UNLISTED_CLASSES);
        keys.remove(Constants.SERVICE_PID);
        keys.remove(ConfigurationAdmin.SERVICE_FACTORYPID);
        keys.remove(ConfigurationAdmin.SERVICE_BUNDLELOCATION);
        return keys;
    }
    
    // Refresh the bundle containing the given persistence unit.
    // (Do nothing if such a bundle is not present.)
    protected void refreshPersistenceUnitIfPresent(String unitName) {
        PUnitInfo unitInfo = mgr.getPUnitsByName().get(unitName);
        if (unitInfo != null) {
            mgr.getExtender().refreshBundle(unitInfo.getBundle());
        }
    }
}