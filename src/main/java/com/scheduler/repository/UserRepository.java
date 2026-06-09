package com.scheduler.repository;

import com.scheduler.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Spring Data JPA derives the query from the method name
    Optional<User> findByUsername(String username);

    // Useful for your requirement to manage active/inactive users
    List<User> findByActive(boolean active);

    // Useful for filtering by project
    List<User> findByProject(String project);
}