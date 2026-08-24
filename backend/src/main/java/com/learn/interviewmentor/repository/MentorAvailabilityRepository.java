package com.learn.interviewmentor.repository;

import com.learn.interviewmentor.model.MentorAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MentorAvailabilityRepository extends JpaRepository<MentorAvailability, Long> {

    Optional<MentorAvailability> findByMentorIdAndSlotStart(Long mentorId, LocalDateTime slotStart);

    /** One mentor's own declared hours, from a point in time onwards. */
    @Query("""
            select a from MentorAvailability a
            where a.mentor.id = :mentorId
              and a.slotStart >= :from
              and a.status <> com.learn.interviewmentor.model.AvailabilityStatus.WITHDRAWN
            order by a.slotStart asc
            """)
    List<MentorAvailability> findMineFrom(@Param("mentorId") Long mentorId,
                                          @Param("from") LocalDateTime from);

    /**
     * Every mentor's declared hours in a window. The admin's reference view.
     *
     * Only APPROVED mentors: an unverified mentor cannot take a session, so their
     * availability must not make a slot look bookable.
     */
    @Query("""
            select a from MentorAvailability a
            where a.slotStart >= :from and a.slotStart < :to
              and a.status <> com.learn.interviewmentor.model.AvailabilityStatus.WITHDRAWN
              and exists (
                    select p from MentorProfile p
                    where p.user.id = a.mentor.id
                      and p.verificationStatus =
                          com.learn.interviewmentor.model.VerificationStatus.APPROVED)
            order by a.slotStart asc
            """)
    List<MentorAvailability> findAllInWindow(@Param("from") LocalDateTime from,
                                             @Param("to") LocalDateTime to);

    /**
     * Open hours on one day that cover the kind of session asked about.
     *
     * This query IS the slot grid. Before this feature the grid was generated
     * 09:00-21:00 with capacity = "how many verified mentors exist", which sold
     * slots nobody had agreed to take. Now a slot exists only because a real,
     * verified mentor said they were free for that kind of session.
     *
     * The interviews/mentoring flags are compared against a pair of booleans
     * rather than a session type, so the caller passes exactly one of them true.
     */
    @Query("""
            select a from MentorAvailability a
            where a.slotStart >= :dayStart and a.slotStart < :dayEnd
              and a.status = com.learn.interviewmentor.model.AvailabilityStatus.OPEN
              and a.bookedRequest is null
              and ((:wantInterview = true and a.forInterviews = true)
                or (:wantInterview = false and a.forMentoring = true))
              and exists (
                    select p from MentorProfile p
                    where p.user.id = a.mentor.id
                      and p.verificationStatus =
                          com.learn.interviewmentor.model.VerificationStatus.APPROVED)
            order by a.slotStart asc
            """)
    List<MentorAvailability> findOpenOnDay(@Param("dayStart") LocalDateTime dayStart,
                                            @Param("dayEnd") LocalDateTime dayEnd,
                                            @Param("wantInterview") boolean wantInterview);

    /**
     * Mentors free for one exact hour and session type.
     *
     * Drives the admin's assign dropdown - so the list an admin picks from is
     * people who actually said yes to that hour, rather than every verified
     * mentor on the books.
     */
    @Query("""
            select a from MentorAvailability a
            where a.slotStart = :slot
              and a.status = com.learn.interviewmentor.model.AvailabilityStatus.OPEN
              and a.bookedRequest is null
              and ((:wantInterview = true and a.forInterviews = true)
                or (:wantInterview = false and a.forMentoring = true))
              and exists (
                    select p from MentorProfile p
                    where p.user.id = a.mentor.id
                      and p.verificationStatus =
                          com.learn.interviewmentor.model.VerificationStatus.APPROVED)
            order by a.createdAt asc
            """)
    List<MentorAvailability> findOpenForSlot(@Param("slot") LocalDateTime slot,
                                              @Param("wantInterview") boolean wantInterview);

    /** Released when a booking is cancelled, so the hour goes back on the market. */
    Optional<MentorAvailability> findByBookedRequestId(Long requestId);

    long countByStatus(com.learn.interviewmentor.model.AvailabilityStatus status);
}
