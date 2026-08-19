package com.tradeball.service;

import com.tradeball.dto.RosterRequest;
import com.tradeball.dto.RosterResponse;
import com.tradeball.entity.PlayerEntity;
import com.tradeball.entity.RosterEntity;
import com.tradeball.entity.UserEntity;
import com.tradeball.exception.ApiErrorCode;
import com.tradeball.exception.ApiException;
import com.tradeball.exception.ConflictException;
import com.tradeball.exception.ResourceNotFoundException;
import com.tradeball.mapper.PlayerMapper;
import com.tradeball.repository.PlayerRepository;
import com.tradeball.repository.RosterRepository;
import com.tradeball.repository.UserRepository;
import com.tradeball.security.SecurityUtils;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RosterService {

    private final RosterRepository rosterRepository;
    private final UserRepository userRepository;
    private final PlayerRepository playerRepository;
    private final PlayerMapper playerMapper;

    public RosterService(RosterRepository rosterRepository,
                         UserRepository userRepository,
                         PlayerRepository playerRepository,
                         PlayerMapper playerMapper) {
        this.rosterRepository = rosterRepository;
        this.userRepository = userRepository;
        this.playerRepository = playerRepository;
        this.playerMapper = playerMapper;
    }

    @Transactional
    public RosterResponse create(RosterRequest request) {
        Long userId = SecurityUtils.currentUserId();
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        RosterEntity roster = new RosterEntity();
        roster.setUser(user);
        roster.setName(request.name().trim());
        return toResponse(rosterRepository.save(roster));
    }

    @Transactional(readOnly = true)
    public List<RosterResponse> listMine() {
        Long userId = SecurityUtils.currentUserId();
        return rosterRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RosterResponse get(Long id) {
        return toResponse(requireOwned(id));
    }

    @Transactional
    public RosterResponse update(Long id, RosterRequest request) {
        RosterEntity roster = requireOwned(id);
        roster.setName(request.name().trim());
        return toResponse(rosterRepository.save(roster));
    }

    @Transactional
    public void delete(Long id) {
        RosterEntity roster = requireOwned(id);
        rosterRepository.delete(roster);
    }

    @Transactional
    public RosterResponse addPlayer(Long rosterId, Long playerId) {
        RosterEntity roster = requireOwned(rosterId);
        PlayerEntity player = playerRepository.findById(playerId)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found: " + playerId));
        boolean alreadyPresent = roster.getPlayers().stream().anyMatch(p -> p.getId().equals(playerId));
        if (alreadyPresent) {
            throw new ConflictException("Player already on roster");
        }
        roster.getPlayers().add(player);
        return toResponse(rosterRepository.save(roster));
    }

    @Transactional
    public RosterResponse removePlayer(Long rosterId, Long playerId) {
        RosterEntity roster = requireOwned(rosterId);
        boolean removed = roster.getPlayers().removeIf(p -> p.getId().equals(playerId));
        if (!removed) {
            throw new ResourceNotFoundException("Player not on roster");
        }
        return toResponse(rosterRepository.save(roster));
    }

    private RosterEntity requireOwned(Long rosterId) {
        Long userId = SecurityUtils.currentUserId();
        RosterEntity roster = rosterRepository.findByIdWithPlayers(rosterId)
                .orElseThrow(() -> new ResourceNotFoundException("Roster not found: " + rosterId));
        if (!roster.getUser().getId().equals(userId)) {
            throw new ApiException(ApiErrorCode.AUTHORIZATION_ERROR, HttpStatus.FORBIDDEN, "Not your roster");
        }
        return roster;
    }

    private RosterResponse toResponse(RosterEntity roster) {
        List<com.tradeball.dto.PlayerResponse> players = roster.getPlayers().stream()
                .sorted(Comparator.comparing(PlayerEntity::getLastName).thenComparing(PlayerEntity::getFirstName))
                .map(playerMapper::toResponse)
                .toList();
        return new RosterResponse(roster.getId(), roster.getName(), players, roster.getCreatedAt(), roster.getUpdatedAt());
    }
}
