<if(hasTeam||hasUser||isTopN||isBottomN)>
SELECT
    *
FROM
(
<endif>

    SELECT
        tbl.user_id AS userId,
        tbl.team_id AS teamId,
        tbl.team_scope_id AS teamScopeId,
        tbl.totalPoints AS totalPoints,
        (RANK() over (PARTITION BY tbl.team_id ORDER BY tbl.totalPoints DESC)) AS 'rankTeam',
        (RANK() over (PARTITION BY tbl.team_scope_id ORDER BY tbl.totalPoints DESC)) AS 'rankTeamScope',
        (LAG(tbl.totalPoints) over (PARTITION BY tbl.team_id ORDER BY tbl.totalPoints DESC)) AS 'nextRankVal',
        (LAG(tbl.totalPoints) over (PARTITION BY tbl.team_scope_id ORDER BY tbl.totalPoints DESC)) AS 'nextTeamScopeRankVal',
        UNIX_TIMESTAMP(NOW()) * 1000 AS calculatedTime
    FROM
    (
        SELECT
            user_id,
            team_scope_id,
            team_id,
            ROUND(<aggType>(points), 2) AS totalPoints
        FROM OA_POINT
        WHERE
            is_active = 1
            AND
            team_scope_id = :teamScopeId
            <if(hasTimeRange)>
            AND ts >= :rangeStart
            AND ts \< :rangeEnd
            <endif>

            <if(hasInclusions)>
            AND point_id IN \<ruleIds>
            <endif>
            <if(hasExclusions)>
            AND point_id NOT IN \<excludeRuleIds>
            <endif>
        GROUP BY
            team_id,
            team_scope_id,
            user_id
    ) tbl

<if(hasTeam||hasUser||isTopN||isBottomN)>
) rankTbl

WHERE
    1 = 1
<if(hasTeam)>
    AND rankTbl.teamId = :teamId
<endif>
<if(hasUser)>
    AND rankTbl.userId = :userId
<endif>

<if(isTopN)>
    <if(hasTeam)>
    ORDER BY rankTbl.rankTeam ASC
    <else>
    ORDER BY rankTbl.rankTeamScope ASC
    <endif>
LIMIT :topN
<endif>

<if(isBottomN)>
    <if(hasTeam)>
    ORDER BY rankTbl.rankTeam DESC
    <else>
    ORDER BY rankTbl.rankTeamScope DESC
    <endif>
LIMIT :bottomN
<endif>

<endif>