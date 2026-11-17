package io.gymcrm.workload.model;

import java.time.YearMonth;
import java.util.Collections;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

/**
 * One trainer's training minutes, grouped by year and then by month (1-12), both in ascending order.
 * Not thread-safe: the repository only ever changes a private copy and then swaps it in.
 */
public class TrainerWorkload {

    private final String username;
    private String firstName;
    private String lastName;
    private boolean active;
    private final NavigableMap<Integer, NavigableMap<Integer, Integer>> minutesByYear = new TreeMap<>();

    public TrainerWorkload(String username, String firstName, String lastName, boolean active) {
        this.username = Objects.requireNonNull(username, "username must not be null");
        updateTrainer(firstName, lastName, active);
    }

    /** The latest event always carries the trainer's current name and status. */
    public final void updateTrainer(String firstName, String lastName, boolean active) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.active = active;
    }

    public void addMinutes(YearMonth month, int minutes) {
        requirePositive(minutes);
        minutesByYear.computeIfAbsent(month.getYear(), year -> new TreeMap<>())
                .merge(month.getMonthValue(), minutes, Integer::sum);
    }

    /**
     * Removes up to {@code minutes} from the month and returns how many were actually removed: never more
     * than the month has, so the total can't become negative. Empty months and years are dropped.
     */
    public int removeMinutes(YearMonth month, int minutes) {
        requirePositive(minutes);
        NavigableMap<Integer, Integer> months = minutesByYear.get(month.getYear());
        if (months == null) {
            return 0;
        }
        int current = months.getOrDefault(month.getMonthValue(), 0);
        int removed = Math.min(current, minutes);
        if (current - removed == 0) {
            months.remove(month.getMonthValue());
            if (months.isEmpty()) {
                minutesByYear.remove(month.getYear());
            }
        } else {
            months.put(month.getMonthValue(), current - removed);
        }
        return removed;
    }

    public int minutesIn(YearMonth month) {
        NavigableMap<Integer, Integer> months = minutesByYear.get(month.getYear());
        return months == null ? 0 : months.getOrDefault(month.getMonthValue(), 0);
    }

    /** year → (month → minutes), read-only. */
    public NavigableMap<Integer, NavigableMap<Integer, Integer>> getMinutesByYear() {
        NavigableMap<Integer, NavigableMap<Integer, Integer>> view = new TreeMap<>();
        minutesByYear.forEach((year, months) -> view.put(year, Collections.unmodifiableNavigableMap(months)));
        return Collections.unmodifiableNavigableMap(view);
    }

    /** A deep copy, so callers can't change the stored data and readers never see a half-made change. */
    public TrainerWorkload copy() {
        TrainerWorkload copy = new TrainerWorkload(username, firstName, lastName, active);
        minutesByYear.forEach((year, months) -> copy.minutesByYear.put(year, new TreeMap<>(months)));
        return copy;
    }

    public String getUsername() {
        return username;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public boolean isActive() {
        return active;
    }

    private static void requirePositive(int minutes) {
        if (minutes <= 0) {
            throw new IllegalArgumentException("Training duration must be positive");
        }
    }
}
