package io.gymcrm.dao;

import io.gymcrm.entities.User;

import java.util.Optional;
import java.util.Set;

public interface UserDao {
    Optional<User> findByUsername(String username);

    Set<String> findUsernamesStartingWith(String prefix);
}
