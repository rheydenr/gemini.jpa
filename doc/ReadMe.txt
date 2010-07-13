Getting Started with Gemini JPA 
--------------------------------

Gemini JPA implements the OSGi JPA specification and enables EclipseLink to act as the JPA provider
of the specified services.

Prerequisites
-------------

To run Gemini JPA in OSGi you will need to have the following:

1. Gemini DBAccess (Optional)

While you don't strictly require Gemini DBAccess to run Gemini JPA it renders your application much more
modular and easier to configure. If DBAccess is not present you will need to make the JDBC driver accessible 
to EclipseLink.

2. OSGi Enterprise API bundle

Some of the OSGi Enterprise APIs are used by Gemini JPA so the osgi.enterprise bundle must be resident. 
It includes both the source and the class files so it can be used for both execution and debugging.

3. EclipseLink bundles

The following EclipseLink bundles are required:

- org.eclipse.persistence.asm
- org.eclipse.persistence.antlr
- org.eclipse.persistence.core
- org.eclipse.persistence.jpa

These bundles must be version 2.1.1 or greater.

4. Gemini JPA bundles

There are two Gemini JPA bundles:

- org.elipse.gemini.javax.persistence
- org.elipse.gemini.jpa


Installation
------------


Configuration
-------------


Execution
---------

If you use Plugin Development Environment (PDE) then load all of the bundles into your workspace:




