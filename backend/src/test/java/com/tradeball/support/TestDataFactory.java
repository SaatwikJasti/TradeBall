package com.tradeball.support;

import com.tradeball.entity.PlayerEntity;
import com.tradeball.entity.PlayerStatsEntity;
import com.tradeball.repository.PlayerRepository;
import com.tradeball.repository.PlayerStatsRepository;

public final class TestDataFactory {

    private TestDataFactory() {
    }

    public static PlayerEntity player(PlayerRepository players, String externalId, String first, String last,
                                      String pos, String team, int age) {
        PlayerEntity p = new PlayerEntity();
        p.setExternalId(externalId);
        p.setFirstName(first);
        p.setLastName(last);
        p.setPosition(pos);
        p.setTeam(team);
        p.setAge(age);
        p.setActive(true);
        return players.save(p);
    }

    public static PlayerStatsEntity stats(PlayerStatsRepository repo, PlayerEntity player, int season,
                                          double pts, double reb, double ast, int gp) {
        PlayerStatsEntity s = new PlayerStatsEntity();
        s.setPlayer(player);
        s.setSeason(season);
        s.setGamesPlayed(gp);
        s.setPoints(pts);
        s.setRebounds(reb);
        s.setAssists(ast);
        s.setSteals(1.0);
        s.setBlocks(1.0);
        s.setThreePointers(2.0);
        s.setFieldGoalPercentage(48.0);
        s.setFreeThrowPercentage(80.0);
        s.setTurnovers(2.0);
        return repo.save(s);
    }
}
