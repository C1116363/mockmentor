package com.learn.interviewmentor.service;

import com.learn.interviewmentor.dto.auth.UserDto;
import com.learn.interviewmentor.exception.BadRequestException;
import com.learn.interviewmentor.exception.NotFoundException;
import com.learn.interviewmentor.model.RequestStatus;
import com.learn.interviewmentor.model.Role;
import com.learn.interviewmentor.model.User;
import com.learn.interviewmentor.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Everything only an ADMIN can do. */
@Service
@Transactional(readOnly = true)
public class AdminService {

    private final UserRepository userRepository;
    private final InterviewRequestService requestService;
    private final PaymentService paymentService;

    public AdminService(UserRepository userRepository,
                        InterviewRequestService requestService,
                        PaymentService paymentService) {
        this.userRepository = userRepository;
        this.requestService = requestService;
        this.paymentService = paymentService;
    }

    public List<UserDto> findAllUsers() {
        return userRepository.findAll().stream()
                .map(UserDto::from)
                .toList();
    }

    /** Numbers for the admin dashboard tiles. */
    public Map<String, Long> stats() {
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("students", userRepository.countByRole(Role.STUDENT));
        stats.put("mentors", userRepository.countByRole(Role.MENTOR));
        stats.put("admins", userRepository.countByRole(Role.ADMIN));
        stats.put("totalRequests", requestService.countAll());
        stats.put("awaitingPayment", requestService.countByStatus(RequestStatus.AWAITING_PAYMENT));
        stats.put("paymentsToCheck", paymentService.countAwaitingReview());
        stats.put("pending", requestService.countByStatus(RequestStatus.PENDING));
        stats.put("scheduled", requestService.countByStatus(RequestStatus.SCHEDULED));
        stats.put("completed", requestService.countByStatus(RequestStatus.COMPLETED));
        stats.put("cancelled", requestService.countByStatus(RequestStatus.CANCELLED));
        return stats;
    }

    /** Block or unblock an account. A blocked user cannot log in. */
    @Transactional
    public UserDto setActive(Long userId, boolean active, User admin) {
        if (userId.equals(admin.getId())) {
            throw new BadRequestException("You cannot deactivate your own admin account");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with id " + userId));

        user.setActive(active);
        return UserDto.from(user);
    }
}
