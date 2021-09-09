package net.apmoller.crb.ohp.microservices.exception;

/**
 * Exception class to be thrown when unexpected error encountered
 */
public class InternalServerException extends RuntimeException {
    public InternalServerException(String e) {
        super(e);
    }
}
