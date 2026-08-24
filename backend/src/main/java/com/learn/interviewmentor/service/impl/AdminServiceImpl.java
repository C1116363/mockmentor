package com.learn.interviewmentor.service.impl;

import com.learn.interviewmentor.service.AdminService;
import com.learn.interviewmentor.service.InterviewRequestService;
import com.learn.interviewmentor.service.PaymentService;
import com.learn.interviewmentor.service.PlanEnrollmentService;
import com.learn.interviewmentor.service.PlanService;
import com.learn.interviewmentor.service.StudyMaterialService;

import com.learn.interviewmentor.vo.auth.UserVo;
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
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final InterviewRequestService requestService;
    private final PaymentService paymentService;
    private final PlanService planService;
    private final PlanEnrollmentService enrollmentService;
    private final StudyMaterialService materialService;

    public AdminServiceImpl(UserRepository userRepository,
                        InterviewRequestService requestService,
                        PaymentService paymentService,
                        PlanService planService,
                        PlanEnrollmentService enrollmentService,
                        StudyMaterialService materialService) {
        this.userRepository = userRepository;
        this.requestService = requestService;
        this.paymentService = paymentService;
        this.planService = planService;
        this.enrollmentService = enrollmentService;
        this.materialService = materialService;
    }

    @Override
    public List<UserVo> findAllUsers() {
        return userRepository.findAll().stream()
                .map(UserVo::from)
                .toList();
    }

    /** Numbers for the admin dashboard tiles. */
    @Override
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
        stats.put("activePlans", planService.activeCount());
        stats.put("planPaymentsToCheck", enrollmentService.countAwaitingReview());
        stats.put("activeEnrollments", enrollmentService.countActive());
        stats.put("materials", materialService.activeCount());
        return stats;
    }

    /** Block or unblock an account. A blocked user cannot log in. */
    @Transactional
    @Override
    public UserVo setActive(Long userId, boolean active, User admin) {
        if (userId.equals(admin.getId())) {
            throw new BadRequestException("You cannot deactivate your own admin account");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with id " + userId));

        user.setActive(active);
        return UserVo.from(user);
    }
}
