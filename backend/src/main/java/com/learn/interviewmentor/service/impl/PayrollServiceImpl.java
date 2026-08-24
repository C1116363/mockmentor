package com.learn.interviewmentor.service.impl;

import com.learn.interviewmentor.dto.payroll.MarkPaidDto;
import com.learn.interviewmentor.dto.payroll.PayrollSettingsDto;
import com.learn.interviewmentor.exception.BadRequestException;
import com.learn.interviewmentor.exception.ConflictException;
import com.learn.interviewmentor.exception.NotFoundException;
import com.learn.interviewmentor.model.MentorPayout;
import com.learn.interviewmentor.model.MentorPayoutStatus;
import com.learn.interviewmentor.model.MentorProfile;
import com.learn.interviewmentor.model.SessionType;
import com.learn.interviewmentor.model.User;
import com.learn.interviewmentor.repository.InterviewRequestRepository;
import com.learn.interviewmentor.repository.MentorPayoutRepository;
import com.learn.interviewmentor.repository.MentorProfileRepository;
import com.learn.interviewmentor.service.PayrollService;
import com.learn.interviewmentor.vo.payroll.MentorPayoutVo;
import com.learn.interviewmentor.vo.payroll.MentorPayrollVo;
import com.learn.interviewmentor.vo.payroll.PayrollSummaryVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Paying mentors.
 *
 * Read {@link PayrollService} for the one rule. This class is the decisions
 * around it, and most of them are about what to refuse.
 */
@Service
@Transactional(readOnly = true)
public class PayrollServiceImpl implements PayrollService {

    private static final Logger log = LoggerFactory.getLogger(PayrollServiceImpl.class);

    private final MentorProfileRepository profileRepository;
    private final MentorPayoutRepository payoutRepository;
    private final InterviewRequestRepository requestRepository;

    public PayrollServiceImpl(MentorProfileRepository profileRepository,
                              MentorPayoutRepository payoutRepository,
                              InterviewRequestRepository requestRepository) {
        this.profileRepository = profileRepository;
        this.payoutRepository = payoutRepository;
        this.requestRepository = requestRepository;
    }

    // ------------------------------------------------------------------
    // The screen
    // ------------------------------------------------------------------

    /**
     * @implNote Every mentor with a profile, not only the verified ones.
     *
     * An unverified mentor has taken no sessions and shows zeros, so including
     * them costs a row and nothing else. Filtering them out would mean an admin
     * who just verified somebody cannot find them on this screen and has no way
     * to tell whether that is because payroll is off or because the filter hid
     * them.
     */
    @Override
    public List<MentorPayrollVo> allMentors() {
        return profileRepository.findAllByOrderBySubmittedAtDesc()
                .stream()
                .map(this::toPayrollVo)
                .toList();
    }

    @Override
    public PayrollSummaryVo summary() {
        List<MentorPayrollVo> mentors = allMentors();

        BigDecimal owed = mentors.stream()
                .map(MentorPayrollVo::amountDue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new PayrollSummaryVo(
                mentors.stream().filter(MentorPayrollVo::payrollEnabled).count(),
                mentors.stream().filter(m -> m.unpaidInterviews() + m.unpaidMentoring() > 0).count(),
                owed,
                payoutRepository.countByStatus(MentorPayoutStatus.PENDING),
                payoutRepository.totalIn(MentorPayoutStatus.PENDING),
                payoutRepository.totalIn(MentorPayoutStatus.PAID));
    }

    // ------------------------------------------------------------------
    // Settings
    // ------------------------------------------------------------------

    @Transactional
    @Override
    public MentorPayrollVo configure(Long mentorId, PayrollSettingsDto dto) {
        MentorProfile profile = profileFor(mentorId);

        // Enabling without rates would put a mentor on a screen that offers a
        // "create payout" button which then refuses - so it is caught here,
        // where the message can say which number is missing.
        if (dto.enabled()) {
            if (dto.interviewRate() == null) {
                throw new BadRequestException("Set an interview rate before turning payroll on");
            }
            if (dto.mentoringRate() == null) {
                throw new BadRequestException("Set a mentoring rate before turning payroll on");
            }
        }

        profile.configurePayroll(dto.enabled(), dto.interviewRate(), dto.mentoringRate());

        log.info("Payroll for {}: enabled={}, interview={}, mentoring={}",
                profile.getUser().getEmail(), dto.enabled(), dto.interviewRate(), dto.mentoringRate());

        return toPayrollVo(profile);
    }

    // ------------------------------------------------------------------
    // Raising a payout
    // ------------------------------------------------------------------

    /**
     * @implNote The order here is the load-bearing part.
     *
     * The payout row is saved <b>first</b>, because a row has to exist before
     * its id can be stamped onto anything. Then one UPDATE claims every unpaid
     * session. Then the totals are read back <i>from what was claimed</i>.
     *
     * Counting first and claiming second would be the natural way to write it
     * and is wrong: between the count and the claim, a mentor can complete
     * another session, or a second admin can raise a payout of their own. The
     * numbers would then describe something other than what this payout
     * actually holds - and the mentor would be paid that other thing.
     */
    @Transactional
    @Override
    public MentorPayoutVo createPayout(Long mentorId, User admin) {
        MentorProfile profile = profileFor(mentorId);

        if (!profile.isPayrollReady()) {
            throw new BadRequestException(
                    "Turn payroll on for this mentor and set both rates first.");
        }

        // 409 rather than 400: nothing about the request is malformed, the
        // mentor simply already has money waiting to go out. Two open payouts
        // would also mean the second claims nothing, because the first took
        // every session.
        if (payoutRepository.existsByMentorIdAndStatus(mentorId, MentorPayoutStatus.PENDING)) {
            throw new ConflictException(
                    "This mentor already has a payout waiting to be paid. "
                            + "Pay or cancel it before raising another.");
        }

        MentorPayout payout = payoutRepository.save(new MentorPayout(
                profile.getUser(), profile.getInterviewRate(), profile.getMentoringRate(), admin));

        int claimed = requestRepository.claimUnpaidSessions(mentorId, payout);

        // Re-read, because the claim was a bulk UPDATE with clearAutomatically.
        //
        // That clear is correct - after a bulk update every entity in the
        // persistence context is potentially stale - but it also DETACHES the
        // payout we just saved. Calling summarise() on a detached entity is not
        // an error and not a warning: the fields change in memory, dirty
        // checking never sees them, and nothing is written.
        //
        // The first version of this method did exactly that, and it is worth
        // spelling out how quiet the failure was. The API response was built
        // from the in-memory object, so it correctly said "2 interviews + 2
        // mentoring = 2600". The database row said 0 sessions and 0.00 - while
        // the four sessions were already stamped as covered and could never be
        // picked up again. A mentor would have been recorded as paid nothing
        // for work that could not be re-claimed.
        Long payoutId = payout.getId();
        payout = payoutRepository.findById(payoutId)
                .orElseThrow(() -> new IllegalStateException(
                        "Payout " + payoutId + " vanished during creation"));

        if (claimed == 0) {
            // Nothing to pay for. Removing the row rather than leaving a zero
            // payout lying about: it would show on the screen as something to
            // action, and paying ₹0 to a bank is not a thing anyone does.
            payoutRepository.delete(payout);
            throw new BadRequestException(
                    "This mentor has no completed sessions waiting to be paid.");
        }

        Map<SessionType, Integer> byType = countsByType(
                requestRepository.countByTypeInPayout(payout));

        LocalDateTime[] range = periodOf(payout);

        payout.summarise(
                byType.getOrDefault(SessionType.MOCK_INTERVIEW, 0),
                byType.getOrDefault(SessionType.MENTORING, 0),
                range[0], range[1]);

        log.info("Payout {} raised for {}: {} interviews + {} mentoring = {}",
                payout.getId(), profile.getUser().getEmail(),
                payout.getInterviewCount(), payout.getMentoringCount(), payout.getAmount());

        return MentorPayoutVo.from(payout);
    }

    // ------------------------------------------------------------------
    // Paying it
    // ------------------------------------------------------------------

    @Transactional
    @Override
    public MentorPayoutVo markPaid(Long payoutId, MarkPaidDto dto, User admin) {
        MentorPayout payout = payoutFor(payoutId);

        if (payout.getStatus() == MentorPayoutStatus.PAID) {
            throw new ConflictException("This payout is already marked paid.");
        }
        if (payout.getStatus() == MentorPayoutStatus.CANCELLED) {
            throw new ConflictException(
                    "This payout was cancelled. Raise a new one - its sessions went back in the pot.");
        }

        payout.markPaid(admin, dto.paymentReference().trim(),
                dto.notes() == null || dto.notes().isBlank() ? null : dto.notes().trim());

        log.info("Payout {} ({} to {}) marked paid by {} - ref {}",
                payoutId, payout.getAmount(), payout.getMentor().getEmail(),
                admin.getEmail(), payout.getPaymentReference());

        return MentorPayoutVo.from(payout);
    }

    /**
     * @implNote A paid payout cannot be cancelled, and that is not an oversight.
     *
     * Cancelling releases the sessions to be paid again. Doing that after the
     * money has left the bank would quietly queue up a second payment for work
     * that has already been paid for - the exact failure this whole design
     * exists to prevent, reintroduced through the undo button.
     *
     * A payment sent in error is a real thing that happens, and the answer is a
     * correcting entry rather than pretending it did not - which is what every
     * ledger does, for this reason.
     */
    @Transactional
    @Override
    public MentorPayoutVo cancelPayout(Long payoutId, String reason, User admin) {
        MentorPayout payout = payoutFor(payoutId);

        if (payout.getStatus() == MentorPayoutStatus.PAID) {
            throw new ConflictException(
                    "This payout has already been paid, so it cannot be cancelled - "
                            + "that would put its sessions back in line to be paid a second time.");
        }
        if (payout.getStatus() == MentorPayoutStatus.CANCELLED) {
            throw new ConflictException("This payout is already cancelled.");
        }

        int released = requestRepository.releaseSessions(payout);

        // Re-read for the same reason createPayout does: releaseSessions is a
        // bulk UPDATE with clearAutomatically, so it detaches this entity, and
        // calling cancel() on a detached entity changes nothing in the database.
        //
        // This one was worse than the createPayout version of the mistake, and
        // is the reason both now re-read. The sessions really were released,
        // while the payout kept its old status - so a payout could be "cancelled"
        // in the UI, still read as PAID in the database, and have its four
        // sessions sitting unclaimed waiting to be paid a second time. The exact
        // double payment this whole design exists to prevent, introduced by the
        // undo path.
        payout = payoutFor(payoutId);
        payout.cancel(reason);

        log.info("Payout {} for {} cancelled by {} ({} sessions released): {}",
                payoutId, payout.getMentor().getEmail(), admin.getEmail(), released, reason);

        return MentorPayoutVo.from(payout);
    }

    // ------------------------------------------------------------------

    @Override
    public List<MentorPayoutVo> allPayouts() {
        return payoutRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(MentorPayoutVo::from).toList();
    }

    @Override
    public List<MentorPayoutVo> payoutsFor(Long mentorId) {
        return payoutRepository.findByMentorIdOrderByCreatedAtDesc(mentorId)
                .stream().map(MentorPayoutVo::from).toList();
    }

    @Override
    public long countPending() {
        return payoutRepository.countByStatus(MentorPayoutStatus.PENDING);
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private MentorPayrollVo toPayrollVo(MentorProfile profile) {
        Long mentorId = profile.getUser().getId();

        Map<SessionType, Integer> unpaid = countsByType(
                requestRepository.countUnpaidByType(mentorId));

        int interviews = unpaid.getOrDefault(SessionType.MOCK_INTERVIEW, 0);
        int mentoring = unpaid.getOrDefault(SessionType.MENTORING, 0);

        // Zero until both rates exist. The session counts are still shown -
        // the work happened and hiding it would be wrong - but it cannot be
        // priced until somebody decides what it is worth.
        BigDecimal due = profile.getInterviewRate() == null || profile.getMentoringRate() == null
                ? BigDecimal.ZERO
                : profile.getInterviewRate().multiply(BigDecimal.valueOf(interviews))
                        .add(profile.getMentoringRate().multiply(BigDecimal.valueOf(mentoring)));

        List<com.learn.interviewmentor.model.MentorPayout> history =
                payoutRepository.findByMentorIdOrderByCreatedAtDesc(mentorId);

        LocalDateTime lastPaid = history.stream()
                .filter(p -> p.getStatus() == MentorPayoutStatus.PAID)
                .map(com.learn.interviewmentor.model.MentorPayout::getPaidAt)
                .findFirst()
                .orElse(null);

        boolean bankComplete = notBlank(profile.getBankAccountHolder())
                && notBlank(profile.getBankAccountNumber())
                && notBlank(profile.getBankIfsc())
                && notBlank(profile.getBankName());

        return new MentorPayrollVo(
                mentorId,
                profile.getUser().getFullName(),
                profile.getUser().getEmail(),
                profile.getVerificationStatus().name(),
                profile.isPayrollEnabled(),
                profile.getInterviewRate(),
                profile.getMentoringRate(),
                interviews,
                mentoring,
                due,
                payoutRepository.totalPaidTo(mentorId),
                history.stream().anyMatch(p -> p.getStatus() == MentorPayoutStatus.PENDING),
                lastPaid,
                profile.getBankAccountHolder(),
                profile.getBankAccountNumber(),
                profile.getBankIfsc(),
                profile.getBankName(),
                profile.getPanNumber(),
                bankComplete);
    }

    /**
     * Turns the repository's [SessionType, Long] rows into a map.
     *
     * A grouped count only returns rows for types that actually occur, so a
     * mentor who has run interviews and no mentoring gets one row rather than
     * two - hence getOrDefault at every call site rather than get.
     */
    private static Map<SessionType, Integer> countsByType(List<Object[]> rows) {
        Map<SessionType, Integer> counts = new EnumMap<>(SessionType.class);
        for (Object[] row : rows) {
            // Null session types exist on rows created before the column did.
            // They are mock interviews - that is what the entity's own getter
            // coalesces them to - so they are counted as such rather than
            // dropped, which would quietly underpay for older work.
            SessionType type = row[0] == null ? SessionType.MOCK_INTERVIEW : (SessionType) row[0];
            counts.merge(type, ((Number) row[1]).intValue(), Integer::sum);
        }
        return counts;
    }

    /** The window a payout's sessions span. Both null if none carry a date. */
    private LocalDateTime[] periodOf(MentorPayout payout) {
        List<Object[]> rows = requestRepository.completedRangeIn(payout);
        if (rows.isEmpty() || rows.get(0) == null) {
            return new LocalDateTime[]{null, null};
        }
        Object[] row = rows.get(0);
        return new LocalDateTime[]{(LocalDateTime) row[0], (LocalDateTime) row[1]};
    }

    private MentorProfile profileFor(Long mentorId) {
        return profileRepository.findByUserId(mentorId)
                .orElseThrow(() -> new NotFoundException("No mentor profile for user " + mentorId));
    }

    private MentorPayout payoutFor(Long id) {
        return payoutRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No payout with id " + id));
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
