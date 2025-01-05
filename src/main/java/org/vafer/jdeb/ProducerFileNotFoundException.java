package org.vafer.jdeb;

import java.io.FileNotFoundException;

public class ProducerFileNotFoundException extends FileNotFoundException {

    private String filePath;

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

    public void setFilePath(String path) {
        this.filePath = path;
    }

    public String getFilePath() {
        return this.filePath;
    }
}
