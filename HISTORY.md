## Version 1.1, released ?

* [ADD] Added support "Distribution" field
* [ADD] Added support of maven-encrypted passphrases
* [ADD] xz compression support
* [ADD] Added link support to the Ant task
* [ADD] Added support for system properties in parsed files (Thanks to David Sauer)
* [ADD] Support permission in (hard) link setup
* [ADD] User defined fields in the control file are now supported (Thanks to Marco Soeima)
* [ADD] Added the ability to disable an execution during Maven build (Thanks to  Jonathan Piron)
* [FIX] On Windows, parent directories are now created automatically when adding files to the data archive
* [CHG] Links are now symbolic by default
* [CHG] Alpha, Beta and RC versions are transformed to a package version ordered before the final release (ex: 1.0~RC1)
* [FIX] Permission mappers now work properly with Ant (Thanks to Christian Egli)
* [FIX] Symbolic links longer than 100 characters are now supported
* [FIX] The signed changes files now pass the validation with gpg --verify (Thanks to Max Garmash and Roman Kashitsyn)

## Version 1.0.1, released 28.02.2013

* [ADD] Override version via environment variable DEBVERSION
* [FIX] Use the joint copyright
* [FIX] Fixed the maven to debian version mapping


## Version 1.0, released 10.01.2013

* [ADD] Support for symbolic links (Maven only)
* [FIX] Unresolved variables in package maintainer scripts no longer break the build
* [CHG] Use "_all" as architecture postfix by default


## Version 0.11, released 14.07.2012

* [ADD] Added "timestamped" maven config to turn "SNAPSHOT" into a timestamp
* [ADD] Added "verbose" maven config option to show/hide INFO logs
* [ADD] Expand variables in configuration files "conffiles", "preinst", "postinst", "prerm", "postrm"
* [ADD] Added a "template" data type to create dirs
* [ADD] Added "missingSrc" maven config to control behavior on missing files/folders
* [CHG] Use "~" instead of "+" when converting from a SNAPSHOT version
* [REM] Deprecated PrefixMapper was removed (use PermMapper instead)


## Version 0.10, released 18.02.2012

Polishing and regression fixes

* [FIX] Don't throw exception when detecting zip archives
* [FIX] Make sure to close the tar output stream
* [CHG] Have warn/info level on messages
* [CHG] Fix line endings for control files
* [ADD] Access to all Maven variables


## Version 0.9, released 17.12.2011

Some smaller fixes, Support for the 1.8 format, Changes support working.

* [FIX] Default path for changes file with Maven
* [FIX] Unresolved variables are now treated as null
* [CHG] "Changes" support version 1.8 
* [CHG] Warn if control files have non-unix line endings
* [CHG] Throw an exception for unknown mappers
* [REM] InvalidDescriptorException, wasn't really used anyway
* [ADD] Added an "attach" attribute to specify whether maven artifact should be attached to project.
* [ADD] Provide SHA1, SHA256 and not just MD5 for descriptors
* [ADD] Provide "project.version" when using maven


## Version 0.8, released 27.06.2010

Lot of refactoring and support for configuration on the maven plugin. Easier permission mapping.

* [CHG] Default maven artifact type is now "deb" instead of "deb-archive".
* [CHG] Switched to commons compress for archive building.
* [CHG] Renamed the maven goal from "deb" to "jdeb" to be more consistent.
* [CHG] Maven goal no longer attached to execution phase by default. (see examples)
* [CHG] Deprecated "FileSetDataProducer" in favor of "DataProducerFileSet"
* [ADD] Added a "type" attribute to the "data" elements.
* [ADD] New options to the maven plugin to configure the attached artifact.
* [ADD] Added Examples for ant and maven.
* [ADD] New "file" data source.


## Version 0.7, released 18.08.2008

Proper closing of streams!
Many improvements on the Ant task.
Quite a few fixes related to locale settings.
Support for bzip2 and more descriptor keys.

* [FIX] English locale for date format.	
* [FIX] Proper installation size to be kbytes instead of bytes.	
* [FIX] Close streams properly.	
* [CHG] The Ant task now breaks on errors.
* [ADD] Support for bzip2 compression in data element of the Ant task.
* [ADD] Compression attribute to specify data file compression (bzip2, gzip, none).
* [ADD] More package descriptor keys (Pre-Depends, Recommends, Suggests, Breaks, Enhances, Homepage>).
* [ADD] Verbose attribute for the Ant task.
* [ADD] The Ant task now accepts tarfileset elements.


## Version 0.6, released 11.01.2008

* [FIX] Fixed the trailing linefeed in the 'changes' section of the changes file.


## Version 0.5, released 26.11.2007

* [FIX] Fixed the 'ls' parsing.
* [CHG] Switched to ArInputStream/ArOutputStream.
* [REM] Removed deprecated ant task delegate.
* [ADD] Added "changesSave" attribute to save release information to. No longer saving those information to "changesIn".


## Version 0.4, released 20.09.2007

* [FIX] Fixed mapper support.
* [CHG] Changed lookup from environment (DEBEMAIL, DEBFULLNAME) to overrule the descriptor.
* [REM] Removed deprecated prefix/strip syntax.
* [ADD] Added support for multiple mappers.
* [ADD] Added more fields to the package descriptor.


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
