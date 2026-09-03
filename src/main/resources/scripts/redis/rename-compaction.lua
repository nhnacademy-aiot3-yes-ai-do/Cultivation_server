if KEYS[5] and KEYS[5] ~= '' and redis.call('GET', KEYS[5]) ~= ARGV[1] then
    return 0
end
redis.call('RENAME', KEYS[4], KEYS[2])
redis.call('RENAME', KEYS[3], KEYS[1])
return 1
