package com.skala03.skala_backend.repository;

import com.skala03.skala_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String>{

    Optional<User> findByUserEmail(String userEmail);

    boolean existsByUserEmail(String userEmail);
}