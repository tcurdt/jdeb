## Version 1.12, dev

* [FIX] Re-add the maven archiver dependency (Thanks to Jeremy Norris)
* [FIX] Fixed timestamp parsing deprecation (Thanks to Jeremy Norris)

## Version 1.11, release 17.08.2024

* [ADD] "signDigest" for setting just the signing digest
* [CHG] Required java 11
* [CHG] Separate digest settings for creating the deb and signing it
* [CHG] Upgraded deps
* [FIX] Removed invalid Maven Mojo Deprecation
* [CHG] Improve exception propagation for failures from package content processing

## Version 1.10, released 30.11.2021

* [CHG] Breaking: The deps already required java8. Now also require java8 officially.
* [CHG] Upgraded deps
* [CHG] The default digest algorithm for PGP signatures is now SHA-256 instead of SHA-1
* [ADD] Allow constant modified time to support reproducibility (Thanks to Michal Riha)
* [FIX] Sort control files for reproducibility (Thanks to Tomas Saghy)
* [FIX] Ensure conffile paths are not absolute anymore

## Version 1.9, released 05.06.2021

* [CHG] Upgraded plugins and dependencies, now requires maven 3.6.0 and java 1.7
* [ADD] Added project.inceptionYear, project.organization.name, and project.organization.url to the variable expansions (Thanks to Adam Retter)
* [ADD] Also interpolate the copyright file (Thanks to Adam Retter)

## Version 1.8, released 24.08.2019

* [FIX] Removed unnessary file/path limitation (Thanks to Roberto Perez Alcolea)
* [CHG] Upgraded deps

## Version 1.7, released 16.10.2018

* [FIX] Fixed a regression. Always provide the proper relative paths in archives.
* [FIX] Upgraded deps
* [FIX] Windows fixes
* [ADD] Added example to show file and dirmode
* [ADD] Added support custom snapshot patterns (Thanks to Jamie Magee)

## Version 1.6, released 03.01.2018

* [CHG] Upgraded Commons Compress
* [FIX] Fixed the maintainer name according to Debian standards (Thanks to Bogdan Artyushenko)
* [FIX] Fixed deriving the changes file (Thanks to Reinhard Pointner)
* [ADD] Added support for the fullpath attribute in Ant (Thanks to (Daniel Ivan Ruiz Barranco)
* [ADD] Allow different styles of tar archives (Thanks to Kevin Conaway)

## Version 1.5, released 01.02.2016

* [CHG] Breaking: No longer provides a default "Depends" add to your control as needed
* [CHG] Breaking: Use TarArchiveEntry in DataConsumer interface
* [FIX] Fixed maven to debian version conversion
* [FIX] Allow comments in control files
* [FIX] Fixed incorrectly handled links for tar archives
* [FIX] Fixed leading zeros on PGP hex strings
* [ADD] Provide more attributes maven properties (version, deb.name, changes.name, changes.txt.name)
* [ADD] Added option to provide a propertyPrefix
* [ADD] Allow Multi-Arch declarations
* [ADD] Allow overriding of Source

## Version 1.4, released 20.02.2014

* [FIX] Support comments in control files
* [FIX] Fixed handling of links
* [FIX] Fixed hex format to use leading 0 when signing
* [ADD] Provide jdeb build information back to maven (if propertyPrefix is set)
* [ADD] Allow for Multi-Arch declaration in control files

## Version 1.3, released 25.07.2014

* [CHG] Switched from maven2 to maven3
* [FIX] Fixed badly formatted version for "milestone", "m", "a", "b" and "cr" (they where not matched as beta versions)
* [FIX] Add two spaces in the md5sums file between the checksum and the filename to be compatible with GNU coreutils md5sum

## Version 1.2, released 22.06.2014

* [CHG] Deprecated "submodules" in favour of "skipSubmodules"
* [FIX] Warned about missing signing settings even when not signing
* [FIX] Fixed variable replacement issue on line endings
* [ADD] Support &lt;packaging&gt;deb&lt;/packaging&gt; in Maven
* [ADD] Support for "signMethod" and "signRole"
* [ADD] Added "skipPOMs"

## Version 1.1.1, released 10.03.2014

* [FIX] renamed SNAPSHOT handling to snapshotExpand & snapshotEnv

## Version 1.1, released 28.02.2014

* [CHG] Links are now symbolic by default
* [CHG] Alpha, Beta and RC versions are transformed to a package version ordered before the final release (ex: 1.0~RC1)
* [CHG] Renamed configuration option 'disabled' to 'skip'
* [FIX] Better token parsing
* [FIX] On Windows, parent directories are now created automatically when adding files to the data archive
* [FIX] Permission mappers now work properly with Ant (Thanks to Christian Egli)
* [FIX] Symbolic links longer than 100 characters are now supported
* [FIX] The signed changes files now pass the validation with gpg --verify (Thanks to Max Garmash and Roman Kashitsyn)
* [ADD] Added support for conffiles (Thanks to Lukas Roedl)
* [ADD] Added "files" data producer (Thanks to Roman Kashitsyn)
* [ADD] Added support "Distribution" field
* [ADD] Added support of maven-encrypted passphrases
* [ADD] xz compression support
* [ADD] Added link support to the Ant task
* [ADD] Added support for system properties in parsed files (Thanks to David Sauer)
* [ADD] Support permission in (hard) link setup
* [ADD] User defined fields in the control file are now supported (Thanks to Marco Soeima)
* [ADD] Added the ability to disable an execution during Maven build (Thanks to  Jonathan Piron)


## Version 1.0.1, released 28.02.2013

* [FIX] Use the joint copyright
* [FIX] Fixed the maven to debian version mapping
* [ADD] Override version via environment variable DEBVERSION


## Version 1.0, released 10.01.2013

* [CHG] Use "_all" as architecture postfix by default
* [FIX] Unresolved variables in package maintainer scripts no longer break the build
* [ADD] Support for symbolic links (Maven only)


## Version 0.11, released 14.07.2012

* [REM] Deprecated PrefixMapper was removed (use PermMapper instead)
* [CHG] Use "~" instead of "+" when converting from a SNAPSHOT version
* [ADD] Added "timestamped" maven config to turn "SNAPSHOT" into a timestamp
* [ADD] Added "verbose" maven config option to show/hide INFO logs
* [ADD] Expand variables in configuration files "conffiles", "preinst", "postinst", "prerm", "postrm"
* [ADD] Added a "template" data type to create dirs
* [ADD] Added "missingSrc" maven config to control behavior on missing files/folders


## Version 0.10, released 18.02.2012

Polishing and regression fixes

* [CHG] Have warn/info level on messages
* [CHG] Fix line endings for control files
* [FIX] Don't throw exception when detecting zip archives
* [FIX] Make sure to close the tar output stream
* [ADD] Access to all Maven variables


## Version 0.9, released 17.12.2011

Some smaller fixes, Support for the 1.8 format, Changes support working.

* [REM] InvalidDescriptorException, wasn't really used anyway
* [CHG] "Changes" support version 1.8
* [CHG] Warn if control files have non-unix line endings
* [CHG] Throw an exception for unknown mappers
* [FIX] Default path for changes file with Maven
* [FIX] Unresolved variables are now treated as null
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

* [REM] Removed deprecated ant task delegate.
* [CHG] Switched to ArInputStream/ArOutputStream.
* [FIX] Fixed the 'ls' parsing.
* [ADD] Added "changesSave" attribute to save release information to. No longer saving those information to "changesIn".


## Version 0.4, released 20.09.2007

* [REM] Removed deprecated prefix/strip syntax.
* [CHG] Changed lookup from environment (DEBEMAIL, DEBFULLNAME) to overrule the descriptor.
* [FIX] Fixed mapper support.
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
