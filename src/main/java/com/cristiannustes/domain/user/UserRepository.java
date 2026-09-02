package com.cristiannustes.domain.user;

import java.util.List;
import java.util.Optional;

public interface UserRepository {

    User create(User user);

    User update(User user);

    Optional<User> findById(String id);

    Optional<User> findByLogin(String login);

    List<User> findAll();

    boolean deleteById(String id);

    long count();
}
