/*
 * Copyright 2015 The jdeb developers.
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

package org.vafer.jdeb.ant;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.vafer.jdeb.Console;

/**
 * Console implementation for Ant tasks. debug messages are only displayed
 * when the <tt>verbose</tt> parameter is true.
 */
class TaskConsole implements Console {

    private final Task task;
    private final boolean verbose;

    public TaskConsole(Task task, boolean verbose) {
        this.task = task;
        this.verbose = verbose;
    }

    @Override
    public void debug(String message) {
        if (verbose) {
            task.log(message);
        }
    }

    @Override
    public void info(String message) {
        task.log(message);
    }

    @Override
    public void warn(String message) {
        task.log(message, Project.MSG_WARN);
    }

}
