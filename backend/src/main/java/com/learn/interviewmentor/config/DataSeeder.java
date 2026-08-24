package com.learn.interviewmentor.config;

import com.learn.interviewmentor.model.InterviewRequest;
import com.learn.interviewmentor.model.MentorProfile;
import com.learn.interviewmentor.model.Plan;
import com.learn.interviewmentor.model.Role;
import com.learn.interviewmentor.model.SessionType;
import com.learn.interviewmentor.model.StudyMaterial;
import com.learn.interviewmentor.model.User;
import com.learn.interviewmentor.repository.InterviewRequestRepository;
import com.learn.interviewmentor.repository.MentorProfileRepository;
import com.learn.interviewmentor.repository.PlanRepository;
import com.learn.interviewmentor.repository.StudyMaterialRepository;
import com.learn.interviewmentor.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
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
    private final PlanRepository planRepository;
    private final StudyMaterialRepository materialRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository,
                      MentorProfileRepository mentorProfileRepository,
                      InterviewRequestRepository requestRepository,
                      PlanRepository planRepository,
                      StudyMaterialRepository materialRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.mentorProfileRepository = mentorProfileRepository;
        this.requestRepository = requestRepository;
        this.planRepository = planRepository;
        this.materialRepository = materialRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // Two independent guards, not one. Plans and study material were added
        // after the accounts were, so a database seeded before they existed
        // still needs them - a single "if any users, do nothing" check would
        // leave every existing install with an empty plans page.
        seedAccounts();
        seedPlans();
    }

    private void seedAccounts() {
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
                SessionType.MOCK_INTERVIEW,
                "Spring Boot backend round",
                "Fresher",
                LocalDateTime.of(LocalDate.now().plusDays(3), java.time.LocalTime.of(15, 0)),
                "Final year student. Want to practise JPA and REST API questions.");
        seeded.markPaid(); // demo data: treat it as already paid for
        requestRepository.save(seeded);

        // One of each kind, so the mentor queue shows both and it is obvious
        // from the first screen that a booking is not always an interview.
        InterviewRequest mentoring = new InterviewRequest(
                rahul,
                SessionType.MENTORING,
                "Which stack should I specialise in?",
                "Fresher",
                LocalDateTime.of(LocalDate.now().plusDays(4), java.time.LocalTime.of(11, 0)),
                "Not an interview - I just want to talk through backend vs frontend "
                        + "before I commit the next six months to one of them.");
        mentoring.markPaid();
        requestRepository.save(mentoring);

        log.info("Seeded demo accounts. Every account uses the password: {}", DEMO_PASSWORD);
        log.info("arjun@example.com is a MENTOR with an INCOMPLETE profile - use it to try onboarding.");
    }

    /**
     * The starting price list, so the plans page isn't empty on a first run.
     *
     * These are only defaults. The whole point of keeping prices in the database
     * is that an admin changes them from the admin panel, and once they have,
     * this method must never overwrite their numbers - hence the count check.
     */
    private void seedPlans() {
        if (planRepository.count() > 0) {
            return;
        }

        Plan placement = planRepository.save(new Plan(
                "Placement Guide",
                "Everything from resume to offer letter",
                "Our most complete track. A mentor walks you from a rewritten resume through "
                        + "mock interviews to negotiating the offer.",
                """
                Resume rewritten by a hiring manager
                4 mock interviews with feedback
                DSA sheet with weekly targets
                LinkedIn and referral strategy
                Salary negotiation walkthrough""",
                new BigDecimal("2999.00"), 120, 1, true));

        planRepository.save(new Plan(
                "Backend with Java & Spring Boot",
                "Learn it properly from someone who ships it",
                "Eight live sessions on Java, Spring Boot, JPA and REST design, built around a "
                        + "project you keep.",
                """
                8 live sessions with a working backend engineer
                Build and deploy one real REST API
                Code review on your own project
                JPA, security and testing covered properly""",
                new BigDecimal("4499.00"), 90, 2, false));

        planRepository.save(new Plan(
                "Frontend with React",
                "Hooks, state and performance, in depth",
                "Six sessions on modern React with a frontend lead - the parts tutorials skip.",
                """
                6 live sessions with a frontend lead
                Hooks and state management in depth
                Performance profiling on your own app
                Accessibility and testing basics""",
                new BigDecimal("3499.00"), 90, 3, false));

        planRepository.save(new Plan(
                "DSA Sprint",
                "Six weeks, one interview pattern at a time",
                "A structured run at data structures and algorithms, organised by the patterns "
                        + "interviewers actually ask about.",
                """
                Curated sheet, ordered by pattern
                Weekly targets and progress checks
                2 mock DSA interviews
                Written feedback after each mock""",
                new BigDecimal("1999.00"), 60, 4, false));

        // One piece of material for everyone, so the student page isn't empty
        // the first time somebody looks at it.
        userRepository.findByEmailIgnoreCase("admin@example.com").ifPresent(admin -> {
            StudyMaterial welcome = StudyMaterial.link(
                    "How to get the most out of a mock interview",
                    "Read this before your first session - it is the difference between an hour "
                            + "of practice and an hour of feedback you can act on.",
                    "https://www.youtube.com/results?search_query=mock+interview+preparation",
                    admin);
            welcome.sendToEveryone();
            materialRepository.save(welcome);

            StudyMaterial placementOnly = StudyMaterial.link(
                    "Placement Guide: week 1 checklist",
                    "Only visible to students on the Placement Guide plan.",
                    "https://example.com/placement-guide/week-1",
                    admin);
            placementOnly.sendToPlan(placement);
            materialRepository.save(placementOnly);
        });

        log.info("Seeded {} plans and their starter study material.", planRepository.count());
    }

    private User user(String name, String email, Role role) {
        return new User(name, email, passwordEncoder.encode(DEMO_PASSWORD), role);
    }
}
