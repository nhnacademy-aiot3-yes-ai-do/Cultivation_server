redis.call('RENAME', KEYS[4], KEYS[2])
redis.call('RENAME', KEYS[3], KEYS[1])
return 1
