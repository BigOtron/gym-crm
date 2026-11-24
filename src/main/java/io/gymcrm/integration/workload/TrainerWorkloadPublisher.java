package io.gymcrm.integration.workload;

/**
 * Tells the Trainer Workload service about added and deleted trainings, asynchronously.
 *
 * @see JmsTrainerWorkloadPublisher
 */
public interface TrainerWorkloadPublisher {

    /**
     * Sends the event to the workload queue. Inside a transaction it is sent only if the transaction
     * commits; the workload service processes it later, even if it is down right now.
     *
     * @throws io.gymcrm.exceptions.ServiceUnavailableException if the message broker can't be reached
     */
    void publish(TrainerWorkloadEvent event);
}
