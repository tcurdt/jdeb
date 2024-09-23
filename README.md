# jdeb - Debian Packages in Java

[![Build Status](https://img.shields.io/github/actions/workflow/status/tcurdt/jdeb/ci.yml?style=for-the-badge&logo=github)](https://github.com/tcurdt/jdeb/actions)
[![Coverage Status](https://img.shields.io/codecov/c/github/tcurdt/jdeb/master?style=for-the-badge&logo=codecov)](https://codecov.io/gh/tcurdt/jdeb)
[![Maven Central](https://img.shields.io/maven-central/v/org.vafer/jdeb.svg?style=for-the-badge&logo=apache-maven&maxAge=86400)](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22org.vafer%22%20AND%20a%3A%22jdeb%22)


# Debian Packages in Java

**jdeb** is a powerful library that enables you to create Debian packages from Java builds in a truly cross-platform manner. Build your Debian packages seamlessly on any platform that supports Java, including Windows, Linux, and macOS, without the need for additional native tools.

## Documentation

- Learn how to use **jdeb** with [Maven](http://github.com/tcurdt/jdeb/blob/master/docs/maven.md).
- Explore usage with [Ant](http://github.com/tcurdt/jdeb/blob/master/docs/ant.md).
- Don't forget to check out the [examples](http://github.com/tcurdt/jdeb/blob/master/src/examples/) for practical implementations.
- Access the current [Javadocs](http://tcurdt.github.io/jdeb/apidocs/) and source [xref](http://tcurdt.github.io/jdeb/xref/) for detailed API documentation.

## Getting Started

You can download the latest JAR files from the [Maven Central Repository](https://repo1.maven.org/maven2/org/vafer/jdeb/).

If you're looking to contribute or explore the latest developments, you can clone the repository via Git:


    git clone git://github.com/tcurdt/jdeb.git

## Questions and Support

If you have any questions or need assistance, please don't hesitate to reach out. You can submit your inquiries or issues through our [GitHub Issues](https://github.com/tcurdt/jdeb/issues) page. We're here to help!

---

## Working Towards jdeb 2.0

As **jdeb** has evolved, it has become increasingly powerful, but also more complex. To streamline its usage and implement the lessons we've learned, we are taking a step back to redesign **jdeb** for version 2.0.

We invite you to participate in shaping the future of **jdeb**! Your insights and contributions are invaluable. Join the discussion in our [planning issue](https://github.com/tcurdt/jdeb/issues/195). Together, we can make **jdeb** even better, but we will need sponsorship to support this effort.

---

## Related Projects

Here are some useful cross-platform tools for packaging Linux applications:

- [apt-repo](https://github.com/theoweiss/apt-repo): A tool for managing APT repositories.
- [ant-deb-task](http://code.google.com/p/ant-deb-task): An Ant task for building Debian packages.
- [jRPM](http://jrpm.sourceforge.net): A Java tool for creating RPM packages.
- [Install-Toolkit](http://install-toolkit.sourceforge.net): A toolkit for installation package creation.

Explore these resources to enhance your packaging workflows!
