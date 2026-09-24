package com.ems.dao;

import com.ems.entity.User;

import java.util.Optional;

public interface UserDao {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    User save(User user);
}
