package com.sakurastore.backend.infrastructure.persistence.adapter;

import com.sakurastore.backend.application.dto.*;
import com.sakurastore.backend.domain.exception.DomainException;
import com.sakurastore.backend.domain.model.RoleEnum;
import com.sakurastore.backend.infrastructure.persistence.entity.UserJpaEntity;
import com.sakurastore.backend.infrastructure.persistence.mapper.UserPersistenceMapper;
import com.sakurastore.backend.infrastructure.persistence.repository.*;
import jakarta.persistence.criteria.Predicate;
import java.util.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class JpaUserSearch {
  private final SpringDataUserRepository users;

  public JpaUserSearch(SpringDataUserRepository users) {
    this.users = users;
  }

  public PageResponse<UserResponseDto> search(
      Long id,
      String username,
      String fullName,
      String email,
      String role,
      Boolean active,
      int page,
      int size,
      String sort) {
    return search(id, username, fullName, email, role, active, page, size, sort, null);
  }

  public PageResponse<UserResponseDto> search(
      Long id, String username, String fullName, String email, String role,
      Boolean active, int page, int size, String sort, Long roleId) {
    RoleEnum selected = null;
    if (QuerySupport.supplied(role)) {
      try {
        selected = RoleEnum.valueOf(role.toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException e) {
        throw new DomainException("Rol inválido.");
      }
    }
    final RoleEnum roleFilter = selected;
    Specification<UserJpaEntity> filter =
        (root, query, cb) -> {
          var predicates = new ArrayList<Predicate>();
          if (id != null) predicates.add(cb.equal(root.get("id"), id));
          if (QuerySupport.supplied(username))
            predicates.add(
                cb.like(cb.lower(root.get("username")), QuerySupport.contains(username), '\\'));
          if (QuerySupport.supplied(fullName)) {
            var name =
                cb.concat(
                    cb.concat(root.get("nombre"), " "), cb.coalesce(root.get("apellido"), ""));
            predicates.add(cb.like(cb.lower(name), QuerySupport.contains(fullName), '\\'));
          }
          if (QuerySupport.supplied(email))
            predicates.add(
                cb.like(cb.lower(root.get("email")), QuerySupport.contains(email), '\\'));
          if (roleFilter != null)
            predicates.add(cb.equal(root.get("role").get("name"), roleFilter));
          if (roleId != null) predicates.add(cb.equal(root.get("role").get("id"), roleId));
          if (active != null) predicates.add(cb.equal(root.get("active"), active));
          return cb.and(predicates.toArray(Predicate[]::new));
        };
    var paging =
        QuerySupport.page(
            page,
            size,
            sort,
            Map.of(
                "id",
                "id",
                "username",
                "username",
                "fullName",
                "nombre",
                "email",
                "email",
                "role",
                "role.name",
                "active",
                "active",
                "createdAt",
                "createdAt"));
    return PageResponse.from(
        users.findAll(filter, paging),
        e -> UserResponseDto.fromDomain(UserPersistenceMapper.toDomainUser(e)));
  }
}
