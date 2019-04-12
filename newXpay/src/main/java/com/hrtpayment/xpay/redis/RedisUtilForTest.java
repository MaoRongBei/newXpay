package com.hrtpayment.xpay.redis;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hrtpayment.xpay.utils.PropertiesUtil;
import com.hrtpayment.xpay.utils.exception.BusinessException;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPool;

public class RedisUtilForTest  extends AbstractRedisCheck {

	private static Log log = LogFactory.getLog(RedisUtilForTest.class);
	
//	private static String redisHost =PropertiesUtil.getProperties("application.properties").getProperty("redis.redishost");
//	private static String redisPwd =PropertiesUtil.getProperties("application.properties").getProperty("redis.redispwd");
//	private static String redisPort =PropertiesUtil.getProperties("application.properties").getProperty("redis.redisport");
	 
	private static String redisHost =PropertiesUtil.getProperties("source/application.properties").getProperty("redis.test.redishost");
	private static String redisPwd =PropertiesUtil.getProperties("source/application.properties").getProperty("redis.test.redispwd");
	private static String redisPort =PropertiesUtil.getProperties("source/application.properties").getProperty("redis.test.redisport");

	
	private static JedisLock jedisLock = new JedisLock();
	static ShardedJedisPool pool;

	static{
	        JedisPoolConfig config =new JedisPoolConfig();//Jedis池配置
	        config.setMaxTotal(200);//最大活动的对象个数
	        config.setMaxIdle(100);//对象最大空闲
	        config.setMinIdle(30);//对象最小空闲
	        config.setMaxWaitMillis(1000 * 3);//获取对象时最大等待时间
	        config.setTestOnBorrow(true);
	        List<JedisShardInfo> jdsInfoList =new ArrayList<JedisShardInfo>(2);
	        JedisShardInfo infoA = new JedisShardInfo(redisHost, redisPort);
	        infoA.setPassword(redisPwd);
	       // JedisShardInfo infoB = new JedisShardInfo(hostB, portB);
	       // infoB.setPassword("admin");
	        jdsInfoList.add(infoA);
	      //  jdsInfoList.add(infoB);
	        pool =new ShardedJedisPool(config, jdsInfoList);
	     }
	    
 
 
 
    
    /**
     * 2019-04-08 新增
     * 
     * 计算当天调用多少次查询操作，并累增耗时时长
     * @return
     * @throws BusinessException
     */
    public static void addCount(String methodName ,Long times) throws BusinessException{
    	ShardedJedis shardedJedis = pool.getResource();
		if(shardedJedis==null ){
			throw new BusinessException(9000,"获取redis客户端异常");
		}
    	Collection<Jedis> collection=shardedJedis.getAllShards();
    	Iterator<Jedis> jedis = collection.iterator();
    	while(jedis.hasNext()){
    		jedis.next().select(4);
    	}
    	boolean broken = false; 
    	int ttl=60*60*24;
		try {
			//是否存在key=mid
			//addCountForQuery
			boolean flag=shardedJedis.exists(methodName);
			if(flag){
				 /**
				  * 存在卡号-金额 键值对 更新
				  */
				long count=Long.valueOf(shardedJedis.get(methodName));
				long thisTime=Long.valueOf(shardedJedis.get(methodName+"Time"));
				shardedJedis.set(methodName, String.valueOf(count+1));
				shardedJedis.set(methodName, String.valueOf(thisTime+times));
			 }else{
				 shardedJedis.set(methodName, "1","NX","EX",Long.parseLong(String.valueOf(ttl)));
			 }  
		} catch (Exception e) {
           log.error(e.getMessage(), e);
           broken = true;
		}finally {
			    if(broken){
		            pool.returnBrokenResource(shardedJedis);
			    }else{
		            pool.returnResource(shardedJedis);
			    }
		} 
    }
}
