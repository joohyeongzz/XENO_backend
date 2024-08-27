package com.daewon.xeno_backend.service;

import com.daewon.xeno_backend.domain.auth.Level;
import com.daewon.xeno_backend.domain.auth.UserRole;
import com.daewon.xeno_backend.domain.auth.Users;
import com.daewon.xeno_backend.exception.UnauthorizedException;
import com.daewon.xeno_backend.exception.UserNotFoundException;
import com.daewon.xeno_backend.repository.auth.CustomerRepository;
import com.daewon.xeno_backend.repository.auth.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Set;

@Service
@Log4j2
@RequiredArgsConstructor
public class ManagerServiceImpl implements ManagerService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;

    @Transactional
    @Override
    public String deleteUserByManager(String managerEmail, Long userIdToDelete) throws UserNotFoundException, UnauthorizedException {
        // Manager가 맞는지 검증 시도
        Users manager = validateManager(managerEmail);
        Users userToDelete = getUserById(userIdToDelete);

        log.info("현재 " + manager + "가 " + userToDelete + "user계정을 삭제합니다.");

        deleteUserData(userToDelete);

        log.info("UserId {} 가 manager {}의해서 회원 탈퇴가 완료되었습니다.", userIdToDelete, managerEmail);
        return managerEmail;
    }

    @Transactional
    @Override
    public void updateUserRoleByManager(String managerEmail, Long userId, Set<UserRole> newRoles) throws UserNotFoundException, UnauthorizedException {
        // Manager가 맞는지 검증
        Users manager = validateManager(managerEmail);
        Users user = getUserById(userId);

        log.info("Manager {} 가 해당 userId {} 의 role값을 변경중입니다.", managerEmail, userId);
        user.setRoleSet(newRoles);
        userRepository.save(user);
        log.info("userId {} 의 role값이 Manager {} 의하여 변경 됐습니다.", userId, managerEmail);
    }

    @Override
    public void updateUserPointByManager(String managerEmail, Long userId, int pointsChange) throws UserNotFoundException, UnauthorizedException {

    }

    @Override
    public void updateUserLevelByManager(String managerEmail, Long userId, Level newLevel) throws UserNotFoundException, UnauthorizedException {

    }

    // Manager인지 검증하는 메서드
    private Users validateManager(String managerEmail) throws UserNotFoundException, UnauthorizedException {
        Users manager = userRepository.findByEmail(managerEmail)
                .orElseThrow(() -> new UserNotFoundException("Manager의 email을 찾을 수 없음 : " + managerEmail));

        if (manager.getManager() == null) {
            throw new UnauthorizedException("현재 사용자는 이 작업을 수행할 권한이 없습니다.");
        }
        return manager;
    }

    // user의 id를 가져오는 메서드
    private Users getUserById(Long userId) throws UserNotFoundException {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User의 id를 찾을 수 없음: " + userId));
    }

    // userData를 삭제하는 메서드
    // 해당하는 user, customer값을 삭제함
    private void deleteUserData(Users user) {
        if (user.getCustomer() != null) {
            customerRepository.delete(user.getCustomer());
        }
        userRepository.delete(user);
    }
}
