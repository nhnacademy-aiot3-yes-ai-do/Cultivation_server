local existing = redis.call('HGET', KEYS[2], ARGV[1])
local should_update = existing == false
if existing ~= false then
    should_update = tonumber(existing) == nil or tonumber(existing) < tonumber(ARGV[3])
end
if should_update then
    redis.call('HSET', KEYS[1], ARGV[1], ARGV[2])
    redis.call('HSET', KEYS[2], ARGV[1], ARGV[3])
end
redis.call('EXPIRE', KEYS[1], ARGV[4])
redis.call('EXPIRE', KEYS[2], ARGV[4])
return should_update and 1 or 0
