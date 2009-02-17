package org.vafer.jdeb.descriptors;

public final class InvalidDescriptorException extends Exception {

    private static final long serialVersionUID = 1L;
    private final AbstractDescriptor desc;

    public InvalidDescriptorException(AbstractDescriptor desc) {
        this.desc = desc;
    }

    public InvalidDescriptorException(AbstractDescriptor desc, String message) {
        super(message);
        this.desc = desc;
    }

    public InvalidDescriptorException(AbstractDescriptor desc, Throwable cause) {
        super(cause);
        this.desc = desc;
    }

    public InvalidDescriptorException(AbstractDescriptor desc, String message, Throwable cause) {
        super(message, cause);
        this.desc = desc;
    }

    public AbstractDescriptor getDescriptor() {
        return desc;
    }

    public String toString() {
        return "Invalid keys are " + desc.invalidKeys() + "\n" + desc; 
    }

    
}
