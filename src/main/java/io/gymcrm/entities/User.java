package io.gymcrm.entities;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.NoArgsConstructor;

import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@ToString
@NoArgsConstructor
public abstract class User {
    private UUID userId;
    private String firstName;
    private String lastName;
    private String username;
    @ToString.Exclude
    private String password;
    private boolean active;

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User other = (User) o;
        return userId != null && userId.equals(other.userId);
    }

    @Override
    public final int hashCode() {
        return Objects.hashCode(userId);
    }
}
