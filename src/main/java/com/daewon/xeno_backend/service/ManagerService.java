package com.daewon.xeno_backend.service;

import com.daewon.xeno_backend.domain.auth.Level;
import com.daewon.xeno_backend.domain.auth.UserRole;
import com.daewon.xeno_backend.dto.manager.BrandListDTO;
import com.daewon.xeno_backend.dto.manager.PointUpdateDTO;
import com.daewon.xeno_backend.dto.manager.ProductListDTO;
import com.daewon.xeno_backend.dto.manager.UserListDTO;
import com.daewon.xeno_backend.exception.UnauthorizedException;
import com.daewon.xeno_backend.exception.UserNotFoundException;

import java.util.List;
import java.util.Set;

public interface ManagerService {

    // user 강제 탈퇴
    String deleteUserByManager(String managerEmail, Long userIdToDelete) throws UserNotFoundException, UnauthorizedException;

    // user의 Role 변경
    void updateUserRoleByManager(String managerEmail, Long userId, Set<UserRole> newRoles) throws UserNotFoundException, UnauthorizedException;

    // user의 point(적립금) 조정
    void updateUserPointByManager(String managerEmail, Long userId, int newPoint) throws UserNotFoundException, UnauthorizedException;

    // user의 level(등급) 수정
    void updateUserLevelByManager(String managerEmail, Long userId, Level newLevel) throws UserNotFoundException, UnauthorizedException;

    // brand 강제 탈퇴
    String deleteBrandByManager(String managerEmail, Long brandIdToDelete, Long userIdToDelete) throws UserNotFoundException, UnauthorizedException;

    // 상품 강제 삭제
    String deleteProductByManager(String managerEmail, Long productIdToDelete) throws UserNotFoundException, UnauthorizedException;

    // user list 불러오기
    List<UserListDTO> getAllUsers();

    // brand list 불러오기
    List<BrandListDTO> getAllBrands();

    // product list 불러오기
    List<ProductListDTO> getAllProducts();
}
