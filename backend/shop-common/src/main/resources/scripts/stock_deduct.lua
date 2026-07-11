local key = KEYS[1]
local quantity = tonumber(ARGV[1])
local stock = tonumber(redis.call('GET', key) or 0)
if stock < quantity then
    return -1
end
return redis.call('DECRBY', key, quantity)
