-- KEYS[1] = {session:<id>}:lock:<turnIndex>
-- ARGV[1] = workerToken (32 hex characters)
-- ARGV[2] = requestFingerprint (64 hex characters)
-- ARGV[3] = leaseMs (must be exact canonical string "15000")
-- ARGV[4] = timestampMs (positive decimal integer)

-- ============================================================================
-- 1. Input Validation (BEFORE any Redis read or write)
-- ============================================================================
if ARGV[3] ~= "15000" then
    return { "ERROR", "NONE", "INVALID_LEASE_DURATION" }
end

if not ARGV[1] or string.len(ARGV[1]) ~= 32 or not string.match(ARGV[1], '^[a-f0-9]+$') then
    return { "ERROR", "NONE", "ARGUMENT_MALFORMED_WORKER_TOKEN" }
end

if not ARGV[2] or string.len(ARGV[2]) ~= 64 or not string.match(ARGV[2], '^[a-f0-9]+$') then
    return { "ERROR", "NONE", "ARGUMENT_MALFORMED_FINGERPRINT" }
end

if not ARGV[4] or not string.match(ARGV[4], '^[1-9][0-9]*$') then
    return { "ERROR", "NONE", "ARGUMENT_MALFORMED_TIMESTAMP" }
end

-- ============================================================================
-- 2. Lock Existence and Data Type Verification
-- ============================================================================
if redis.call('EXISTS', KEYS[1]) == 0 then
    return { "REJECTED", "NONE", "LOCK_NOT_FOUND" }
end

if redis.call('TYPE', KEYS[1]).ok ~= 'hash' then
    return { "ERROR", "NONE", "LOCK_CORRUPTED_TYPE" }
end

-- ============================================================================
-- 3. Read and Validate Stored Lock Fields
-- ============================================================================
local curToken = redis.call('HGET', KEYS[1], 'token')
local curFp = redis.call('HGET', KEYS[1], 'fp')
local curTs = redis.call('HGET', KEYS[1], 'ts')

if not curToken or not curFp or not curTs or curToken == '' or curFp == '' or curTs == '' then
    return { "ERROR", "NONE", "LOCK_CORRUPTED_MISSING_FIELDS" }
end

if string.len(curToken) ~= 32 or not string.match(curToken, '^[a-f0-9]+$')
   or string.len(curFp) ~= 64 or not string.match(curFp, '^[a-f0-9]+$')
   or not string.match(curTs, '^[1-9][0-9]*$') then
    return { "ERROR", "NONE", "LOCK_CORRUPTED_MALFORMED_FIELDS" }
end

-- ============================================================================
-- 4. Lock Ownership & Association Check
-- ============================================================================
if curToken ~= ARGV[1] or curFp ~= ARGV[2] then
    return { "CONFLICT", "NONE", "LOCK_OWNERSHIP_LOST" }
end

-- ============================================================================
-- 5. Atomic Lease Renewal and Timestamp Update
-- ============================================================================
redis.call('HSET', KEYS[1], 'ts', ARGV[4])
local rexp = redis.call('PEXPIRE', KEYS[1], 15000)

if rexp == 1 then
    return { "COMMITTED", "NONE", "NONE" }
else
    return { "ERROR", "NONE", "LOCK_LEASE_FAILED" }
end

