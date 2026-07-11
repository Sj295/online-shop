redis.replicate_commands()
local key = KEYS[1]
local window = tonumber(ARGV[1])
local limit = tonumber(ARGV[2])
local now = redis.call('TIME')
local nowMicro = tonumber(now[1]) * 1000000 + tonumber(now[2])
local windowStart = nowMicro - window * 1000000

redis.call('ZREMRANGEBYSCORE', key, 0, windowStart)
local count = redis.call('ZCARD', key)
if count < limit then
    redis.call('ZADD', key, nowMicro, nowMicro)
    redis.call('EXPIRE', key, window)
    return 1
else
    return 0
end
