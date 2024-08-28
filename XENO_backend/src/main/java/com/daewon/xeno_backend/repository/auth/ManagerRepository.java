package com.daewon.xeno_backend.repository.auth;

import com.daewon.xeno_backend.domain.auth.Manager;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ManagerRepository extends JpaRepository<Manager, Long> {
}
