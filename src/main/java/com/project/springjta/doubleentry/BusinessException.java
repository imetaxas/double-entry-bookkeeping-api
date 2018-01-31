package com.project.springjta.doubleentry;

/**
 * Base type for recoverable business exceptions.
 */
public abstract class BusinessException extends RuntimeException {
    private static final long serialVersionUID = 1L;

	protected BusinessException(String message) {
        super(message);
    }
}
