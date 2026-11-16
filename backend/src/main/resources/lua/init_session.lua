-- KEYS[1] = {session:<id>}
-- ARGV[1] = ownerUserId
-- ARGV[2] = timestampMs

if redis.call('EXISTS', KEYS[1]) == 1 then
    return { "CONFLICT", "NONE", "SESSION_ALREADY_EXISTS" }
end

redis.call('HSET', KEYS[1],
    'ownerUserId', ARGV[1],
    'currentTurnIndex', '1',
    'committedTurn', '0',
    'committedStatus', 'INITIALIZED',
    'lastFingerprint', '',
    'lastActiveTimestamp', ARGV[2]
)

local pexp = redis.call('PEXPIRE', KEYS[1], 1800000)
if pexp ~= 1 then
    redis.pcall('DEL', KEYS[1])
    return { "ERROR", "NONE", "INITIAL_TTL_FAILED" }
end

return { "COMMITTED", "NONE", "NONE" }

