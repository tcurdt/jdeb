package org.vafer.jdeb.ant;

import org.apache.tools.ant.Task;
import org.vafer.jdeb.Console;

class TaskConsole implements Console {

    private final Task task;
    private final boolean verbose;

    TaskConsole( Task task, boolean verbose ) {
        this.task = task;
        this.verbose = verbose;
    }

    @Override
    public void info( String message ) {
        if (verbose) {
            task.log(message);
        }
    }

    @Override
    public void warn( String message ) {
        task.log(message);
    }

}
