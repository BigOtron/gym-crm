package io.gymcrm.util;

import io.gymcrm.dao.UserDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
public class UsernameGenerator {

    private UserDao userDao;

    @Autowired
    public void setUserDao(UserDao userDao) {
        this.userDao = userDao;
    }

    public String generate(String firstName, String lastName) {
        String base = firstName.trim() + "." + lastName.trim();
        Set<String> taken = userDao.findUsernamesStartingWith(base);

        if (!taken.contains(base)) {
            log.debug("Generated username {}", base);
            return base;
        }

        int serial = 1;
        while (taken.contains(base + serial)) {
            serial++;
        }
        String username = base + serial;
        log.debug("Username {} already taken, generated {}", base, username);
        return username;
    }
}
