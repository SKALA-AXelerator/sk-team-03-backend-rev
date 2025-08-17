package com.skala03.skala_backend.repository.auth;

import com.skala03.skala_backend.entity.auth.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String>{

    Optional<User> findByUserEmail(String userEmail);

    boolean existsByUserEmail(String userEmail);
}