# How to use jdeb with Ant

Attribute     | Description                                                                  | Required
------------- | ---------------------------------------------------------------------------- | --------------------------
destfile      | The debian package to be generated                                           | Yes
control       | The directory containing the control files                                   | Yes
compression   | Compression method for the data file (`gzip`, `bzip2`, `xz` or `none`)       | No; defaults to `gzip`
verbose       | Print detailed info during the package generation                            | No; defaults to `false`
keyring       | The file containing the PGP keys                                             | No
key           | The name of the key to be used in the keyring                                | No
passphrase    | The passphrase to use the key                                                | No
changesIn     | The changes to add                                                           | No
changesOut    | The changes file generated                                                   | No
changesSave   | The merged changes file                                                      | No

The jdeb Ant task can package up a directory as Debian package. You have to
provide the control files defining meta information about the package (except
the `md5sums` which gets created automatically). It creates the archive
and if you want even a signed changes file.

```xml
    <target name="package">
      <taskdef name="deb" classname="org.vafer.jdeb.ant.DebAntTask"/>
      <copy todir="${deb}/control">
        <fileset dir="src/main/resources/deb/control"/>
        <filterset begintoken="[[" endtoken="]]">
          <filter token="version" value="${version}"/>
          <filter token="name" value="${ant.project.name}"/>
        </filterset>
      </copy>
      <deb destfile="jdeb.deb" control="${deb}/control">
        <data src="src/main/resources/deb/data" type="directory">
          <exclude name="**/.svn"/>
        </data>
      </deb>
    </target>
```

For cross platform builds it might be important to retain permissions,
ownerships and links. When you provide the original tar as input the meta data
will be kept intact gets included directly into the deb. You can apply simple
modifications like prefixing or stripping of paths though.

```xml
    <deb destfile="jdeb.deb" control="${deb}/control">
      <data src="src/release.tgz" type="archive">
        <mapper type="perm" strip="1" prefix="/somewhere/else"/>
        <exclude name="**/.svn"/>
      </data>
    </deb>
```

For more complex permission and ownership adjustments you can use a "ls"
mapper. It allows you to define permissions and ownerships in a text file and
even under Windows you will be able to build your debian package.

```xml
    <deb destfile="jdeb.deb" control="${deb}/control">
      <data src="src/release.tgz" type="archive">
        <mapper type="ls" src="mapping.txt" />
      </data>
    </deb>
```

The mapper will apply the output of an "ls -laR > mapping.txt" command
that should look like this

    ./trunk/target/test-classes/org/vafer/dependency:
    total 176
    drwxr-xr-x   23 tcurdt  tcurdt   782 Jun 25 03:48 .
    drwxr-xr-x    3 tcurdt  tcurdt   102 Jun 25 03:48 ..
    -rw-r--r--    1 tcurdt  tcurdt  2934 Jun 25 03:48 DependenciesTestCase.class
    -rw-r--r--    1 tcurdt  tcurdt  2176 Jun 25 03:48 WarTestCase.class
    drwxr-xr-x    4 tcurdt  tcurdt   136 Jun 25 03:48 classes

It's also possible to use a `fileset` or even a `tarfileset` to
specify the set of files to include with their permissions :

```xml
    <deb destfile="jdeb.deb" control="${deb}/control">
      <tarfileset dir="src/main/resources/deb/data"
               prefix="/somewhere/else"
             filemode="600"
             username="tcurdt"
                group="tcurdt"/>
    </deb>
```

Links can be added by specifying a `link` element:

```xml
    <deb destfile="jdeb.deb" control="${deb}/control">
      <link name="/usr/share/java/foo.jar" target="/usr/share/java/foo-1.0.jar"/>
    </deb>
```

Here are the supported attributes on the `link` element:

Attribute     | Description                                            | Required
------------- | ------------------------------------------------------ | -----------------------
name          | The path of the link                                   | Yes
target        | The target of the link                                 | Yes
symbolic      | The type of the link (`true`: symbolic, `false`: hard) | No; defaults to `true`
uid           | Numerical uid                                          | No; defaults to 0
gid           | Numerical gid                                          | No; defaults to 0
user          | User name                                              | No; defaults to "root"
group         | User group                                             | No; defaults to "root"
mode          | Permissions as octet                                   | No; deftauls to 777

## Changes file

In order to also create a changes file you will need to provide the input and
output of the changes. The input file is a much simpler file where you should
list your changes. Every change should be starting with the " * " and one line
only.

    * changes for the next release
    release distribution=staging, date=20:13 17.08.2007,version=1.4+r89114,urgency=low,by=Torsten Curdt <torsten@vafer.org>
    * debian changes support

When you do a release jdeb will add (or complete!) the release line and create
a debian format standard changes file for you. (Don't forget to commit changes
jdeb did to the file.) From Ant you have to call jdeb like this

```xml
    <deb destfile="jdeb.deb"
          control="${deb}/control"
        changesIn="changes.txt"
       changesOut="jdeb.changes">
      <data src="some/dir"/>
    </deb>
```

If you also provide a `changesSave` attribute the jdeb will add release
information to the original input and write out the new file.

```xml
    <deb destfile="jdeb.deb"
          control="${deb}/control"
        changesIn="changes.txt"
       changesOut="jdeb.changes"
      changesSave="changes.txt">
      <data src="some/dir"/>
    </deb>
```

## Signing changes

To have the changes be signed, make sure you have the
[BouncyCastle OpenPGP/BCPG jar](http://www.bouncycastle.org/latest_releases.html) in your
classpath (just copy it into the `$ANT_HOME/lib` folder - next to jdeb).
Then you can sign your changes file with:

```xml
    <deb destfile="jdeb.deb"
          control="${deb}/control"
        changesIn="changes.txt"
       changesOut="jdeb.changes"
              key="2E074D8F"
       passphrase="secret"
          keyring="/Users/tcurdt/.gnupg/secring.gpg">
      <data src="some/dir"/>
    </deb>
```

<b>Security Note</b>: Hard coding the passphrase in the `<deb>` task can be a serious
security hole. Consider using variable substitution and asking the passphrase
to the user with the `<input>` task, or retrieving it from a secured `.properties` file.

## Conffiles file

If you package up a directory as Debian package you can also add the affected files to
the conffiles file. You only have to set the `conffile` attribute to `true`.

```xml
    <target name="package">
      <taskdef name="deb" classname="org.vafer.jdeb.ant.DebAntTask"/>
      <deb destfile="jdeb.deb" control="${deb}/control">
        <data src="src/main/resources/deb/data" type="directory" conffile="true">
          <exclude name="**/.svn"/>
        </data>
      </deb>
    </target>
```

## Reproducible builds

Starting with version 1.9, the jdeb supports reproducible builds. You can use `SOURCE_DATE_EPOCH`
environment variable, containing int representing seconds since the epoch.