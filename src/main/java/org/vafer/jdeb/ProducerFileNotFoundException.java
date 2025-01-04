package org.vafer.jdeb;

import java.io.IOException;

public class ProducerFileNotFoundException extends IOException {
    
    public ProducerFileNotFoundException() {
        super();
    }

    public ProducerFileNotFoundException(String message) {
        super(message);
    }

    public ProducerFileNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
