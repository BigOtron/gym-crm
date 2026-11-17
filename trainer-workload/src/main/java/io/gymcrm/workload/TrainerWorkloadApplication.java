package io.gymcrm.workload;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Keeps each trainer's training hours per month. Gym CRM reports every added or deleted training here.
 */
@SpringBootApplication
public class TrainerWorkloadApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrainerWorkloadApplication.class, args);
    }
}
