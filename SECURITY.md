https://github.com/tcurdt/jdeb 
jdeb/src/examples/maven/pom.xml at master
 xml version="http://maven.apache.org/POM/4.0.0"
 xmlns:xsi="http://www.w3.org/2004/XMLSchema-instance"xsi:schemaLocation="http://maven.apache.org/POM/4.0.0http://maven.apache.org/maven-v4_0_0.xsd">
              <modelVersion>4.0.0</modelVersion>
              <groupid>org.varf</groupid>
              <artifactId>jdeb-example</artifactId>
              <version>1.0-SNAPSHOT</version>
              <description>description frompom</description>
              <properties>
               <project.build.sourceEncoding>UTF-8</project.build.
          sourceEncoding>
              <project.reporting.outputEncoding>UTF-8</project.
          reporting.outputEncoding>
          <maven.compiler.source>1.7</maven.compiler.target>
           </properties>
           <build>
             <plugins>
               <plugin>
                  <artifactId>jdeb</artifactId>
                  <groupId>org.vafer</groupId>
                  <version>1.11</version>
                  <executions>
                   <execution>
                    <phase>package</phase>
                    <goals>
                     <goal>jdeb</goal>
                  </goals>
                  <configuration>
                   <verbose>true</verbose>
                   <snapshotExpand>true</snapshotExpand>
                   <!--expand "SNAPSHOT"to what isin the
  "USER"env variable -->
                 <snapshotEnv>USER</snapshotEnv>
                 <controlIDir>${basedir}/src?
  deb/control</controlDir>
                <dataSet>

                   <data>
                    <src>${project.build.
  directory}/${project.build.finalName}.jar</src>
                       <type>file</type>
                       <mapper>
                        <type>perm</type>
                        <prefix>/usr/share/jdeb/lib</prefix>
                        <user>loader</user>
                        <users>loader</group>
                        <filemode>640</filemode>
                      </mapper>
                    </data>

                    <data>
                     <type>link</type>
                     <symlink>true</symlink>
                     <linkName>/usr/share/java/
jdeb.jar</linkName>
                  <linkTarget>lib/${project.
build.finalName}.jar</linkTarget>
                 </data>

                 <data>
                  <src>${basedir}/src/deb/init.d</src>
                  <type>directory</type>
                  <mapper>
                   <type>perm</type>
                   <mapper>
                    <type>perm</type>
                    <prefix>/etc/init.d</prefix>
                    <user>loader</user>
                    <group>loader</group>
                  </mapper>
                </data>

                <data>
                  <type>template</type>
                  <paths>
                   <path>etc/${project.artifactId}</path>
                   <path>var/lib/${project.artifactId}</path>
                   <path>var/log${project.artifactId}</path>
                   <path>var/run/${project.artifactId}</path>
                  </paths>
                  <mapper>
                   <type>>perm</type>
                   <user>loader</user>
                   <group>loader</group>
                   <filemode>750</filemode>
                  </mapper>
                </data>

              </dataSet>
            </configuration>
          </execution>
        </plugin>
      </plugins>
    </build>
  </project>
        
  
                  <controlIDir>${basedir}/src/
                    
# Security Policy

## Supported Versions

Use this section to tell people about which versions of your project are
currently being supported with security updates.

| Version | Supported          |
| ------- | ------------------ |
| 5.1.x   | :white_check_mark: |
| 5.0.x   | :x:                |
| 4.0.x   | :white_check_mark: |
| < 4.0   | :x:                |

## Reporting a Vulnerability

Use this section to tell people how to report a vulnerability.

Tell them where to go, how often they can expect to get an update on a
reported vulnerability, what to expect if the vulnerability is accepted or
declined, etc.
