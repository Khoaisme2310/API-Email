package com.example.demo.repository;

import com.example.demo.entity.PgpKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PgpKeyRepository extends JpaRepository<PgpKeyEntity, Long> {
    Optional<PgpKeyEntity> findByEmail(String email);
    boolean existsByEmail(String email);
}