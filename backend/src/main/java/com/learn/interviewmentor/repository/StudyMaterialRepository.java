package com.learn.interviewmentor.repository;

import com.learn.interviewmentor.model.StudyMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface StudyMaterialRepository extends JpaRepository<StudyMaterial, Long> {

    /** The admin view: everything ever sent, newest first. */
    List<StudyMaterial> findAllByOrderByCreatedAtDesc();

    /**
     * Everything one student is allowed to see, newest first.
     *
     * This query IS the access control. Filtering on the client would leave the
     * rows sitting in a JSON response that anyone can open the network tab and
     * read, and returning another student's private material would be a real
     * leak even if no screen ever rendered it.
     *
     * The plan branch takes a collection because a student can hold several
     * plans at once. An empty collection would make "in ()" invalid SQL, so the
     * caller passes a harmless sentinel instead - see StudyMaterialService.
     */
    @Query("""
            select m from StudyMaterial m
            where m.active = true
              and (
                    m.audience = com.learn.interviewmentor.model.MaterialAudience.ALL_STUDENTS
                 or (m.audience = com.learn.interviewmentor.model.MaterialAudience.SPECIFIC_STUDENT
                     and m.targetStudent.id = :studentId)
                 or (m.audience = com.learn.interviewmentor.model.MaterialAudience.PLAN_MEMBERS
                     and m.targetPlan.id in :planIds)
              )
            order by m.createdAt desc
            """)
    List<StudyMaterial> findVisibleTo(@Param("studentId") Long studentId,
                                      @Param("planIds") Collection<Long> planIds);

    long countByActiveTrue();
}
