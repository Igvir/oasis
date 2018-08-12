SELECT (
    (SELECT
        COALESCE(ROUND(SUM(points), 2), 0)
    FROM OA_POINTS
    WHERE
        user_id = :userId
        AND
        is_active = 1
    )

    -

    (SELECT
            COALESCE(ROUND(SUM(cost), 2), 0)
        FROM OA_PURCHASE
        WHERE
            user_id = :userId
            AND
            is_active = 1
    )
    ) as Balance

