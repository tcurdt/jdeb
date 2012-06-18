package org.vafer.jdeb.maven;

import org.apache.maven.plugin.logging.Log;
import org.vafer.jdeb.Console;

class MojoConsole implements Console {

    private final Log log;

    MojoConsole( Log log ) {
        this.log = log;
    }

    public void info( String s ) {
        log.info(s);
    }

    public void warn( String s ) {
        log.warn(s);
    }
    
}
