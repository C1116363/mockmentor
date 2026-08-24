package com.learn.interviewmentor.service;

import com.learn.interviewmentor.vo.auth.UserVo;
import com.learn.interviewmentor.exception.BadRequestException;
import com.learn.interviewmentor.exception.NotFoundException;
import com.learn.interviewmentor.model.RequestStatus;
import com.learn.interviewmentor.model.Role;
import com.learn.interviewmentor.model.User;
import com.learn.interviewmentor.repository.UserRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Everything only an ADMIN can do. */
public interface AdminService {

    List<UserVo> findAllUsers();

    /** Numbers for the admin dashboard tiles. */
    Map<String, Long> stats();

    /** Block or unblock an account. A blocked user cannot log in. */
    UserVo setActive(Long userId, boolean active, User admin);
}
