package com.daewon.xeno_backend.repository;

import com.daewon.xeno_backend.domain.Orders;
import com.daewon.xeno_backend.domain.Review;
import com.daewon.xeno_backend.domain.UploadImage;
import com.daewon.xeno_backend.domain.auth.Users;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UploadImageRepository extends JpaRepository<UploadImage, Long> {



}