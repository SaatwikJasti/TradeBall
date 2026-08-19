package com.tradeball.entity;

import com.tradeball.domain.CategoryImpact;
import com.tradeball.domain.FantasyCategory;
import jakarta.persistence.*;

@Entity
@Table(name = "trade_evaluation_categories")
public class TradeEvaluationCategoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trade_evaluation_id", nullable = false)
    private TradeEvaluationEntity tradeEvaluation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private FantasyCategory category;

    @Column(name = "incoming_value", nullable = false)
    private Double incomingValue;

    @Column(name = "outgoing_value", nullable = false)
    private Double outgoingValue;

    @Column(nullable = false)
    private Double delta;

    @Column(name = "z_score_delta", nullable = false)
    private Double zScoreDelta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CategoryImpact impact;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public TradeEvaluationEntity getTradeEvaluation() { return tradeEvaluation; }
    public void setTradeEvaluation(TradeEvaluationEntity tradeEvaluation) { this.tradeEvaluation = tradeEvaluation; }
    public FantasyCategory getCategory() { return category; }
    public void setCategory(FantasyCategory category) { this.category = category; }
    public Double getIncomingValue() { return incomingValue; }
    public void setIncomingValue(Double incomingValue) { this.incomingValue = incomingValue; }
    public Double getOutgoingValue() { return outgoingValue; }
    public void setOutgoingValue(Double outgoingValue) { this.outgoingValue = outgoingValue; }
    public Double getDelta() { return delta; }
    public void setDelta(Double delta) { this.delta = delta; }
    public Double getZScoreDelta() { return zScoreDelta; }
    public void setZScoreDelta(Double zScoreDelta) { this.zScoreDelta = zScoreDelta; }
    public CategoryImpact getImpact() { return impact; }
    public void setImpact(CategoryImpact impact) { this.impact = impact; }
}
