# How to use jdeb with Maven

Generating a default Debian package with maven is particular easy. Just add
the plugin to your POM like this

```xml
  <build>
    <plugins>
      <plugin>
        <artifactId>jdeb</artifactId>
        <groupId>org.vafer</groupId>
        <version>1.0</version>
        <executions>
          <execution>
            <phase>package</phase>
            <goals>
              <goal>jdeb</goal>
            </goals>
            <configuration>
              <dataSet>
                <data>
                  <src>${project.build.directory}/${project.build.finalName}.jar</src>
                  <type>file</type>
                  <mapper>
                    <type>perm</type>
                    <prefix>/usr/share/jdeb/lib</prefix>
                  </mapper>
                </data>
              </dataSet>
            </configuration>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
```

At least the one main control file is required to control the creation of the
debian package. This required control file should be found in the control
directory. By default the control file name is also `control` which gives a
path of `src/deb/control/control` by default. This control file contains the
metadata about the Debian package. Usually it will look something along the lines of

    Package: [[name]]
    Version: [[version]]
    Section: misc
    Priority: optional
    Architecture: all
    Depends: jdk (>= 1.5)
    Maintainer: Torsten Curdt <torsten@something.com>
    Description: jetty java servlet container
    Distribution: development


If the environment variables `DEBEMAIL` and `DEBFULLNAME` are both set this
will overrule the `Maintainer` field set in there. The `Installed-Size` will
also be injected. If a changes file is used, the `Distribution` usually comes
from that file. The default changes file is called `CHANGES.txt`. See below
for the syntax of the content of the changes file.

Property replacement will also occur in any of the standard debian control
files: conffiles, preinst, postinst, prerm, postrm. This allows dynamic
configuration of the form:

    /etc/[[artifactId]]/[[artifactId]].properties
    /etc/[[artifactId]]/log4j.xml

If you now do a `mvn clean install`, the `deb` goal will be called and
artifacts consisting of the deb and potentially the changes file will
automatically be attached to the project.

The jdeb maven plugin also supports a variety of configuration options. These
configuration options provide the same features available in the jdeb ant
task. To configure the jdeb maven plugin, populate the jdeb configuration
section with any of the following options:

Element       | Description                                                                  | Required
------------- | ---------------------------------------------------------------------------- | -----------------------------------------------------------------
deb           | The debian package to be generated                                           | No; defaults to `${buildDirectory}/${artifactId}_${version}.deb`
type          | Artifact type                                                                | No; defaults to `deb`
classifier    | Artifact classifier                                                          | No; defaults to ''
controlDir    | The directory containing the control files                                   | No; defaults to `src/deb/control`
installDir    | The default directory for the project artifact if no data section is present | No; defaults to `/opt/${artifactId}`
dataSet       | A list of directories, tarballs, or files to include in the deb package      | No; defaults to include your maven artifact
changesIn     | The changes to add                                                           | No
changesOut    | The changes file generated                                                   | No
changesSave   | (NYI) The merged changes file                                                | No
compression   | (NYI) Compression method for the data file (`gzip`, `bzip2`, `xz` or `none`) | No; defaults to `gzip`
signPackage   | If the debian package should be signed                                       | No
signCfgPrefix | Prefix for when reading keyring, key and passphrase from settings.xml        | No; defaults to `jdeb.`
keyring       | The file containing the PGP keys                                             | No
key           | The name of the key to be used in the keyring                                | No
passphrase    | The passphrase to use the key                                                | No
attach        | Attach artifact to project                                                   | No; defaults to `true`
submodules    | Execute the goal on all sub-modules                                          | No; defaults to `true`
timestamped   | Turn SNAPSHOT into timestamps                                                | No; defaults to `false`
verbose       | Verbose logging                                                              | No; defaults to `true`, will be `false` in the future
skip          | Indicates if an execution should be skipped                                  | No; defaults to `false`

If you use the `dataSet` element, you'll need to populate it with a one or
more `data` elements. A `data` element is used to specify a directory, a
tarball archive, or a file. You can add as many data
elements to your dataSet as you'd like. The `data` element has the
following options:

Element          | Description                                                                  | Required
---------------- | ---------------------------------------------------------------------------- | ------------------------------------
src              | The directory, tarball, file to include in the package                       | Yes
dst              | New filename at destination (type must be `file`)                            | No
linkName         | The path of the link (type must be `link`)                                   | Yes for link
linkTarget       | The target of the link (type must be `link`)                                 | Yes for link
type             | Type of the data source. (archive, directory, file, link or template)        | No; but will be Yes in the future
missingSrc       | Fail if src file/folder is missing (ignore or fail)                          | No; defaults to `fail`
includes         | A comma seperated list of files to include from the directory or tarball     | No; defaults to all files
excludes         | A comma seperated list of files to exclude from the directory or tarball     | No; defaults to no exclutions
conffile         | A boolean value to define if the files should be included in the conffiles   | No; defaults to `false`
mapper           | The files to exclude from the directory or tarball                           | No
paths/(path..)   | One or more string literal paths that will created in the package            | No; Yes for type `template`

There are different kinds of mappers that can be selected via the `type` argument. The most common one is the 'perm' mapper.

Element       | Description                                           | Required
------------- | ----------------------------------------------------- | -----------------------
type          | 'perm'                                                | Yes
prefix        | Add this prefix to the files                          | No; defaults to ""
uid           | Numerical uid                                         | No; defaults to 0
gid           | Numerical gid                                         | No; defaults to 0
user          | User name                                             | No; defaults to "root"
group         | User group                                            | No; defaults to "root"
filemode      | File permissions as octet                             | No; deftauls to 644
dirmode       | Dir permissions as octet                              | No; defaults to 755
strip         | Strip n path components from the original file        | No; defaults to 0

Below is an example of how you could configure your jdeb maven plugin to
include a directory, a tarball, and a file in your deb package and then sign it with the key 8306FE21 in /home/user/.gnupg/secring.gpg:

```xml
  <build>
    <plugins>
      <plugin>
        <artifactId>jdeb</artifactId>
        <groupId>org.vafer</groupId>
        <version>1.0</version>
        <executions>
          <execution>
            <phase>package</phase>
            <goals>
              <goal>jdeb</goal>
            </goals>
            <configuration>
              <signPackage>true</signPackage>
              <keyring>/home/user/.gnupg/secring.gpg</keyring>
              <key>8306FE21</key>
              <passphrase>abcdef</passphrase>

              <dataSet>

                <!-- Tarball example -->
                <data>
                  <src>${project.basedir}/target/my_archive.tar.gz</src>
                  <type>archive</type>
                  <includes>...</includes>
                  <excludes>...</excludes>
                  <mapper>
                    <type>perm</type>
                    <strip>1</strip>
                    <prefix>/somewhere/else</prefix>
                    <user>tcurdt</user>
                    <group>tcurdt</group>
                    <filemode>600</filemode>
                  </mapper>
                </data>

                <!-- Directory example -->
                <data>
                  <src>${project.build.directory}/data</src>
                  <type>directory</type>
                  <includes/>
                  <excludes>**/.svn</excludes>
                  <mapper>
                    <type>ls</type>
                    <src>mapping.txt</src>
                  </mapper>
                </data>

                <!-- File example -->
                <data>
                  <src>${project.basedir}/README.txt</src>
                  <dst>README</dst>
                  <type>file</type>
                  <missingSrc>ignore</missingSrc>
                </data>

                <!-- Template example -->
                <data>
                  <type>template</type>
                  <paths>
                    <path>/etc/${artifactId}</path>
                    <path>/var/lib/${artifactId}</path>
                    <path>/var/log/${artifactId}</path>
                    <path>/var/run/${artifactId}</path>
                  </paths>
                </data>

                <!-- Hard link example -->
                <data>
                  <type>link</type>
                  <linkName>/a/path/on/the/target/fs</linkName>
                  <linkTarget>/a/link/to/the/scr/file</linkTarget>
                  <symlink>false</symlink>
                </data>

                <!-- Symbolic link example -->
                <data>
                  <type>link</type>
                  <linkName>/a/path/on/the/target/fs</linkName>
                  <linkTarget>/a/sym/link/to/the/scr/file</linkTarget>
                  <symlink>true</symlink>
                </data>

                <!-- Conffiles example -->
                <data>
                  <src>${project.build.directory}/data</src>
                  <type>directory</type>
                  <includes/>
                  <excludes>**/.svn</excludes>
                  <conffile>true</conffile>
                  <mapper>
                    <type>ls</type>
                    <src>mapping.txt</src>
                  </mapper>
                </data>
              </dataSet>

            </configuration>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
```
If you don't want to store your key information in the POM you can store this is your settings.xml, here's an example settings.xml:

    <settings>
        <profiles>
            <profile>
                <id>jdeb-signing</id>
                <properties>
                    <jdeb.keyring>/home/user/.gnupg/secring.gpg</jdeb.keyring>
                    <jdeb.key>8306FE21</jdeb.key>
                    <jdeb.passphrase>abcdef</jdeb.passphrase>
                </properties>
            </profile>
        </profiles>
        <activeProfiles>
            <activeProfile>jdeb-signing</activeProfile>
        </activeProfiles>
    </settings>

keyring, key and passphrase can then be omitted from the POM entirely.
