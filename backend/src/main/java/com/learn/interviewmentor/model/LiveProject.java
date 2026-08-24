package com.learn.interviewmentor.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * One of our real, private codebases that a student can pay to contribute to.
 *
 * The point of the feature: open source is the usual advice for building a
 * portfolio, and it is bad advice for a beginner - nobody reviews their PRs and
 * nothing they touch is in production. These repos are ours, they run, and a
 * senior engineer reviews every pull request.
 *
 * <h2>The repo is stored as owner + name, not a URL</h2>
 * Because the GitHub API needs them separately - adding a collaborator is
 * {@code PUT /repos/{owner}/{repo}/collaborators/{username}}. Storing
 * "https://github.com/acme/api" and parsing it back out at the point of use
 * means every caller re-implements the parsing, and one of them gets a trailing
 * slash wrong. {@link #getRepoUrl()} builds the URL for display.
 *
 * <h2>Private on purpose</h2>
 * Nothing here is public, which is exactly why access has to be granted per
 * person. A student with no access cannot see the code at all - so the repo
 * owner/name are only ever sent to somebody whose access is active.
 */
@Entity
@Table(name = "live_projects")
public class LiveProject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 140)
    private String name;

    /** One line under the name on the card. */
    @Column(length = 240)
    private String summary;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    /** Comma-separated: "Java, Spring Boot, MySQL, React". */
    @Column(name = "tech_stack", length = 300)
    private String techStack;

    /** What a contributor would actually pick up. One per line. */
    @Lob
    @Column(name = "sample_tasks", columnDefinition = "TEXT")
    private String sampleTasks;

    // ---- the repository ----

    @Column(name = "repo_owner", nullable = false, length = 100)
    private String repoOwner;

    @Column(name = "repo_name", nullable = false, length = 120)
    private String repoName;

    /** Optional: a CONTRIBUTING.md or onboarding doc to read first. */
    @Column(name = "onboarding_url", length = 500)
    private String onboardingUrl;

    // ---- commercials ----

    /**
     * Rupees. Stored here so an admin can change it from the admin panel, and
     * {@link ProjectAccessRequest} copies it at request time so a later change
     * never rewrites what somebody already paid.
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "access_duration_days", nullable = false)
    private int accessDurationDays = 90;

    /**
     * How many contributors can hold access at once. Null means no limit.
     *
     * A real repo has a real review budget - one senior engineer cannot
     * meaningfully review thirty newcomers at the same time, and selling access
     * past that point sells something we cannot deliver.
     */
    @Column(name = "max_contributors")
    private Integer maxContributors;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProjectDifficulty difficulty = ProjectDifficulty.INTERMEDIATE;

    /** The senior engineer who reviews pull requests on this repo. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_reviewer_id")
    private User leadReviewer;

    /** Retired projects go inactive rather than being deleted - access rows point here. */
    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected LiveProject() {
        // JPA
    }

    public LiveProject(String name, String summary, String description, String techStack,
                       String sampleTasks, String repoOwner, String repoName,
                       BigDecimal price, int accessDurationDays, Integer maxContributors,
                       ProjectDifficulty difficulty, int displayOrder) {
        this.name = name;
        this.summary = summary;
        this.description = description;
        this.techStack = techStack;
        this.sampleTasks = sampleTasks;
        this.repoOwner = repoOwner;
        this.repoName = repoName;
        this.price = price;
        this.accessDurationDays = accessDurationDays;
        this.maxContributors = maxContributors;
        this.difficulty = difficulty;
        this.displayOrder = displayOrder;
        this.active = true;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /** "owner/name" - how GitHub itself refers to a repo. */
    public String getRepoFullName() {
        return repoOwner + "/" + repoName;
    }

    /** Built, not stored - see the class comment. */
    public String getRepoUrl() {
        return "https://github.com/" + getRepoFullName();
    }

    public List<String> getTechStackList() {
        return splitOn(techStack, ",");
    }

    public List<String> getSampleTaskList() {
        return splitOn(sampleTasks, "\\r?\\n");
    }

    private static List<String> splitOn(String value, String separator) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(separator))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getSummary() { return summary; }
    public String getDescription() { return description; }
    public String getTechStack() { return techStack; }
    public String getSampleTasks() { return sampleTasks; }
    public String getRepoOwner() { return repoOwner; }
    public String getRepoName() { return repoName; }
    public String getOnboardingUrl() { return onboardingUrl; }
    public BigDecimal getPrice() { return price; }
    public int getAccessDurationDays() { return accessDurationDays; }
    public Integer getMaxContributors() { return maxContributors; }
    public ProjectDifficulty getDifficulty() { return difficulty; }
    public User getLeadReviewer() { return leadReviewer; }
    public boolean isActive() { return active; }
    public int getDisplayOrder() { return displayOrder; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setName(String name) { this.name = name; }
    public void setSummary(String summary) { this.summary = summary; }
    public void setDescription(String description) { this.description = description; }
    public void setTechStack(String techStack) { this.techStack = techStack; }
    public void setSampleTasks(String sampleTasks) { this.sampleTasks = sampleTasks; }
    public void setRepoOwner(String repoOwner) { this.repoOwner = repoOwner; }
    public void setRepoName(String repoName) { this.repoName = repoName; }
    public void setOnboardingUrl(String onboardingUrl) { this.onboardingUrl = onboardingUrl; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public void setAccessDurationDays(int days) { this.accessDurationDays = days; }
    public void setMaxContributors(Integer maxContributors) { this.maxContributors = maxContributors; }
    public void setDifficulty(ProjectDifficulty difficulty) { this.difficulty = difficulty; }
    public void setLeadReviewer(User leadReviewer) { this.leadReviewer = leadReviewer; }
    public void setActive(boolean active) { this.active = active; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
}
