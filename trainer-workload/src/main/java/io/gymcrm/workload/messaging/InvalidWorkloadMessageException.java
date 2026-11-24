package io.gymcrm.workload.messaging;

/**
 * A message that can never be processed (not JSON, or required information missing). Retrying won't help,
 * so it goes straight to the dead letter queue; the message explains why.
 */
public class InvalidWorkloadMessageException extends RuntimeException {

    public InvalidWorkloadMessageException(String reason) {
        super(reason);
    }

    public InvalidWorkloadMessageException(String reason, Throwable cause) {
        super(reason, cause);
    }
}
