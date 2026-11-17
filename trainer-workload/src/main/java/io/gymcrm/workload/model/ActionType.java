package io.gymcrm.workload.model;

/** What happened to a training in Gym CRM. */
public enum ActionType {
    /** A training was added: its duration is added to the trainer's month. */
    ADD,
    /** A training was deleted: its duration is removed from the trainer's month. */
    DELETE
}
