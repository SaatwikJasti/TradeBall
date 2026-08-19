package com.tradeball.entity;

import com.tradeball.domain.TradeVerdict;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "trade_evaluations")
public class TradeEvaluationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Integer score;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TradeVerdict verdict;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "model_version", nullable = false, length = 64)
    private String modelVersion;

    @OneToMany(mappedBy = "tradeEvaluation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TradeEvaluationPlayerEntity> players = new ArrayList<>();

    @OneToMany(mappedBy = "tradeEvaluation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TradeEvaluationCategoryEntity> categories = new ArrayList<>();

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public UserEntity getUser() { return user; }
    public void setUser(UserEntity user) { this.user = user; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public TradeVerdict getVerdict() { return verdict; }
    public void setVerdict(TradeVerdict verdict) { this.verdict = verdict; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }
    public List<TradeEvaluationPlayerEntity> getPlayers() { return players; }
    public void setPlayers(List<TradeEvaluationPlayerEntity> players) { this.players = players; }
    public List<TradeEvaluationCategoryEntity> getCategories() { return categories; }
    public void setCategories(List<TradeEvaluationCategoryEntity> categories) { this.categories = categories; }
}
