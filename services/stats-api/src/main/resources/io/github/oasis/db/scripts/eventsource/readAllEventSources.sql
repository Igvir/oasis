SELECT
    id,
    token,
    display_name AS name,
    is_active AS active
FROM
    OA_EVENT_SOURCE
WHERE
    is_active = true