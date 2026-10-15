package io.gymcrm.dao.implementations;

import io.gymcrm.dao.UserDao;
import io.gymcrm.entities.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Repository
public class UserDaoImpl implements UserDao {

    private EntityManager entityManager;

    @PersistenceContext
    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        Optional<User> user = entityManager.createQuery(
                        "select u from User u where u.username = :username", User.class)
                .setParameter("username", username)
                .getResultStream()
                .findFirst();
        log.debug("Lookup user by username {}: {}", username, user.isPresent() ? "found" : "not found");
        return user;
    }

    @Override
    public Set<String> findUsernamesStartingWith(String prefix) {
        String pattern = prefix.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_") + "%";
        Set<String> usernames = new HashSet<>(entityManager.createQuery(
                        "select u.username from User u where u.username like :pattern escape '\\'", String.class)
                .setParameter("pattern", pattern)
                .getResultList());
        log.debug("Found {} usernames starting with {}", usernames.size(), prefix);
        return usernames;
    }
}
