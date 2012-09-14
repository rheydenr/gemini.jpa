
Gemini JPA 1.1.0-RC1 - Sept 14, 2012

This release provides you with access to the EclipseLink JPA 2.0 provider. The 
bundles in the EclipseLink 2.4.0 release or later should be used.

For an example of how to access an EntityManager from an OSGi program see the 
sample bundles (e.g. org.eclipse.gemini.jpa.sample.basic).
To do this, install and start the bundles described in "GettingStarted.txt" (included in this 
distribution). 

Bugs Fixed in 1.1.0-RC1:

388986	Multiple EMFs can cause PUnitInfo to be corrupted
389198	Multiple EMF per persistence unit leaves an orphaned weaving service hook
389508	Weaving service needs to have PU properties registered with it

Bugs Fixed in 1.1.0-M2:

366017  ClassNotFoundException: java/sql/Timestamp on fields annotated with @Version
369029  Support JPA access to NoSQL databases
384168  Core needs to be more test friendly and make its config queryable
387988	Option to not refresh is not fully implemented by extender
385170  Property "eclipselink.classloader" unusable because overridden by Gemini own class loader
385788  Remove attempt at loose coupling with EclipseLink
385790  Create new activator and remove life cycle code from EclipseLinkOSGiProvider class
384323  Missing persistence.xml leads to infinite start/stop loop
360246  Generic Capabilities help to avoid bundle refresh
365619  Integrate with properties from Config Admin service

Bugs Fixed in 1.1.0-M1:

366040  Two EMFBuilder Service instances for one PU
367873  GeminiProperties.setDebugWeaving method typo
370166  PlainDriverDataSource class needs to be upgraded to work in Java SE 7
374804  Fix leftover compiler warnings
344951  Patch proposal for issue with generation of fragment bundles
375424  Fix debug functions to print out state when undefined
375628  Refactor gemini properties
364748  MalformedURLException when parsing persistence.xml in persistence bundle
364873  Calling Equal on an EntityManagerFactoryBuilder causes ClassCastException
365811  resolveGenericTypes causes IndexOutOfBounds in MetadataAsmFactory
373288  Discriminate JDBC drivers based on version

