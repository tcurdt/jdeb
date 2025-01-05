package org.vafer.jdeb;

import java.io.FileNotFoundException;

public class ProducerFileNotFoundException extends FileNotFoundException {


    public ProducerFileNotFoundException() {
        super();
    }

    public ProducerFileNotFoundException(String message) {
        super(message);
    }

    public ProducerFileNotFoundException(String message, Throwable cause) {
        super(message);
        this.initCause(cause);
    }
}
