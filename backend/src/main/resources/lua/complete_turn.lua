-- KEYS[1] = {session:<id>}
-- KEYS[2] = {session:<id>}:lock:<turnIndex>
-- KEYS[3] = {session:<id>}:result:<turnIndex>
-- ARGV[1] = ownerUserId
-- ARGV[2] = turnIndex
-- ARGV[3] = requestFingerprint
-- ARGV[4] = workerToken
-- ARGV[5] = resultJsonPayload
-- ARGV[6] = timestampMs

-- ============================================================================
-- 1. Session Existence & Data Type Verification
-- ============================================================================
if redis.call('EXISTS', KEYS[1]) == 0 then
    return { "REJECTED", "NONE", "SESSION_NOT_FOUND" }
end

if redis.call('TYPE', KEYS[1]).ok ~= 'hash' then
    return { "REJECTED", "NONE", "SESSION_CORRUPTED_TYPE" }
end

-- ============================================================================
-- 2. Read Required Session Fields
-- ============================================================================
local owner = redis.call('HGET', KEYS[1], 'ownerUserId')
local currTurnStr = redis.call('HGET', KEYS[1], 'currentTurnIndex')
local commTurnStr = redis.call('HGET', KEYS[1], 'committedTurn')
local commStatus = redis.call('HGET', KEYS[1], 'committedStatus')

if not owner or not currTurnStr or not commTurnStr or not commStatus then
    return { "REJECTED", "NONE", "SESSION_CORRUPTED_MISSING_FIELDS" }
end

-- ============================================================================
-- 3. Authenticated Owner UID Validation (BEFORE any mutation)
-- ============================================================================
if owner ~= ARGV[1] then
    return { "REJECTED", "NONE", "SESSION_OWNER_CONFLICT" }
end

-- ============================================================================
-- 4. Turn Bounds & State Continuity Invariant Validation
-- ============================================================================
local currTurn = tonumber(currTurnStr)
local commTurn = tonumber(commTurnStr)
local reqTurn = tonumber(ARGV[2])

if not currTurn or not commTurn or not reqTurn then
    return { "REJECTED", "NONE", "SESSION_CORRUPTED_NON_NUMERIC" }
end

if reqTurn < 1 or reqTurn > 2147483646 then
    return { "REJECTED", "NONE", "REQUESTED_TURN_OUT_OF_BOUNDS" }
end

local isValidState = false
if commTurn == 0 and currTurn == 1 and commStatus == 'INITIALIZED' then
    isValidState = true
elseif commTurn >= 1 and commTurn <= 2147483645 and currTurn == (commTurn + 1) and commStatus == 'COMMITTED' then
    isValidState = true
elseif commTurn == 2147483646 and currTurn == 2147483647 and commStatus == 'TERMINAL' then
    isValidState = true
end

if not isValidState then
    return { "REJECTED", "NONE", "SESSION_STATE_CORRUPTED" }
end

-- ============================================================================
-- 5. Argument Validation
-- ============================================================================
if not ARGV[4] or string.len(ARGV[4]) ~= 32 or not string.match(ARGV[4], '^[a-f0-9]+$') then
    return { "ERROR", "NONE", "ARGUMENT_MALFORMED_WORKER_TOKEN" }
end

if not ARGV[3] or string.len(ARGV[3]) ~= 64 or not string.match(ARGV[3], '^[a-f0-9]+$') then
    return { "ERROR", "NONE", "ARGUMENT_MALFORMED_FINGERPRINT" }
end

if not ARGV[5] or string.len(ARGV[5]) == 0 or string.match(ARGV[5], '^%s*$') then
    return { "ERROR", "NONE", "ARGUMENT_MALFORMED_EMPTY_PAYLOAD" }
end

if string.len(ARGV[5]) > 65536 then
    return { "ERROR", "NONE", "ARGUMENT_MALFORMED_PAYLOAD_TOO_LARGE" }
end

if not ARGV[6] or not string.match(ARGV[6], '^[1-9][0-9]*$') then
    return { "ERROR", "NONE", "ARGUMENT_MALFORMED_TIMESTAMP" }
end

-- ============================================================================
-- 6. Idempotent Retry Evaluation (PRIOR to Terminal Check or Lock Validation)
-- ============================================================================
if commTurn >= reqTurn then
    if redis.call('EXISTS', KEYS[3]) == 1 then
        if redis.call('TYPE', KEYS[3]).ok ~= 'hash' then
            return { "ERROR", "NONE", "RESULT_CORRUPTED_TYPE" }
        end
        
        local storedFp = redis.call('HGET', KEYS[3], 'fp')
        local cachedData = redis.call('HGET', KEYS[3], 'data')
        local storedTs = redis.call('HGET', KEYS[3], 'ts')
        
        -- Complete Result Hash Validation (Identical to acquire_lock.lua)
        if not storedFp or not cachedData or not storedTs then
            return { "ERROR", "NONE", "RESULT_CORRUPTED_MISSING_FIELDS" }
        end
        if string.len(storedFp) ~= 64 or not string.match(storedFp, '^[a-f0-9]+$') then
            return { "ERROR", "NONE", "RESULT_CORRUPTED_MALFORMED_FP" }
        end
        if string.len(cachedData) == 0 or string.match(cachedData, '^%s*$') then
            return { "ERROR", "NONE", "RESULT_CORRUPTED_EMPTY_DATA" }
        end
        if string.len(cachedData) > 65536 then
            return { "ERROR", "NONE", "RESULT_CORRUPTED_DATA_TOO_LARGE" }
        end
        if not string.match(storedTs, '^[1-9][0-9]*$') then
            return { "ERROR", "NONE", "RESULT_CORRUPTED_INVALID_TS" }
        end
        
        if storedFp == ARGV[3] then
            -- Safe Stale-Lock Cleanup (Identical to acquire_lock.lua)
            if redis.call('EXISTS', KEYS[2]) == 1 then
                if redis.call('TYPE', KEYS[2]).ok ~= 'hash' then
                    return { "ERROR", "NONE", "LOCK_CORRUPTED_TYPE" }
                end
                local lToken = redis.call('HGET', KEYS[2], 'token')
                local lFp = redis.call('HGET', KEYS[2], 'fp')
                local lTs = redis.call('HGET', KEYS[2], 'ts')
                
                if not lToken or not lFp or not lTs or lToken == '' or lFp == '' or lTs == '' then
                    return { "ERROR", "NONE", "LOCK_CORRUPTED_MISSING_FIELDS" }
                end
                if string.len(lToken) ~= 32 or not string.match(lToken, '^[a-f0-9]+$')
                   or string.len(lFp) ~= 64 or not string.match(lFp, '^[a-f0-9]+$')
                   or not string.match(lTs, '^[1-9][0-9]*$') then
                    return { "ERROR", "NONE", "LOCK_CORRUPTED_MALFORMED_FIELDS" }
                end
                
                if lFp == storedFp then
                    redis.call('DEL', KEYS[2])
                else
                    return { "CONFLICT", "NONE", "LOCK_FINGERPRINT_MISMATCH" }
                end
            end
            return { "RESULT_CACHED", cachedData, "NONE" }
        else
            return { "CONFLICT", "NONE", "IDEMPOTENCY_FINGERPRINT_MISMATCH" }
        end
    else
        return { "EXPIRED", "NONE", "TURN_RESULT_EXPIRED_410_GONE" }
    end
end

-- ============================================================================
-- 7. Terminal Session Check (Applied ONLY to New Turn Requests)
-- ============================================================================
if commStatus == 'TERMINAL' or currTurn >= 2147483647 then
    return { "REJECTED", "NONE", "SESSION_TERMINAL_CLOSED" }
end

-- ============================================================================
-- 8. Turn Sequence Validation for New Turn
-- ============================================================================
if reqTurn ~= currTurn or commTurn ~= (reqTurn - 1) then
    return { "REJECTED", "NONE", "TURN_OUT_OF_SEQUENCE" }
end

-- ============================================================================
-- 9. Active Lock Validation & Ownership Check
-- ============================================================================
if redis.call('EXISTS', KEYS[2]) == 0 then
    return { "REJECTED", "NONE", "LOCK_NOT_FOUND" }
end

if redis.call('TYPE', KEYS[2]).ok ~= 'hash' then
    return { "ERROR", "NONE", "LOCK_CORRUPTED_TYPE" }
end

local curToken = redis.call('HGET', KEYS[2], 'token')
local curFp = redis.call('HGET', KEYS[2], 'fp')
local curTs = redis.call('HGET', KEYS[2], 'ts')

if not curToken or not curFp or not curTs or curToken == '' or curFp == '' or curTs == '' then
    return { "ERROR", "NONE", "LOCK_CORRUPTED_MISSING_FIELDS" }
end

if string.len(curToken) ~= 32 or not string.match(curToken, '^[a-f0-9]+$')
   or string.len(curFp) ~= 64 or not string.match(curFp, '^[a-f0-9]+$')
   or not string.match(curTs, '^[1-9][0-9]*$') then
    return { "ERROR", "NONE", "LOCK_CORRUPTED_MALFORMED_FIELDS" }
end

if curToken ~= ARGV[4] or curFp ~= ARGV[3] then
    return { "CONFLICT", "NONE", "LOCK_OWNERSHIP_LOST" }
end

-- ============================================================================
-- 10. Result Staging & Atomic Commit
-- ============================================================================
redis.call('HSET', KEYS[3], 'fp', ARGV[3], 'data', ARGV[5], 'ts', ARGV[6])
local expRes = redis.call('EXPIRE', KEYS[3], 900)
if expRes ~= 1 then
    redis.pcall('DEL', KEYS[3])
    return { "ERROR", "NONE", "RESULT_STAGING_FAILED" }
end

local nextCommTurn = reqTurn
local nextCurrTurn
local nextStatus
if reqTurn == 2147483646 then
    nextCurrTurn = 2147483647
    nextStatus = 'TERMINAL'
else
    nextCurrTurn = reqTurn + 1
    nextStatus = 'COMMITTED'
end

redis.call('HSET', KEYS[1],
    'committedTurn', tostring(nextCommTurn),
    'currentTurnIndex', tostring(nextCurrTurn),
    'committedStatus', nextStatus,
    'lastFingerprint', ARGV[3],
    'lastActiveTimestamp', ARGV[6]
)

-- ============================================================================
-- 11. Post-Commit Maintenance
-- ============================================================================
redis.pcall('DEL', KEYS[2])

local rep = redis.call('PEXPIRE', KEYS[1], 1800000)
if rep ~= 1 then
    return { "COMPLETED_WARNING", ARGV[5], "SESSION_TTL_REFRESH_FAILED" }
end

return { "COMMITTED", ARGV[5], "NONE" }

