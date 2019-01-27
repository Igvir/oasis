package io.github.isuru.oasis.services.services.scheduler;

import io.github.isuru.oasis.model.DefaultEntities;
import io.github.isuru.oasis.model.collect.Pair;
import io.github.isuru.oasis.model.defs.LeaderboardDef;
import io.github.isuru.oasis.model.defs.RaceDef;
import io.github.isuru.oasis.model.defs.ScopingType;
import io.github.isuru.oasis.services.dto.game.GlobalLeaderboardRecordDto;
import io.github.isuru.oasis.services.dto.game.LeaderboardRequestDto;
import io.github.isuru.oasis.services.dto.game.TeamLeaderboardRecordDto;
import io.github.isuru.oasis.services.dto.stats.UserCountStat;
import io.github.isuru.oasis.services.model.RaceWinRecord;
import io.github.isuru.oasis.services.model.UserTeam;
import io.github.isuru.oasis.services.services.IGameDefService;
import io.github.isuru.oasis.services.services.IGameService;
import io.github.isuru.oasis.services.services.IProfileService;
import io.github.isuru.oasis.services.utils.Commons;
import org.mvel2.MVEL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class BaseScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(BaseScheduler.class);

    protected abstract Pair<Long, Long> deriveTimeRange(long ms, ZoneId zoneId);

    protected Map<Long, List<RaceWinRecord>> runForGame(IProfileService profileService,
                              IGameDefService gameDefService,
                              IGameService gameService,
                              long gameId, long awardedAt) {

        Map<Long, List<RaceWinRecord>> winnersByRace = new HashMap<>();

        try {
            Map<Long, Long> teamCountMap = loadTeamStatus(profileService);
            Map<Long, Long> teamScopeCountMap = loadTeamScopeStatus(profileService);

            List<RaceDef> raceDefList = readRaces(gameDefService, gameId, "weekly");
            LOG.info(" #{} race(s) found for game #{}", raceDefList.size(), gameId);
            for (RaceDef raceDef : raceDefList) {
                LeaderboardDef lb = gameDefService.readLeaderboardDef(raceDef.getLeaderboardId());
                if (lb == null) {
                    LOG.warn("No leaderboard is found by referenced id '{}' in race definition '{}'!",
                            raceDef.getLeaderboardId(), raceDef.getId());
                    continue;
                }

                ScopingType scopingType = ScopingType.from(raceDef.getFromScope());

                Pair<Long, Long> timeRange = deriveTimeRange(awardedAt, ZoneId.systemDefault());

                Serializable expr = !Commons.isNullOrEmpty(raceDef.getRankPointsExpression())
                        ? MVEL.compileExpression(raceDef.getRankPointsExpression())
                        : null;

                LeaderboardRequestDto requestDto = new LeaderboardRequestDto(timeRange.getValue0(), timeRange.getValue1());
                requestDto.setLeaderboardDef(lb);
                requestDto.setTopThreshold(raceDef.getTop());


//                Map<String, Object> templateData = new HashMap<>();
//                templateData.put("hasUser", false);
//                templateData.put("hasTeam", false);
//                templateData.put("hasTimeRange", true);
//                templateData.put("hasInclusions", lb.getRuleIds() != null && !lb.getRuleIds().isEmpty());
//                templateData.put("hasExclusions", lb.getExcludeRuleIds() != null && !lb.getExcludeRuleIds().isEmpty());
//                templateData.put("isTopN", false);
//                templateData.put("isBottomN", false);
//                templateData.put("hasStates", lb.hasStates());
//                templateData.put("onlyFinalTops", true);
//
//
//                Map<String, Object> data = new HashMap<>();
//                data.put("rangeStart", timeRange.getValue0());
//                data.put("rangeEnd", timeRange.getValue1());
//                data.put("topN", raceDef.getTop());
//                data.put("ruleIds", lb.getRuleIds());
//                data.put("aggType", lb.getAggregatorType());
//                data.put("topThreshold", raceDef.getTop());

                // @TODO consider sending conditional statement as well

                LOG.info(" - Executing leaderboard for race #{} between @[{}, {}]...",
                        raceDef.getId(), timeRange.getValue0(), timeRange.getValue1());

                List<RaceWinRecord> winners;
                if (scopingType == ScopingType.TEAM || scopingType == ScopingType.TEAM_SCOPE) {
                    List<TeamLeaderboardRecordDto> recordOrder = gameService.readTeamLeaderboard(requestDto);
                    winners = deriveTeamWinners(recordOrder, scopingType, raceDef, teamCountMap, teamScopeCountMap);
                } else {
                    List<GlobalLeaderboardRecordDto> recordOrder = gameService.readGlobalLeaderboard(requestDto);
                    winners = deriveGlobalWinners(profileService, recordOrder);
                }

                // append other information to the record
                winners.forEach(record -> {
                    record.setGameId(gameId);
                    record.setRaceStartAt(timeRange.getValue0());
                    record.setRaceEndAt(timeRange.getValue1());
                    record.setRaceId(raceDef.getId());
                    record.setAwardedAt(awardedAt);

                    calculateAwardPoints(record, raceDef, expr);
                });

                // insert winners to database
                winnersByRace.put(raceDef.getId(), winners);
            }

        } catch (Exception e) {
            LOG.error("Error while reading race definitions from database!", e);
        }
        return winnersByRace;
    }

    private void calculateAwardPoints(RaceWinRecord winner, RaceDef raceDef, Serializable expr) {
        if (expr != null) {
            Map<String, Object> vars = new HashMap<>();
            vars.put("$rank", winner.getRank());
            vars.put("$points", winner.getPoints());
            vars.put("$count", winner.getTotalCount());
            vars.put("$winner", winner);

            double awardPoints = Commons.asDouble(MVEL.executeExpression(expr, vars));
            if (awardPoints == Double.NaN) {
                awardPoints = DefaultEntities.DEFAULT_RACE_WIN_VALUE;
            }
            winner.setAwardedPoints(awardPoints);

        } else {
            winner.setAwardedPoints(raceDef.getRankPoints().getOrDefault(winner.getRank(),
                    DefaultEntities.DEFAULT_RACE_WIN_VALUE));
        }
    }

    private List<RaceWinRecord> deriveGlobalWinners(IProfileService profileService,
                                                    List<GlobalLeaderboardRecordDto> recordOrder) throws Exception {
        List<RaceWinRecord> winners = new LinkedList<>();
        for (GlobalLeaderboardRecordDto row : recordOrder) {
            RaceWinRecord winnerRecord = new RaceWinRecord();

            long userId = row.getUserId();
            winnerRecord.setUserId(userId);
            winnerRecord.setPoints(row.getTotalPoints());
            winnerRecord.setTotalCount(row.getTotalCount());

            UserTeam currentTeamOfUser = profileService.findCurrentTeamOfUser(userId);
            winnerRecord.setTeamId(currentTeamOfUser.getTeamId());
            winnerRecord.setTeamScopeId(currentTeamOfUser.getScopeId());
            winnerRecord.setRank(row.getRankGlobal());
            winners.add(winnerRecord);
        }
        return winners;
    }

    private List<RaceWinRecord> deriveTeamWinners(List<TeamLeaderboardRecordDto> recordOrder,
                                                        ScopingType scopingType,
                                                        RaceDef raceDef,
                                                        Map<Long, Long> teamCountMap,
                                                        Map<Long, Long> teamScopeCountMap) {
        List<RaceWinRecord> winners = new LinkedList<>();
        for (TeamLeaderboardRecordDto row : recordOrder) {
            RaceWinRecord winnerRecord = new RaceWinRecord();

            long userId = row.getUserId();
            int rank;

            winnerRecord.setUserId(userId);
            winnerRecord.setPoints(row.getTotalPoints());
            winnerRecord.setTotalCount(row.getTotalCount());

            if (scopingType == ScopingType.TEAM_SCOPE) {
                long teamScopeId = row.getTeamScopeId();
                rank = row.getRankInTeamScope();

                long playerCount = teamScopeCountMap.get(teamScopeId);
                if (playerCount == 1) {
                    continue;
                } else if (playerCount < raceDef.getTop() && rank != 1) {
                    continue;
                }

                winnerRecord.setTeamId(row.getTeamId().intValue());
                winnerRecord.setTeamId(row.getTeamScopeId().intValue());

            } else {
                Long teamId = row.getTeamId();
                rank = row.getRankInTeam();

                long playerCount = teamCountMap.get(teamId);
                if (playerCount <= 1) { // no awards. skip.
                    continue;
                } else if (playerCount < raceDef.getTop() && rank != 1) {   // @TODO review
                    // only the top will be awarded points
                    continue;
                }

                winnerRecord.setTeamId(teamId.intValue());
                winnerRecord.setTeamScopeId(row.getTeamScopeId().intValue());
            }
            winners.add(winnerRecord);
        }
        return winners;
    }


    private Map<Long, Long> loadTeamStatus(IProfileService profileService) throws Exception {
        List<UserCountStat> teamList = profileService.listUserCountInTeams();
        Map<Long, Long> teamCounts = new HashMap<>();
        for (UserCountStat statusStat : teamList) {
            teamCounts.put(statusStat.getId(), statusStat.getTotalUsers());
        }
        return teamCounts;
    }

    private Map<Long, Long> loadTeamScopeStatus(IProfileService profileService) throws Exception {
        List<UserCountStat> teamList = profileService.listUserCountInTeamScopes();
        Map<Long, Long> teamScopeCounts = new HashMap<>();
        for (UserCountStat statusStat : teamList) {
            teamScopeCounts.put(statusStat.getId(), statusStat.getTotalUsers());
        }
        return teamScopeCounts;
    }

    private List<RaceDef> readRaces(IGameDefService gameDefService, long gameId, String timePeriod) throws Exception {
        return gameDefService.listRaces(gameId).stream()
                .filter(r -> timePeriod.equals(r.getTimeWindow()))
                .collect(Collectors.toList());
    }

}