## Version 0.9, unreleased

* [ADD] Added an "attach" attribute to specify whether maven artifact should be attached to project.
* [CHG] "Changes" support version 1.8 
* [ADD] Provide SHA1, SHA256 and not just MD5 for descriptors
* [ADD} Provide "project.version" when using maven


## Version 0.8, released 27.06.2010

Lot of refactoring and support for configuration on the maven plugin. Easier permission mapping.

* [ADD] New options to the maven plugin to configure the attached artifact.
* [ADD] Added Examples for ant and maven.
* [ADD] New "file" data source.
* [ADD] Added a "type" attribute to the "data" elements.
* [CHG] Default maven artifact type is now "deb" instead of "deb-archive".
* [CHG] Switched to commons compress for archive building.
* [CHG] Renamed the maven goal from "deb" to "jdeb" to be more consistent.
* [CHG] Maven goal no longer attached to execution phase by default. (see examples)
* [CHG] Deprecated "FileSetDataProducer" in favor of "DataProducerFileSet"


## Version 0.7, released 18.08.2008

Proper closing of streams!
Many improvements on the Ant task.
Quite a few fixes related to locale settings.
Support for bzip2 and more descriptor keys.

* [FIX] English locale for date format.	
* [FIX] Proper installation size to be kbytes instead of bytes.	
* [FIX] Close streams properly.	
* [ADD] Support for bzip2 compression in data element of the Ant task.
* [ADD] Compression attribute to specify data file compression (bzip2, gzip, none).
* [ADD] More package descriptor keys (Pre-Depends, Recommends, Suggests, Breaks, Enhances, Homepage>).
* [ADD] Verbose attribute for the Ant task.
* [ADD] The Ant task now accepts tarfileset elements.
* [CHANGE] The Ant task now breaks on errors.


## Version 0.6, released 11.01.2008

* [FIX] Fixed the trailing linefeed in the 'changes' section of the changes file.


## Version 0.5, released 26.11.2007

* [REMOVED] Removed deprecated ant task delegate.
* [ADD] Added "changesSave" attribute to save release information to. No longer saving those information to "changesIn".
* [CHANGE] Switched to ArInputStream/ArOutputStream.
* [FIX] Fixed the 'ls' parsing.


## Version 0.4, released 20.09.2007

* [FIX] Fixed mapper support.
* [ADD] Added support for multiple mappers.
* [ADD] Added more fields to the package descriptor.
* [CHANGE] Changed lookup from environment (DEBEMAIL, DEBFULLNAME) to overrule the descriptor.
* [REMOVED] Removed deprecated prefix/strip syntax.


## Version 0.3, released 15.09.2007

* [ADD] Added maintainer lookup from environment (DEBEMAIL, DEBFULLNAME).
* [ADD] Added plugin implementation for maven.
* [ADD] Added stricter descriptor validation.


## Version 0.2, released 21.08.2007

* [FIX] Fixed the delete of the temporary files.
* [ADD] Added support for signed changes files.
* [ADD] Added support for mapping ownerships and rights.


## Version 0.1, released 19.02.2007

Initial release.
