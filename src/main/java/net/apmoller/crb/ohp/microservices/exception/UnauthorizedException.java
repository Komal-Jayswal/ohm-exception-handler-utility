package net.apmoller.crb.ohp.microservices.exception;

/**
 * Exception class to be thrown when client is not authenticated
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String e) {
        super(e);
    }

}
