[![Build Status](https://img.shields.io/github/actions/workflow/status/tcurdt/jdeb/ci.yml?style=for-the-badge)](https://github.com/tcurdt/jdeb/actions)
[![Coverage Status](https://img.shields.io/codecov/c/github/tcurdt/jdeb/master?style=for-the-badge)](https://codecov.io/gh/tcurdt/jdeb)
[![Maven Central](https://img.shields.io/maven-central/v/org.vafer/jdeb.svg?style=for-the-badge&maxAge=86400)](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22org.vafer%22%20AND%20a%3A%22jdeb%22)

# Debian packages in Java

This library provides an Ant task and a Maven plugin to create Debian packages
from Java builds in a truly cross platform manner. Build your Debian packages
on any platform that has Java support. Windows, Linux, OS X - it doesn't require
additional native tools installed.

Check the documentation on how to use it with [Maven](http://github.com/tcurdt/jdeb/blob/master/docs/maven.md)
or [Ant](http://github.com/tcurdt/jdeb/blob/master/docs/ant.md). Especially don't forget to check out the
[examples](http://github.com/tcurdt/jdeb/blob/master/src/examples/). Current
[javadocs](http://tcurdt.github.io/jdeb/apidocs/) and a source
[xref](http://tcurdt.github.io/jdeb/xref/) is also available.


## Where to get it

The jars are available in the [Maven central repository](https://repo1.maven.org/maven2/org/vafer/jdeb/).

If you feel adventurous or want to help out feel free to get the latest code
[via git](http://github.com/tcurdt/jdeb/tree/master).

    git clone git://github.com/tcurdt/jdeb.git

## Where to ask questions

[via git issue](https://github.com/tcurdt/jdeb/issues)

## Working towards jdeb 2.0

Over the years jdeb has grown more and more powerful - but also got more complex. In order to simplify the usage and apply what we have learned we are taking a step back. If you want to help to shape the usage of jdeb 2.0 feel free to contribute to the [planning](https://github.com/tcurdt/jdeb/issues/195). I think it still could be worth but this needs a sponsor.

## Related projects

Some links to other cross platform tools to package Linux applications:
* [apt-repo](https://github.com/theoweiss/apt-repo)
* [ant-deb-task](http://code.google.com/p/ant-deb-task)
* [jRPM](http://jrpm.sourceforge.net)
* [Install-Toolkit](http://install-toolkit.sourceforge.net)
