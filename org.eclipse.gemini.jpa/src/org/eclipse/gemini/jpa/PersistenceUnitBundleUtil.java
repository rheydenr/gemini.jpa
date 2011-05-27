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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import org.eclipse.persistence.internal.libraries.asm.ClassWriter;
import org.eclipse.persistence.internal.libraries.asm.Constants;

import org.eclipse.gemini.jpa.xml.PersistenceDescriptorHandler;

import static org.eclipse.gemini.jpa.GeminiUtil.*;

/**
 * Utility class that implements functionality for JPA providers 
 * to be able to support the OSGi JPA specification.
 * 
 * This class provides some of the functionality for processing
 * a persistence unit bundle.
 */
public class PersistenceUnitBundleUtil {
    
    public static final String JPA_MANIFEST_HEADER = "Meta-Persistence"; 
    public static final String EMBEDDED_JAR_SEPARATOR = "!/"; 
    public static final String DEFAULT_DESCRIPTOR_PATH = "META-INF/persistence.xml"; 

    /**
     * Return a List of PersistenceDescriptorInfo, each of which contains info about 
     * a persistence descriptor in the bundle. An entry to the default file will be 
     * included if it exists in the bundle.
     * 
     * @param b the persistence unit bundle 
     * 
     * @return a List of PersistenceDescriptorInfo
     */
    public List<PersistenceDescriptorInfo> persistenceDescriptorInfos(Bundle pUnitBundle) {
        List<PersistenceDescriptorInfo> descInfos = new ArrayList<PersistenceDescriptorInfo>();
        debug("Looking for persistence descriptors in bundle ", pUnitBundle.getSymbolicName());
        
        // Add default META-INF/persistence.xml if it exists in the bundle
        URL defaultUrl = pUnitBundle.getEntry(DEFAULT_DESCRIPTOR_PATH);
        if (defaultUrl != null) {
            descInfos.add(new PersistenceDescriptorInfo(defaultUrl, DEFAULT_DESCRIPTOR_PATH));
        }

        Object headerEntry = pUnitBundle.getHeaders().get(JPA_MANIFEST_HEADER);
        
        // If no entries were specified then we're done
        if (headerEntry == null) return descInfos;

        // Iterate through all of the specified Meta-Persistence entries
        for (String paddedPath : headerEntry.toString().split(",")) {
            String path = stripPrecedingSlash(paddedPath.trim());
            
            // If standard path was specified ignore it since we already added it above
            if ((path.length() == 0) || path.equals(DEFAULT_DESCRIPTOR_PATH)) {
                continue;
            }
            // Check if it is an embedded JAR path
            int splitPosition = path.indexOf(EMBEDDED_JAR_SEPARATOR);
            URL url = null;
            if (splitPosition == -1) {
                // Not an embedded JAR path, just get URL from bundle call
                url = pUnitBundle.getEntry(path);
                if (url != null)
                    descInfos.add(new PersistenceDescriptorInfo(url, path));
                else
                    warning("Could not find JPA descriptor: ", path);
            } else {
                // It's an embedded JAR path, we need to do some work to get the info
                String jarPrefixPath = path.substring(0, splitPosition);
                String descPath = path.substring(splitPosition+2);
                descPath = stripPrecedingSlash(descPath);
                debug("Descriptor JAR prefix: ", jarPrefixPath);
                debug("Embedded descriptor suffix: ", descPath);
                URL prefixUrl = pUnitBundle.getEntry(jarPrefixPath);
                debug("Embedded JAR url: ", prefixUrl);
                if (prefixUrl != null) {
                    descInfos.add(new PersistenceDescriptorInfo(prefixUrl, descPath, jarPrefixPath));
                } else {
                    warning("Could not find nested JAR: ", jarPrefixPath);                    
                    continue;
                }
            }
        }
        debug("Found persistence descriptors: ", descInfos);
        return descInfos;
    }

    /**
     * Process each of the descriptor infos passed in and return a Set of 
     * PartialPersistenceUnitInfo objects, one for each persistence unit.
     * The descriptor infos are expected to be in the same bundle.
     * 
     * @param descInfos a List of PersistenceDescriptorInfo with each 
     *                  referring to a different persistence descriptor file
     * 
     * @return a Set of < partial persistence unit information >
     */
    public Set<PUnitInfo> persistenceUnitInfoFromXmlFiles(List<PersistenceDescriptorInfo> descriptorInfos) {

        // Set of p-unit info
        Set<PUnitInfo> pUnits = new HashSet<PUnitInfo>();
        // Set of p-unit names to ensure no duplicates
        Set<String> pUnitNames = new HashSet<String>();
        
        // Read each of the persistence descriptor files
        InputStream in = null;
        PersistenceDescriptorHandler handler = null;
        for (PersistenceDescriptorInfo info : descriptorInfos) {
            try { 
                debug("Parsing persistence descriptor ", info.getDescriptorPath());
                // Open a stream on the descriptor
                in = info.getDescriptorStream();
                // Create a parser
                SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
                // Parse the file
                handler = new PersistenceDescriptorHandler();
                parser.parse(in, handler);
                debug("Finished parsing persistence descriptor ", info.getUrl());
            } catch(Throwable ex) {
                fatalError("Could not parse XML descriptor ", ex);
            } finally {
                close(in);
            }
            // Get the p-units found in the current descriptor file
            Set<PUnitInfo> newPUnits = handler.getPersistenceUnitInfo();
            debug("Found " + newPUnits.size() + " persistence units");

            // Cycle through the newly found p-units ensuring there are no duplicates
            // and setting the URL
            for (PUnitInfo unitInfo : newPUnits) {
                String unitName = unitInfo.getUnitName();
                if (pUnitNames.contains(unitName)) {
                    fatalError("Persistence unit " + unitName + " already defined ", null);
                }
                pUnitNames.add(unitName);
                // Set the URL 
                unitInfo.setDescriptorInfo(info);
            }
            // Add the new ones to the list of processed p-units
            pUnits.addAll(newPUnits);
        }
        debug("Parsed persistence descriptors: ", pUnits);
        return pUnits;
    }

    /**
     * Return a Set of the unique packages in which a JPA managed class exists.
     * The "." characters will be replaced by "/" chars, with a trailing slash.
     *  
     * @param pUnits the collection of p-units in the bundle 
     * 
     * @return a List of unique package names in all p-units in the bundle
     */
    public List<String> uniquePackages(Collection<PUnitInfo> pUnits) {
        Set<String> packageNamesInBundle = new HashSet<String>();

        // Iterate through all the p-units in the bundle
        for (PUnitInfo info : pUnits) {
            Set<String> packageNamesInUnit = new HashSet<String>();
            
            // Iterate through each managed class in the p-unit
            for (String classString : info.getClasses()) {
                int idx = classString.lastIndexOf('.');
                String packageName = classString.substring(0,idx);
                String formattedName = formattedPackageString(packageName, '.', '/');
                packageNamesInUnit.add(formattedName);
                packageNamesInBundle.add(formattedName);
            }
            // Convert to a List and set the local p-unit package names in the p-unit info
            List<String> namesInUnit = new ArrayList<String>();
            namesInUnit.addAll(packageNamesInUnit);
            info.setUniquePackageNames(namesInUnit);
        }
        // Convert to a List before returning
        List<String> namesInBundle = new ArrayList<String>();
        namesInBundle.addAll(packageNamesInBundle);
        debug("Unique managed class package names: ", namesInBundle);
        return namesInBundle;
    }

    /**
     * Generate an anchor interface for each unique package in which a 
     * JPA managed class exists. The anchors will all get generated as the 
     * specified class name, but in different packages.
     * 
     * @param packageNames the collection of package name strings. They are 
     *                     assumed to be in the format returned by {@link #uniquePackages() uniquePackages}
     * @param className the name of the generated interface
     * 
     * @return an ordered List of bytecode arrays, each being a generated anchor 
     *         interface, in the order that the package names were in
     */
    public List<byte[]> generateAnchorInterfaces(List<String> packageNames, String className) {
        List<byte[]> bytes = new ArrayList<byte[]>();
        
        // Iterate through the packageNames, generating an interface for each one
        for (String packageName : packageNames) {
            ClassWriter writer = new ClassWriter(false);
            String fullClassName = formattedPackageString(packageName, '.', '/') + className;
            final int CLASS_ACCESS = Constants.ACC_PUBLIC + Constants.ACC_ABSTRACT + Constants.ACC_INTERFACE;
            writer.visit(Constants.V1_5, CLASS_ACCESS, fullClassName, "java/lang/Object", null, null);
            writer.visitEnd();
            bytes.add(writer.toByteArray());
        }
        return bytes;
    }

    /**
     * Generate a manifest for the fragment. No additional imports or exports have been added.
     * 
     * @param pUnitBundle the persistence unit bundle collection of package name strings. They are 
     * 
     * @return a Manifest with its headers appropriately filled in
     */
    public Manifest generateFragmentManifest(Bundle pUnitBundle){
        Manifest manifest = new Manifest();
        Attributes attrs = manifest.getMainAttributes();
        debug("Creating manifest "); 
        
        // Standard manifest versions
        attrs.putValue("Manifest-Version", "1.0");
        attrs.putValue("Bundle-ManifestVersion", "2");
        
        // Informational Entries
        attrs.putValue("Bundle-Name", "JPA Fragment");
        attrs.putValue("Bundle-Description", "Generated JPA Fragment");

        // Use a mapping from the p-unit bundle name to the fragment name 
        String fragmentName = fragmentSymbolicName(pUnitBundle);
        attrs.putValue("Bundle-SymbolicName", fragmentName);

        // Import the org.osgi.service.jpa package to allow access to the EMFBuilder class
        String packageImport = "org.osgi.service.jpa";
        attrs.putValue("Import-Package", packageImport);

        // Adopt the version of the persistence unit bundle so it is unique
        String hostVersion = bundleVersion(pUnitBundle);
        attrs.putValue("Bundle-Version", hostVersion);

        // Lock this fragment to the symbolic name and version of the persistence unit bundle
        String fragmentHost = pUnitBundle.getSymbolicName() + ";bundle-version=" + hostVersion; 
        attrs.putValue("Fragment-Host", fragmentHost);

        debug("Created manifest for ", fragmentName); 
        return manifest;
    }
   
    // Out of the blue...
    public static final int ESTIMATED_FRAGMENT_SIZE = 2000;
    
    /**
     * Create and return a fragment bundle as a byte array. The manifest and generated classes 
     * that were passed in are written to the fragment.
     *  
     * @param manifest the manifest to use in the fragment 
     * @param packageNames the names of the packages in which interfaces were generated  
     * @param className the name of the interfaces that were generated 
     * @param generatedClasses the byte arrays of generated interfaces
     * 
     * @return a byte array of the fragment bundle data
     */    
    public byte[] createFragment(
            Manifest manifest, 
            List<String> packageNames,
            String className,
            List<byte[]> generatedClasses) {
        
        debug("Creating fragment "); 
        ByteArrayOutputStream baos = new ByteArrayOutputStream(ESTIMATED_FRAGMENT_SIZE);
        BufferedOutputStream bos = new BufferedOutputStream(baos);
        JarOutputStream jos = null;
        try { 
            jos = new JarOutputStream(bos, manifest);
            for (String p : packageNames) {
                JarEntry entry = new JarEntry(
                        formattedPackageString(p, '.', '/') + className + ".class");
                jos.putNextEntry(entry);
                jos.write(generatedClasses.remove(0));
            }
            debug("Successfully created fragment ");
            jos.close();
            return baos.toByteArray();
        } catch (Exception ex) { 
            fatalError("Error creating fragment: ", ex); 
        } finally { 
            close(jos);
            close(bos);
            close(baos);
        }
        return null;
    }    

    /**
     * Install the fragment into the framework. Return the installed bundle if the fragment 
     * was successfully installed, or null if it could not be installed.
     *  
     * @param pUnitBundle the persistence unit bundle 
     * @param providerBundle the bundle of the provider
     * @param bundleData the byte array that represents the fragment bundle 
     * 
     * @return the bundle that was created or null if none was able to be created
     */    
    public Bundle installFragment(Bundle pUnitBundle, Bundle providerBundle, byte[] bundleData) { 
        
        ByteArrayInputStream bais = new ByteArrayInputStream(bundleData);
        String locationUrl = null;
        Bundle fragmentBundle = null;
        try {
            locationUrl = fragmentLocation(pUnitBundle);
            debug("Installing fragment: ", locationUrl);
            // Use the provider bundle ctx to install the fragment
            BundleContext ctx = providerBundle.getBundleContext();
            fragmentBundle = ctx.installBundle(locationUrl, bais);
        } catch (Exception ex) { 
            fatalError("Could not install fragment: " + locationUrl + ": ", ex);
        }
        return fragmentBundle;
    }
        
    /* ============== */
    /* Helper methods */
    /* ============== */
    
    // Function to create a unique fragment location identifier from a p-unit bundle
    public String fragmentLocation(Bundle pUnitBundle) {
        return fragmentSymbolicName(pUnitBundle) + "_" + bundleVersion(pUnitBundle);
    }

    // Function to create a symbolic name of the fragment based on the symbolic name of the p-unit bundle
    public String fragmentSymbolicName(Bundle b) {
        return "jpa$fragment." + b.getSymbolicName();
    }
}
