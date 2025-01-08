package org.figuramc.figura.util;

public abstract class FiguraException extends Exception {
    // Whether this exception should report its cause
    public abstract boolean showCause();
}
