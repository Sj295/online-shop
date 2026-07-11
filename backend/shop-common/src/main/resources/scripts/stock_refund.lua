local key = KEYS[1]
local quantity = tonumber(ARGV[1])
return redis.call('INCRBY', key, quantity)
