package com.cristiannustes.data.user;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

import com.cristiannustes.domain.user.User;
import com.cristiannustes.domain.user.UserRepository;
import com.cristiannustes.http.error.ConflictException;

public final class InMemoryUserRepository implements UserRepository {

    private final Map<String, User> byId = new LinkedHashMap<>();
    private final Map<String, String> idByLogin = new HashMap<>();
    private final ReentrantLock lock = new ReentrantLock();

    @Override
    public User create(User user) {
        lock.lock();
        try {
            requireLoginAvailable(user.login(), user.id());
            byId.put(user.id(), user);
            idByLogin.put(user.login(), user.id());
            return user;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public User update(User user) {
        lock.lock();
        try {
            User existing = byId.get(user.id());
            if (existing == null) {
                throw new IllegalStateException("cannot update a user that does not exist: " + user.id());
            }
            requireLoginAvailable(user.login(), user.id());
            idByLogin.remove(existing.login());
            byId.put(user.id(), user);
            idByLogin.put(user.login(), user.id());
            return user;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Optional<User> findById(String id) {
        lock.lock();
        try {
            return Optional.ofNullable(byId.get(id));
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Optional<User> findByLogin(String login) {
        lock.lock();
        try {
            return Optional.ofNullable(idByLogin.get(login)).map(byId::get);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<User> findAll() {
        lock.lock();
        try {
            return List.copyOf(byId.values());
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean deleteById(String id) {
        lock.lock();
        try {
            User removed = byId.remove(id);
            if (removed == null) {
                return false;
            }
            idByLogin.remove(removed.login());
            return true;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public long count() {
        lock.lock();
        try {
            return byId.size();
        } finally {
            lock.unlock();
        }
    }

    private void requireLoginAvailable(String login, String ownerId) {
        String currentOwner = idByLogin.get(login);
        if (currentOwner != null && !currentOwner.equals(ownerId)) {
            throw new ConflictException("login '%s' is already taken".formatted(login));
        }
    }
}
