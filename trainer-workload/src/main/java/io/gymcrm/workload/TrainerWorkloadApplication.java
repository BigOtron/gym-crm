package io.gymcrm.workload;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Keeps each trainer's training hours per month. Gym CRM reports every added or deleted training through
 * an ActiveMQ queue, and reads the result over REST.
 */
@SpringBootApplication
public class TrainerWorkloadApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrainerWorkloadApplication.class, args);
    }
}
