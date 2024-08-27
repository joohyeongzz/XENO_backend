package com.daewon.xeno_backend.repository;

import com.daewon.xeno_backend.domain.auth.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

}
