/*
 * Copyright 2014 The jdeb developers.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.vafer.jdeb.maven;

import org.apache.maven.plugin.logging.Log;
import org.vafer.jdeb.Console;

/**
 * Console implementation for Maven plugins. debug messages are only displayed
 * when the <tt>verbose</tt> parameter is true.
 */
class MojoConsole implements Console {

    private final Log log;
    private final boolean verbose;

    public MojoConsole(Log log, boolean verbose) {
        this.log = log;
        this.verbose = verbose;
    }

    public void debug(String message) {
        if (verbose) {
            log.info(message);
        }
    }

    public void info(String message) {
        log.info(message);
    }

    public void warn(String message) {
        log.warn(message);
    }
}
