package com.example.Oboe.aspect;

import com.example.Oboe.Config.CustomUserDetails;
import com.example.Oboe.Entity.AccountType;
import com.example.Oboe.Entity.User;
import com.example.Oboe.Exception.AccessDeniedException;
import com.example.Oboe.Repository.UserRepository;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;

@Aspect
@Component
public class PremiumAspect {

    private final UserRepository userRepository;

    public PremiumAspect(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Before("@annotation(com.example.Oboe.annotation.PremiumOnly)")
    public void checkPremiumUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Không thể xác định tài khoản.");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails userDetails) {
            if (userDetails.getAccountType() != AccountType.PREMIUM) {
                throw new AccessDeniedException("Bạn cần nâng cấp tài khoản Premium để sử dụng chức năng này.");
            }
            return;
        }

        String username = authentication.getName();
        if (username == null || username.isBlank() || "anonymousUser".equalsIgnoreCase(username)) {
            throw new AccessDeniedException("Không thể xác định tài khoản.");
        }

        List<User> users = userRepository.findAllByExactUserName(username);
        if (users.isEmpty()) {
            throw new AccessDeniedException("Không thể xác định tài khoản.");
        }

        boolean hasPremium = users.stream()
                .anyMatch(user -> user.getAccountType() == AccountType.PREMIUM);

        if (!hasPremium) {
            throw new AccessDeniedException("Bạn cần nâng cấp tài khoản Premium để sử dụng chức năng này.");
        }
    }
}
