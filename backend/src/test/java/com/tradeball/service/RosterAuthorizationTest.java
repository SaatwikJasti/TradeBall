package com.tradeball.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.tradeball.domain.Role;
import com.tradeball.entity.RosterEntity;
import com.tradeball.entity.UserEntity;
import com.tradeball.exception.ApiException;
import com.tradeball.mapper.PlayerMapper;
import com.tradeball.repository.PlayerRepository;
import com.tradeball.repository.RosterRepository;
import com.tradeball.repository.UserRepository;
import com.tradeball.security.UserPrincipal;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class RosterAuthorizationTest {

    @Mock RosterRepository rosterRepository;
    @Mock UserRepository userRepository;
    @Mock PlayerRepository playerRepository;

    RosterService rosterService;

    @BeforeEach
    void setUp() {
        rosterService = new RosterService(rosterRepository, userRepository, playerRepository, new PlayerMapper());
        UserPrincipal principal = new UserPrincipal(1L, "a@example.com", "x", Role.USER, "A");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deniesAccessToAnotherUsersRoster() {
        UserEntity owner = new UserEntity();
        owner.setId(99L);
        RosterEntity roster = new RosterEntity();
        roster.setId(5L);
        roster.setUser(owner);
        roster.setName("Other");
        when(rosterRepository.findByIdWithPlayers(5L)).thenReturn(Optional.of(roster));

        assertThrows(ApiException.class, () -> rosterService.get(5L));
    }
}
