package com.sakurastore.backend.infrastructure.persistence.adapter;

import com.sakurastore.backend.domain.model.User;
import com.sakurastore.backend.domain.port.UserRepositoryPort;
import com.sakurastore.backend.infrastructure.persistence.entity.UserJpaEntity;
import com.sakurastore.backend.infrastructure.persistence.mapper.UserPersistenceMapper;
import com.sakurastore.backend.infrastructure.persistence.repository.SpringDataUserRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class UserRepositoryAdapter implements UserRepositoryPort {
    private final SpringDataUserRepository springDataUserRepository;

    public UserRepositoryAdapter(SpringDataUserRepository springDataUserRepository) {
        this.springDataUserRepository = springDataUserRepository;
    }

    @Override
    public User save(User user) {
        UserJpaEntity entity = UserPersistenceMapper.toJpaUser(user);
        UserJpaEntity saved = springDataUserRepository.save(entity);
        return UserPersistenceMapper.toDomainUser(saved);
    }

    @Override
    public Optional<User> findById(Long id) {
        return springDataUserRepository.findById(id)
                .map(UserPersistenceMapper::toDomainUser);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return springDataUserRepository.findByUsername(username)
                .map(UserPersistenceMapper::toDomainUser);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return springDataUserRepository.findByEmail(email)
                .map(UserPersistenceMapper::toDomainUser);
    }

    @Override
    public List<User> findAll() {
        return springDataUserRepository.findAll().stream()
                .map(UserPersistenceMapper::toDomainUser)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByUsername(String username) {
        return springDataUserRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return springDataUserRepository.existsByEmail(email);
    }

    @Override
    public boolean existsById(Long id) {
        return springDataUserRepository.existsById(id);
    }

    @Override
    public void deleteById(Long id) {
        springDataUserRepository.deleteById(id);
    }
}
