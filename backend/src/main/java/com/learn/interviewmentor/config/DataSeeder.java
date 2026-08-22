package com.learn.interviewmentor.config;

import com.learn.interviewmentor.model.InterviewRequest;
import com.learn.interviewmentor.model.MentorProfile;
import com.learn.interviewmentor.model.Role;
import com.learn.interviewmentor.model.User;
import com.learn.interviewmentor.repository.InterviewRequestRepository;
import com.learn.interviewmentor.repository.MentorProfileRepository;
import com.learn.interviewmentor.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Creates demo accounts on first start so you have something to log in with.
 *
 * This is also the ONLY place an ADMIN account gets created - there is no public
 * "sign up as admin" endpoint, because that would let anyone grant themselves
 * full access. In a real system you would seed one admin and have them promote
 * others.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    /** Demo only. Obviously never ship a hardcoded password like this. */
    private static final String DEMO_PASSWORD = "password123";

    private final UserRepository userRepository;
    private final MentorProfileRepository mentorProfileRepository;
    private final InterviewRequestRepository requestRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository,
                      MentorProfileRepository mentorProfileRepository,
                      InterviewRequestRepository requestRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.mentorProfileRepository = mentorProfileRepository;
        this.requestRepository = requestRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        userRepository.save(user("Admin", "admin@example.com", Role.ADMIN));

        User ananya = userRepository.save(user("Ananya Rao", "ananya@example.com", Role.MENTOR));
        User vikram = userRepository.save(user("Vikram Shetty", "vikram@example.com", Role.MENTOR));
        User neha = userRepository.save(user("Neha Gupta", "neha@example.com", Role.MENTOR));

        mentorProfileRepository.saveAll(List.of(
                new MentorProfile(ananya, "Java, Spring Boot, System Design", 9, "Flipkart",
                        "Backend engineer. Happy to go deep on JPA and API design."),
                new MentorProfile(vikram, "React, Frontend Architecture, JavaScript", 7, "Razorpay",
                        "Frontend lead. Ask me about hooks, state and performance."),
                new MentorProfile(neha, "DSA, Problem Solving, Interview Prep", 11, "Amazon",
                        "Have taken 200+ interviews. Expect real interview pressure.")
        ));

        // One mentor who has signed up but not filled in their profile yet, so
        // the admin verification queue isn't empty the first time you look.
        User arjun = userRepository.save(user("Arjun Nair", "arjun@example.com", Role.MENTOR));
        mentorProfileRepository.save(new MentorProfile(arjun));

        User rahul = userRepository.save(user("Rahul Sharma", "rahul@example.com", Role.STUDENT));
        userRepository.save(user("Priya Menon", "priya@example.com", Role.STUDENT));

        InterviewRequest seeded = new InterviewRequest(
                rahul,
                "Spring Boot backend round",
                "Fresher",
                LocalDateTime.of(LocalDate.now().plusDays(3), java.time.LocalTime.of(15, 0)),
                "Final year student. Want to practise JPA and REST API questions.");
        seeded.markPaid(); // demo data: treat it as already paid for
        requestRepository.save(seeded);

        log.info("Seeded demo accounts. Every account uses the password: {}", DEMO_PASSWORD);
        log.info("arjun@example.com is a MENTOR with an INCOMPLETE profile - use it to try onboarding.");
    }

    private User user(String name, String email, Role role) {
        return new User(name, email, passwordEncoder.encode(DEMO_PASSWORD), role);
    }
}
