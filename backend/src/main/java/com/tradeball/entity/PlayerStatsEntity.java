package com.tradeball.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "player_stats",
       uniqueConstraints = @UniqueConstraint(name = "uq_player_stats_player_season",
               columnNames = {"player_id", "season"}))
public class PlayerStatsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private PlayerEntity player;

    @Column(nullable = false)
    private Integer season;

    @Column(name = "games_played", nullable = false)
    private Integer gamesPlayed = 0;

    @Column(nullable = false)
    private Double points = 0.0;

    @Column(nullable = false)
    private Double rebounds = 0.0;

    @Column(nullable = false)
    private Double assists = 0.0;

    @Column(nullable = false)
    private Double steals = 0.0;

    @Column(nullable = false)
    private Double blocks = 0.0;

    @Column(name = "three_pointers", nullable = false)
    private Double threePointers = 0.0;

    @Column(name = "field_goal_percentage", nullable = false)
    private Double fieldGoalPercentage = 0.0;

    @Column(name = "free_throw_percentage", nullable = false)
    private Double freeThrowPercentage = 0.0;

    @Column(nullable = false)
    private Double turnovers = 0.0;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public PlayerEntity getPlayer() { return player; }
    public void setPlayer(PlayerEntity player) { this.player = player; }
    public Integer getSeason() { return season; }
    public void setSeason(Integer season) { this.season = season; }
    public Integer getGamesPlayed() { return gamesPlayed; }
    public void setGamesPlayed(Integer gamesPlayed) { this.gamesPlayed = gamesPlayed; }
    public Double getPoints() { return points; }
    public void setPoints(Double points) { this.points = points; }
    public Double getRebounds() { return rebounds; }
    public void setRebounds(Double rebounds) { this.rebounds = rebounds; }
    public Double getAssists() { return assists; }
    public void setAssists(Double assists) { this.assists = assists; }
    public Double getSteals() { return steals; }
    public void setSteals(Double steals) { this.steals = steals; }
    public Double getBlocks() { return blocks; }
    public void setBlocks(Double blocks) { this.blocks = blocks; }
    public Double getThreePointers() { return threePointers; }
    public void setThreePointers(Double threePointers) { this.threePointers = threePointers; }
    public Double getFieldGoalPercentage() { return fieldGoalPercentage; }
    public void setFieldGoalPercentage(Double fieldGoalPercentage) { this.fieldGoalPercentage = fieldGoalPercentage; }
    public Double getFreeThrowPercentage() { return freeThrowPercentage; }
    public void setFreeThrowPercentage(Double freeThrowPercentage) { this.freeThrowPercentage = freeThrowPercentage; }
    public Double getTurnovers() { return turnovers; }
    public void setTurnovers(Double turnovers) { this.turnovers = turnovers; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
