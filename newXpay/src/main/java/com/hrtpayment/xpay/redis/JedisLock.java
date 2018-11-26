package com.hrtpayment.xpay.redis;

import com.hrtpayment.xpay.utils.exception.BusinessException;

import redis.clients.jedis.ShardedJedis;

public class JedisLock {


	/** 默认超时时间（毫秒） */
	public static final long DEFAULT_TIME_OUT = 1000;  
	
	public static final long expireMsecs =200;
    /** 

     * Acquire lock. 

     *  

     * @param jedis 

     * @return true if lock is acquired, false acquire timeouted 

     * @throws InterruptedException 

     *             in case of thread interruption 

     */  

    public synchronized boolean acquire(ShardedJedis jedis,String key) throws BusinessException {  

        long timeout = DEFAULT_TIME_OUT;  

        while (timeout >= 0) {  

            long expires = System.currentTimeMillis() + expireMsecs + 1;  

            String expiresStr = String.valueOf(expires); //锁到期时间  

      

            if (jedis.setnx(key, expiresStr) == 1) {  

                // lock acquired  

               // locked = true;  

                return true;  

            }  

      

            String currentValueStr = jedis.get(key); //redis里的时间  

            if (currentValueStr != null && Long.parseLong(currentValueStr) < System.currentTimeMillis()) {  

                //判断是否为空，不为空的情况下，如果被其他线程设置了值，则第二个条件判断是过不去的  

                // lock is expired  

      

                String oldValueStr = jedis.getSet(key, expiresStr);  

                //获取上一个锁到期时间，并设置现在的锁到期时间，  

                //只有一个线程才能获取上一个线上的设置时间，因为jedis.getSet是同步的  

                if (oldValueStr != null && oldValueStr.equals(currentValueStr)) {  

                    //如过这个时候，多个线程恰好都到了这里，但是只有一个线程的设置值和当前值相同，他才有权利获取锁  

                    // lock acquired  

                    //locked = true;  

                    return true;  

                }  

            } 

            timeout -= 200;  

            try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				throw new BusinessException(9001, "交易异常！");
			}  

        }  
        return false;  

    }  
    
    
    public synchronized void release(ShardedJedis shardedJedis,String key){
    	
    	shardedJedis.del(key);
    }

}
