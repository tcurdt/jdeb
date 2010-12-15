# Howto use "jdeb" with maven

Generating a default Debian package with maven is particular easy. Just add
the plugin to your POM like this

    <build>
        <plugins>
            <plugin>
                <artifactId>jdeb</artifactId>
                <groupId>org.vafer</groupId>
                <version>0.8</version>
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
  
At least the one main control file is required to be present at
'src/deb/control/control'. It contains the information for the Debian package
descriptor. Usually it will look something along the lines of

    Package: [[name]]
    Version: [[version]]
    Section: misc
    Priority: optional
    Architecture: all
    Depends: jdk (>= 1.5)
    Maintainer: Torsten Curdt <torsten@something.com>
    Description: jetty java servlet container
    Distribution: development

If the enviroment variables 'DEBEMAIL' and 'DEBFULLNAME' are both set this
will overrule the 'Maintainer' field set in there. The 'Installed-Size' will
also be injected. If a changes file is used, the 'Distribution' usually comes
from that file. The default changes file is called 'CHANGES.txt'. See below
for the syntax of the content. Now if you do a 'mvn clean install' the
attached 'deb' goal is getting called and the artifacts -the deb and
potentially the changes file- will automatically get attached to the project.

The jdeb maven plugin also supports a variety of configuration options. These
configuration options provide the same features available in the jdeb ant
task. To configure the jdeb maven plugin, populate the jdeb configuraiton
section with any of the following options:

    *---------------+------------------------------------------------------------------------------+-----------------------------------------------------+
    ||   Element    || Description                                                                 || Required                                          ||
    *---------------+------------------------------------------------------------------------------+-----------------------------------------------------+
    | deb           | The debian package to be generated                                           | No; defaults to 'artifactId'_'version'.deb          |
    *---------------+------------------------------------------------------------------------------+-----------------------------------------------------+
    | type          | Artifact type                                                                | No; defaults to 'deb'                               |
    *---------------+------------------------------------------------------------------------------+-----------------------------------------------------+
    | classifier    | Artifact classifier                                                          | No; defaults to ''                                  |
    *---------------+------------------------------------------------------------------------------+-----------------------------------------------------+
    | controlDir    | The directory containing the control files                                   | No; defaults to src/deb/control                     |
    *---------------+------------------------------------------------------------------------------+-----------------------------------------------------+
    | installDir    | The directory where your file(s) will be place when your deb is installed    | No; defaults to /opt/'artifactId'                   |
    *---------------+------------------------------------------------------------------------------+-----------------------------------------------------+
    | dataSet       | A list of directories, tarballs, or files to include in the deb package      | No; defaults to include your maven artifact         |
    *---------------+------------------------------------------------------------------------------+-----------------------------------------------------+
    | changesIn     | The changes to add                                                           | No                                                  |
    *---------------+------------------------------------------------------------------------------+-----------------------------------------------------+
    | changesOut    | The changes file generated                                                   | No                                                  |
    *---------------+------------------------------------------------------------------------------+-----------------------------------------------------+
    | changesSave   | (NYI) The merged changes file                                                | No                                                  |
    *---------------+------------------------------------------------------------------------------+-----------------------------------------------------+
    | compression   | (NYI) Compression method for the data file ('gzip', 'bzip2' or 'none')       | No; defaults to 'gzip'                              |
    *---------------+------------------------------------------------------------------------------+-----------------------------------------------------+
    | keyring       | (NYI) The file containing the PGP keys                                       | No                                                  |
    *---------------+------------------------------------------------------------------------------+-----------------------------------------------------+
    | key           | (NYI) The name of the key to be used in the keyring                          | No                                                  |
    *---------------+------------------------------------------------------------------------------+-----------------------------------------------------+
    | passphrase    | (NYI) The passphrase to use the key                                          | No                                                  |
    *---------------+------------------------------------------------------------------------------+-----------------------------------------------------+
    | attach        | Attach artifact to project                                                   | No; defaults to 'true'                              |
    *---------------+------------------------------------------------------------------------------+-----------------------------------------------------+
  
If you use the 'dataSet' element, you'll need to populate it with a one or
more 'data' elements. A 'data' element is used to specify a 'directory', a
'tarball' archive, or a 'file'. You can add as many 'data'
elements to your 'dataSet' as you'd like. The 'data' element has the
following options:

    *---------------+------------------------------------------------------------------------------+---------------------------------------------+
    ||   Element    || Description                                                                 || Required                                  ||
    *---------------+------------------------------------------------------------------------------+---------------------------------------------+
    | src           | The directory, tarball, or file to include in the package                    | Yes                                         |
    *---------------+------------------------------------------------------------------------------+---------------------------------------------+
    | type          | Type of the data source. (archive|directory|file)                            | No; but will be Yes in the future           |
    *---------------+------------------------------------------------------------------------------+---------------------------------------------+
    | includes      | A comma seperated list of files to include from the directory or tarball     | No; defaults to all files                   |
    *---------------+------------------------------------------------------------------------------+---------------------------------------------+
    | excludes      | A comma seperated list of files to exclude from the directory or tarball     | No; defaults to no exclutions               |
    *---------------+------------------------------------------------------------------------------+---------------------------------------------+
    | mapper        | The files to exclude from the directory or tarball                           | No                                          |
    *---------------+------------------------------------------------------------------------------+---------------------------------------------+
  
Below is an example of how you could configure your jdeb maven plugin to
include a directory, a tarball, and a file in your deb package:

    <build>
        <plugins>
            <plugin>
                <artifactId>jdeb</artifactId>
                <groupId>org.vafer</groupId>
                <version>0.8</version>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals>
                            <goal>jdeb</goal>
                        </goals>
                        <configuration>
                            ...
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
                                        <mode>600</mode>
                                    </mapper>
                                </data>
                                ...
                                <!-- Directory example -->
                                <data>
                                    <src>${project.build.directory}/data</src>
                                    <type>directory</type>
                                    <includes />
                                    <excludes>**/.svn</excludes>
                                    <mapper>
                                        <type>ls</type>
                                        <src>mapping.txt</src>
                                    </mapper>
                                </data>
                                ...
                                <!-- File example -->
                                <data>
                                    <src>${project.basedir}/README.txt</src>
                                    <type>file</type>
                                </data>
                            </dataSet>
                            ...
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
