package com.tradeball.entity;

import com.tradeball.domain.TradeDirection;
import jakarta.persistence.*;

@Entity
@Table(name = "trade_evaluation_players")
public class TradeEvaluationPlayerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trade_evaluation_id", nullable = false)
    private TradeEvaluationEntity tradeEvaluation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private PlayerEntity player;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TradeDirection direction;

    @Column(name = "fantasy_score", nullable = false)
    private Double fantasyScore;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public TradeEvaluationEntity getTradeEvaluation() { return tradeEvaluation; }
    public void setTradeEvaluation(TradeEvaluationEntity tradeEvaluation) { this.tradeEvaluation = tradeEvaluation; }
    public PlayerEntity getPlayer() { return player; }
    public void setPlayer(PlayerEntity player) { this.player = player; }
    public TradeDirection getDirection() { return direction; }
    public void setDirection(TradeDirection direction) { this.direction = direction; }
    public Double getFantasyScore() { return fantasyScore; }
    public void setFantasyScore(Double fantasyScore) { this.fantasyScore = fantasyScore; }
}
