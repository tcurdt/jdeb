[![Build Status](https://secure.travis-ci.org/tcurdt/jdeb.png)](http://travis-ci.org/tcurdt/jdeb)

# Debian packages in Java

This library provides an Ant task and a Maven plugin to create Debian packages
from Java builds in a truly cross platform manner. Build your Debian packages
on any platform that has Java support. Windows, Linux, OS X - it doesn't require
additional native tools installed. The API underneath is well abstracted and
can easily be adopted for other areas as well.

Check the documentation on how to use it with [Maven](http://github.com/tcurdt/jdeb/blob/master/docs/maven.md)
or [Ant](http://github.com/tcurdt/jdeb/blob/master/docs/ant.md). Especially don't forget to check out the
[examples](http://github.com/tcurdt/jdeb/blob/master/src/examples/). Current
[javadocs](http://tcurdt.github.com/jdeb/release/1.0.1/apidocs/) and a source
[xref](http://tcurdt.github.com/jdeb/release/1.0.1/xref/) is also available.


## Where to get it

The jars are available in the [Maven central repository](http://repo1.maven.org/maven2/org/vafer/jdeb/).

If feel adventures or want to help out feel free to get the latest code
[via git](http://github.com/tcurdt/jdeb/tree/master).

    git clone git://github.com/tcurdt/jdeb.git


## Related projects

Some links to other cross platform tools to package Linux applications:

* [ant-deb-task](http://code.google.com/p/ant-deb-task)
* [jRPM](http://jrpm.sourceforge.net)
* [RedLine](http://www.freecompany.org/redline)
* [Install-Toolkit](http://install-toolkit.sourceforge.net)
