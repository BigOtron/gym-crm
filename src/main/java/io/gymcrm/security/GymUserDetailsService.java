package io.gymcrm.security;

import io.gymcrm.dao.TraineeDao;
import io.gymcrm.dao.UserDao;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tells Spring Security how to load a user at login: the stored hash and the role.
 * Deactivated users can still log in, otherwise they could never activate themselves again.
 */
@Service
public class GymUserDetailsService implements UserDetailsService {

    private final UserDao userDao;
    private final TraineeDao traineeDao;

    public GymUserDetailsService(UserDao userDao, TraineeDao traineeDao) {
        this.userDao = userDao;
        this.traineeDao = traineeDao;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        io.gymcrm.entities.User user = userDao.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Unknown user"));
        Role role = traineeDao.findByUsername(username).isPresent() ? Role.TRAINEE : Role.TRAINER;
        return org.springframework.security.core.userdetails.User.withUsername(user.getUsername())
                .password(user.getPassword())
                .roles(role.name())
                .build();
    }
}
