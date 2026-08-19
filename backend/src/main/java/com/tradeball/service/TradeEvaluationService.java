package com.tradeball.service;

import com.tradeball.config.NbaApiProperties;
import com.tradeball.domain.CategoryImpact;
import com.tradeball.domain.CategoryStatistics;
import com.tradeball.domain.CategoryZScores;
import com.tradeball.domain.FantasyCategory;
import com.tradeball.domain.FantasyScore;
import com.tradeball.domain.PopulationStats;
import com.tradeball.domain.TradeDirection;
import com.tradeball.domain.TradeSignalType;
import com.tradeball.domain.TradeVerdict;
import com.tradeball.dto.CategoryAnalysisResponse;
import com.tradeball.dto.PageResponse;
import com.tradeball.dto.TradeEvaluateRequest;
import com.tradeball.dto.TradeEvaluationResponse;
import com.tradeball.entity.PlayerEntity;
import com.tradeball.entity.PlayerStatsEntity;
import com.tradeball.entity.RosterEntity;
import com.tradeball.entity.TradeEvaluationCategoryEntity;
import com.tradeball.entity.TradeEvaluationEntity;
import com.tradeball.entity.TradeEvaluationPlayerEntity;
import com.tradeball.entity.UserEntity;
import com.tradeball.exception.ApiErrorCode;
import com.tradeball.exception.ApiException;
import com.tradeball.exception.ResourceNotFoundException;
import com.tradeball.repository.PlayerRepository;
import com.tradeball.repository.PlayerStatsRepository;
import com.tradeball.repository.RosterRepository;
import com.tradeball.repository.TradeEvaluationRepository;
import com.tradeball.repository.UserRepository;
import com.tradeball.security.SecurityUtils;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TradeEvaluationService {

    private static final Logger log = LoggerFactory.getLogger(TradeEvaluationService.class);

    private final PlayerRepository playerRepository;
    private final PlayerStatsRepository playerStatsRepository;
    private final TradeEvaluationRepository tradeEvaluationRepository;
    private final UserRepository userRepository;
    private final RosterRepository rosterRepository;
    private final FantasyEvaluationService fantasyEvaluationService;
    private final PopulationStatsService populationStatsService;
    private final TradeSignalService tradeSignalService;
    private final TradeScoringPolicy tradeScoringPolicy;
    private final StatsMapperHelper statsMapperHelper;
    private final NbaApiProperties nbaApiProperties;

    public TradeEvaluationService(PlayerRepository playerRepository,
                                  PlayerStatsRepository playerStatsRepository,
                                  TradeEvaluationRepository tradeEvaluationRepository,
                                  UserRepository userRepository,
                                  RosterRepository rosterRepository,
                                  FantasyEvaluationService fantasyEvaluationService,
                                  PopulationStatsService populationStatsService,
                                  TradeSignalService tradeSignalService,
                                  TradeScoringPolicy tradeScoringPolicy,
                                  StatsMapperHelper statsMapperHelper,
                                  NbaApiProperties nbaApiProperties) {
        this.playerRepository = playerRepository;
        this.playerStatsRepository = playerStatsRepository;
        this.tradeEvaluationRepository = tradeEvaluationRepository;
        this.userRepository = userRepository;
        this.rosterRepository = rosterRepository;
        this.fantasyEvaluationService = fantasyEvaluationService;
        this.populationStatsService = populationStatsService;
        this.tradeSignalService = tradeSignalService;
        this.tradeScoringPolicy = tradeScoringPolicy;
        this.statsMapperHelper = statsMapperHelper;
        this.nbaApiProperties = nbaApiProperties;
    }

    @Transactional
    public TradeEvaluationResponse evaluate(TradeEvaluateRequest request) {
        try {
            validateRequest(request);
            List<PlayerBundle> incoming = loadBundles(request.incomingPlayerIds());
            List<PlayerBundle> outgoing = loadBundles(request.outgoingPlayerIds());

            PopulationStats population = populationStatsService.currentSeasonPopulation();
            List<Double> allScores = populationStatsService.allRawFantasyScores();

            List<FantasyScore> incomingScores = scorePlayers(incoming, population);
            List<FantasyScore> outgoingScores = scorePlayers(outgoing, population);

            double incomingFs = sumRaw(incomingScores);
            double outgoingFs = sumRaw(outgoingScores);
            double incomingAge = averageAge(incoming);
            double outgoingAge = averageAge(outgoing);
            double incomingGp = averageGp(incoming);
            double outgoingGp = averageGp(outgoing);

            int score = tradeScoringPolicy.calculateScore(
                    incomingFs, outgoingFs, incomingAge, outgoingAge, incomingGp, outgoingGp);
            TradeVerdict verdict = tradeScoringPolicy.verdictFor(score);

            List<CategoryAnalysisResponse> categoryAnalysis = buildCategoryAnalysis(incoming, outgoing, population);
            List<String> strengths = new ArrayList<>();
            List<String> weaknesses = new ArrayList<>();
            for (CategoryAnalysisResponse cat : categoryAnalysis) {
                if (cat.impact() == CategoryImpact.POSITIVE) {
                    strengths.add(cat.category().name() + " +" + String.format(Locale.US, "%.2f", cat.delta()));
                } else if (cat.impact() == CategoryImpact.NEGATIVE) {
                    weaknesses.add(cat.category().name() + " " + String.format(Locale.US, "%.2f", cat.delta()));
                }
            }

            List<TradeSignalType> signals = tradeSignalService.detect(
                    incomingFs, outgoingFs, incomingAge, allScores);
            List<String> signalNames = signals.stream().map(Enum::name).toList();

            String explanation = buildExplanation(incoming, outgoing, score, incomingFs - outgoingFs, signals);

            TradeEvaluationEntity saved = persistIfAuthenticated(
                    score, verdict, explanation, incoming, outgoing, incomingScores, outgoingScores, categoryAnalysis);

            return new TradeEvaluationResponse(
                    saved == null ? null : saved.getId(),
                    score,
                    verdict,
                    incomingFs,
                    outgoingFs,
                    categoryAnalysis,
                    strengths,
                    weaknesses,
                    signalNames,
                    explanation,
                    fantasyEvaluationService.modelVersion()
            );
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Trade evaluation failed", ex);
            throw new ApiException(ApiErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR,
                    "Trade evaluation failed");
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<TradeEvaluationResponse> history(Pageable pageable) {
        Long userId = SecurityUtils.currentUserId();
        Page<TradeEvaluationResponse> page = tradeEvaluationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toSummaryResponse);
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public TradeEvaluationResponse get(Long id) {
        Long userId = SecurityUtils.currentUserId();
        TradeEvaluationEntity entity = tradeEvaluationRepository.findDetailedByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Trade evaluation not found: " + id));
        // Force initialization within transaction (open-in-view is disabled)
        entity.getPlayers().size();
        entity.getCategories().size();
        return toDetailedResponse(entity);
    }

    private void validateRequest(TradeEvaluateRequest request) {
        Set<Long> incomingIds = new HashSet<>(request.incomingPlayerIds());
        Set<Long> outgoingIds = new HashSet<>(request.outgoingPlayerIds());
        if (incomingIds.size() != request.incomingPlayerIds().size()
                || outgoingIds.size() != request.outgoingPlayerIds().size()) {
            throw badRequest("A player may appear only once on each side of a trade");
        }
        if (incomingIds.stream().anyMatch(outgoingIds::contains)) {
            throw badRequest("A player cannot be both incoming and outgoing");
        }
        if (request.rosterId() != null) {
            Long userId = SecurityUtils.currentUserId();
            RosterEntity roster = rosterRepository.findByIdWithPlayers(request.rosterId())
                    .orElseThrow(() -> new ResourceNotFoundException("Roster not found: " + request.rosterId()));
            if (!roster.getUser().getId().equals(userId)) {
                throw new ApiException(ApiErrorCode.AUTHORIZATION_ERROR, HttpStatus.FORBIDDEN, "Not your roster");
            }
            Set<Long> rosterPlayerIds = roster.getPlayers().stream().map(PlayerEntity::getId).collect(java.util.stream.Collectors.toSet());
            if (!rosterPlayerIds.containsAll(outgoingIds)) {
                throw badRequest("Outgoing players must belong to the selected roster");
            }
        }
    }

    private ApiException badRequest(String message) {
        return new ApiException(ApiErrorCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, message);
    }

    private List<PlayerBundle> loadBundles(List<Long> ids) {
        List<PlayerBundle> bundles = new ArrayList<>();
        for (Long id : ids) {
            PlayerEntity player = playerRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Player not found: " + id));
            PlayerStatsEntity stats = playerStatsRepository.findByPlayerIdAndSeason(id, nbaApiProperties.season())
                    .orElseThrow(() -> new ResourceNotFoundException("Stats not found for player: " + id));
            bundles.add(new PlayerBundle(player, stats, statsMapperHelper.toCategoryStatistics(stats)));
        }
        return bundles;
    }

    private List<FantasyScore> scorePlayers(List<PlayerBundle> bundles, PopulationStats population) {
        List<FantasyScore> scores = new ArrayList<>();
        for (PlayerBundle bundle : bundles) {
            scores.add(fantasyEvaluationService.evaluate(bundle.stats(), population));
        }
        return scores;
    }

    private List<CategoryAnalysisResponse> buildCategoryAnalysis(List<PlayerBundle> incoming,
                                                                 List<PlayerBundle> outgoing,
                                                                 PopulationStats population) {
        List<CategoryZScores> inZ = incoming.stream()
                .map(b -> fantasyEvaluationService.computeZScores(b.stats(), population)).toList();
        List<CategoryZScores> outZ = outgoing.stream()
                .map(b -> fantasyEvaluationService.computeZScores(b.stats(), population)).toList();

        List<CategoryAnalysisResponse> analysis = new ArrayList<>();
        for (FantasyCategory category : FantasyCategory.values()) {
            double inVal = sideStat(incoming, category);
            double outVal = sideStat(outgoing, category);
            double delta = inVal - outVal;
            double inSumZ = fantasyEvaluationService.sumZ(inZ, category);
            double outSumZ = fantasyEvaluationService.sumZ(outZ, category);
            double zDelta = inSumZ - outSumZ;
            CategoryImpact impact;
            if (Math.abs(zDelta) < 0.05) {
                impact = CategoryImpact.NEUTRAL;
            } else if (zDelta > 0) {
                impact = CategoryImpact.POSITIVE;
            } else {
                impact = CategoryImpact.NEGATIVE;
            }
            // Turnovers: lower is better — flip impact based on raw delta when using TO category
            if (category == FantasyCategory.TO) {
                if (Math.abs(delta) < 0.05) {
                    impact = CategoryImpact.NEUTRAL;
                } else {
                    impact = delta < 0 ? CategoryImpact.POSITIVE : CategoryImpact.NEGATIVE;
                }
            }
            analysis.add(new CategoryAnalysisResponse(category, inVal, outVal, delta, zDelta, impact));
        }
        return analysis;
    }

    private TradeEvaluationEntity persistIfAuthenticated(int score,
                                                         TradeVerdict verdict,
                                                         String explanation,
                                                         List<PlayerBundle> incoming,
                                                         List<PlayerBundle> outgoing,
                                                         List<FantasyScore> incomingScores,
                                                         List<FantasyScore> outgoingScores,
                                                         List<CategoryAnalysisResponse> categoryAnalysis) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        Long userId = SecurityUtils.currentUserId();
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        TradeEvaluationEntity entity = new TradeEvaluationEntity();
        entity.setUser(user);
        entity.setScore(score);
        entity.setVerdict(verdict);
        entity.setExplanation(explanation);
        entity.setModelVersion(fantasyEvaluationService.modelVersion());

        for (int i = 0; i < incoming.size(); i++) {
            TradeEvaluationPlayerEntity row = new TradeEvaluationPlayerEntity();
            row.setTradeEvaluation(entity);
            row.setPlayer(incoming.get(i).player());
            row.setDirection(TradeDirection.INCOMING);
            row.setFantasyScore(incomingScores.get(i).rawScore());
            entity.getPlayers().add(row);
        }
        for (int i = 0; i < outgoing.size(); i++) {
            TradeEvaluationPlayerEntity row = new TradeEvaluationPlayerEntity();
            row.setTradeEvaluation(entity);
            row.setPlayer(outgoing.get(i).player());
            row.setDirection(TradeDirection.OUTGOING);
            row.setFantasyScore(outgoingScores.get(i).rawScore());
            entity.getPlayers().add(row);
        }
        for (CategoryAnalysisResponse cat : categoryAnalysis) {
            TradeEvaluationCategoryEntity row = new TradeEvaluationCategoryEntity();
            row.setTradeEvaluation(entity);
            row.setCategory(cat.category());
            row.setIncomingValue(cat.incomingValue());
            row.setOutgoingValue(cat.outgoingValue());
            row.setDelta(cat.delta());
            row.setZScoreDelta(cat.zScoreDelta());
            row.setImpact(cat.impact());
            entity.getCategories().add(row);
        }
        return tradeEvaluationRepository.save(entity);
    }

    private String buildExplanation(List<PlayerBundle> incoming,
                                    List<PlayerBundle> outgoing,
                                    int score,
                                    double fsDelta,
                                    List<TradeSignalType> signals) {
        String give = outgoing.stream().map(b -> b.player().fullName()).reduce((a, b) -> a + " & " + b).orElse("outgoing");
        String get = incoming.stream().map(b -> b.player().fullName()).reduce((a, b) -> a + " & " + b).orElse("incoming");
        StringBuilder sb = new StringBuilder();
        if (score >= 65) {
            sb.append("Trading ").append(give).append(" for ").append(get)
                    .append(" improves your combined 9-cat production by ")
                    .append(String.format(Locale.US, "%.1f", Math.abs(fsDelta)))
                    .append(" fantasy points. The incoming side adds more total value across the scored categories.");
        } else if (score >= 45) {
            sb.append("This is a roughly even swap of combined 9-cat production (")
                    .append(String.format(Locale.US, "%+.1f", fsDelta))
                    .append(" pts). Positional fit and injury risk can still swing it.");
        } else {
            sb.append("You're giving up ").append(String.format(Locale.US, "%.1f", Math.abs(fsDelta)))
                    .append(" combined fantasy points in this deal. ").append(give)
                    .append(" produces more total 9-cat value than ").append(get)
                    .append(". Hold or negotiate a better return.");
        }
        if (incoming.size() != outgoing.size()) {
            sb.append(" Player counts differ (").append(outgoing.size()).append(" out, ")
                    .append(incoming.size())
                    .append(" in); the grade uses summed production, not per-player averages.");
        }
        if (!signals.isEmpty()) {
            sb.append(" Signals: ").append(String.join(", ", signals.stream().map(Enum::name).toList())).append(".");
        }
        return sb.toString();
    }

    private TradeEvaluationResponse toSummaryResponse(TradeEvaluationEntity entity) {
        return new TradeEvaluationResponse(
                entity.getId(),
                entity.getScore(),
                entity.getVerdict(),
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                entity.getExplanation(),
                entity.getModelVersion()
        );
    }

    private TradeEvaluationResponse toDetailedResponse(TradeEvaluationEntity entity) {
        double incoming = entity.getPlayers().stream()
                .filter(p -> p.getDirection() == TradeDirection.INCOMING)
                .mapToDouble(TradeEvaluationPlayerEntity::getFantasyScore).sum();
        double outgoing = entity.getPlayers().stream()
                .filter(p -> p.getDirection() == TradeDirection.OUTGOING)
                .mapToDouble(TradeEvaluationPlayerEntity::getFantasyScore).sum();
        List<CategoryAnalysisResponse> cats = entity.getCategories().stream()
                .map(c -> new CategoryAnalysisResponse(
                        c.getCategory(), c.getIncomingValue(), c.getOutgoingValue(),
                        c.getDelta(), c.getZScoreDelta(), c.getImpact()))
                .toList();
        return new TradeEvaluationResponse(
                entity.getId(),
                entity.getScore(),
                entity.getVerdict(),
                incoming,
                outgoing,
                cats,
                List.of(),
                List.of(),
                List.of(),
                entity.getExplanation(),
                entity.getModelVersion()
        );
    }

    private double sumRaw(List<FantasyScore> scores) {
        return scores.stream().mapToDouble(FantasyScore::rawScore).sum();
    }

    private double averageAge(List<PlayerBundle> bundles) {
        return bundles.stream()
                .mapToDouble(b -> b.player().getAge() == null ? 26.0 : b.player().getAge())
                .average().orElse(26.0);
    }

    private double averageGp(List<PlayerBundle> bundles) {
        return bundles.stream()
                .mapToDouble(b -> b.entityStats().getGamesPlayed() == null ? 0.0 : b.entityStats().getGamesPlayed())
                .average().orElse(0.0);
    }

    private double sideStat(List<PlayerBundle> bundles, FantasyCategory category) {
        if (category.isPercentage()) {
            return bundles.stream().mapToDouble(b -> b.stats().get(category)).average().orElse(0.0);
        }
        return bundles.stream().mapToDouble(b -> b.stats().get(category)).sum();
    }

    private record PlayerBundle(PlayerEntity player, PlayerStatsEntity entityStats, CategoryStatistics stats) {
    }
}
