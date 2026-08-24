package com.learn.interviewmentor.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Something a student can buy: "Placement Guide", "Learn Spring Boot with an
 * expert", and so on.
 *
 * The price lives here, in the database, precisely so an admin can change it
 * from the admin panel and every student sees the new number on their next page
 * load. Contrast this with the interview fee, which is a config property
 * (app.payment.amount) and therefore needs a redeploy to change - that was fine
 * for one fixed fee, and is not fine for a price list somebody wants to tune.
 *
 * What a change to this price must NOT do is rewrite history. Anyone who already
 * bought the plan paid the old number, so {@link PlanEnrollment} copies the
 * price at purchase time rather than pointing back here.
 */
@Entity
@Table(name = "plans")
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String name;

    /** Short line under the name on the card. */
    @Column(length = 200)
    private String tagline;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Bullet points, one per line.
     *
     * A @ElementCollection would model this "properly" as its own table, but a
     * plain text column keeps the whole plan in one row, one insert and one
     * query - and nothing here ever needs to search or join on an individual
     * bullet. {@link #getFeatureList()} does the splitting.
     */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String features;

    /** Rupees. scale 2 because money in a double is how you lose paise. */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /** How long access lasts once an admin verifies payment. */
    @Column(name = "duration_days", nullable = false)
    private int durationDays = 90;

    /**
     * Retired plans go inactive rather than getting deleted - a row that
     * somebody's enrollment points at must not vanish.
     */
    @Column(nullable = false)
    private boolean active = true;

    /** Lets the admin decide the order the cards appear in. */
    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    /** Draws the "most popular" ribbon on the card. */
    @Column(name = "highlighted", nullable = false)
    private boolean highlighted = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected Plan() {
        // JPA
    }

    public Plan(String name, String tagline, String description, String features,
                BigDecimal price, int durationDays, int displayOrder, boolean highlighted) {
        this.name = name;
        this.tagline = tagline;
        this.description = description;
        this.features = features;
        this.price = price;
        this.durationDays = durationDays;
        this.displayOrder = displayOrder;
        this.highlighted = highlighted;
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

    /** Features as a list, blank lines dropped. */
    public List<String> getFeatureList() {
        if (features == null || features.isBlank()) {
            return List.of();
        }
        return Arrays.stream(features.split("\\r?\\n"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .toList();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getTagline() { return tagline; }
    public String getDescription() { return description; }
    public String getFeatures() { return features; }
    public BigDecimal getPrice() { return price; }
    public int getDurationDays() { return durationDays; }
    public boolean isActive() { return active; }
    public int getDisplayOrder() { return displayOrder; }
    public boolean isHighlighted() { return highlighted; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setName(String name) { this.name = name; }
    public void setTagline(String tagline) { this.tagline = tagline; }
    public void setDescription(String description) { this.description = description; }
    public void setFeatures(String features) { this.features = features; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public void setDurationDays(int durationDays) { this.durationDays = durationDays; }
    public void setActive(boolean active) { this.active = active; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
    public void setHighlighted(boolean highlighted) { this.highlighted = highlighted; }
}
