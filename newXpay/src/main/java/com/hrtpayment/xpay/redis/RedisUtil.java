package com.hrtpayment.xpay.redis;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hrtpayment.xpay.utils.PropertiesUtil;
import com.hrtpayment.xpay.utils.exception.BusinessException;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPool;

public class RedisUtil  extends AbstractRedisCheck {

	private static Log log = LogFactory.getLog(RedisUtil.class);
	
	private static String redisHost =PropertiesUtil.getProperties("application.properties").getProperty("redis.redishost");
	private static String redisPwd =PropertiesUtil.getProperties("application.properties").getProperty("redis.redispwd");
	private static String redisPort =PropertiesUtil.getProperties("application.properties").getProperty("redis.redisport");
	
	
	private static JedisLock jedisLock = new JedisLock();
	static ShardedJedisPool pool;

	static{
	        JedisPoolConfig config =new JedisPoolConfig();//Jedis池配置
	        config.setMaxIdle(200);//最大活动的对象个数
	        config.setMaxIdle(100);//对象最大空闲
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
	    
	    
		public static void addRiskAmt(String key ,Double payAmt ,String toactacn,Double daySumAmt) throws BusinessException{
			
//			if(!getRedisFlag())
//				return ;
			ShardedJedis shardedJedis = pool.getResource();
			if(shardedJedis==null ){
				throw new BusinessException(9000,"获取redis客户端异常");
			}
	    	Collection<Jedis> collection=shardedJedis.getAllShards();
	    	Iterator<Jedis> jedis = collection.iterator();
	    	while(jedis.hasNext()){
	    		jedis.next().select(0);
	    	}
			 boolean broken = false;
			// JedisLock jedisLock = new JedisLock();
	        try {
	        	long flag=shardedJedis.hsetnx(key, toactacn, payAmt.toString());
	   		 	if(flag==0){
						 boolean lockFlag= jedisLock.acquire(shardedJedis, toactacn);
						 if(lockFlag){
							 /**
							  * 存在卡号-金额 键值对 更新
							  */
							 String amt=shardedJedis.hget(key, toactacn);
							 if(amt!=null && !"".equals(amt)){
									BigDecimal bd = new BigDecimal(amt);
						            BigDecimal bd2 = new BigDecimal(payAmt);
						            Double result=bd.add(bd2).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
						            if(result>daySumAmt){
						            	throw new BusinessException(9001, "单日限额不允许交易！");
						            }
								 shardedJedis.hset(key, toactacn, String.valueOf(result));
							 }else{
								 /**
								  * 存在卡号-金额 但是value为空
								  */
								 shardedJedis.hset(key, toactacn, payAmt.toString());
							 } 
							 // 释放锁
							 jedisLock.release(shardedJedis, toactacn);
						 }
			 }
	        } catch (BusinessException e) {
	            log.info("商户号-"+toactacn+"---->"+e.getCode()+":"+e.getMessage());
	            broken = true;
	            throw new BusinessException(e.getCode(), e.getMessage());
	        } finally {
			    if (jedisLock != null) {
			        try {  
			        	jedisLock.release(shardedJedis, toactacn);// 则解锁  
			        } catch (Exception e) {  
			        	
			        }
			    }
			    if(broken){
		            pool.returnBrokenResource(shardedJedis);
			    }else{
		            pool.returnResource(shardedJedis);
			    }
	            
	        }
			 
		}
		
		/**
		 * 立码富专用 限额校验
		 * @param key
		 * @param payAmt
		 * @param toactacn
		 * @param daySumAmt
		 * @throws BusinessException
		 */
		public static void checkRiskAmtForLMF(String key ,Double payAmt ,String toactacn,Double daySumAmt)
				throws BusinessException{
			ShardedJedis shardedJedis = pool.getResource();
			if(shardedJedis==null ){
				throw new BusinessException(9000,"获取redis客户端异常");
			}
	    	Collection<Jedis> collection=shardedJedis.getAllShards();
	    	Iterator<Jedis> jedis = collection.iterator();
	    	while(jedis.hasNext()){
	    		jedis.next().select(0);
	    	}
			 boolean broken = false;
	        try { 
				String amt=shardedJedis.hget(key, toactacn);
				if(amt!=null && !"".equals(amt)){
					BigDecimal bd = new BigDecimal(amt);
					BigDecimal bd2 = new BigDecimal(payAmt);
					Double result=bd.add(bd2).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
					if(result>daySumAmt){
					     throw new BusinessException(9001, "单日限额不允许交易！");
					}
				}
	        } catch (BusinessException e) {
	            log.info("商户号-"+toactacn+"---->"+e.getCode()+":"+e.getMessage());
	            broken = true;
	            throw new BusinessException(e.getCode(), e.getMessage());
	        } finally {
			    if(broken){
		            pool.returnBrokenResource(shardedJedis);
			    }else{
		            pool.returnResource(shardedJedis);
			    }
	            
	        }
				 
		}
		
	 /**
	  * 立码富 专用 金额累增方法
	  * @param key
	  * @param payAmt
	  * @param toactacn
	  * @throws BusinessException
	  */
      public static void addRiskAmtForLMF(String key ,Double payAmt ,String toactacn) throws BusinessException{
			ShardedJedis shardedJedis = pool.getResource();
			if(shardedJedis==null ){
				throw new BusinessException(9000,"获取redis客户端异常");
			}
	    	Collection<Jedis> collection=shardedJedis.getAllShards();
	    	Iterator<Jedis> jedis = collection.iterator();
	    	while(jedis.hasNext()){
	    		jedis.next().select(0);
	    	}
			 boolean broken = false;
	        try {
	        	long flag=shardedJedis.hsetnx(key, toactacn, payAmt.toString());
	   		 	if(flag==0){
						 boolean lockFlag= jedisLock.acquire(shardedJedis, toactacn);
						 if(lockFlag){
							 /**
							  * 存在卡号-金额 键值对 更新
							  */
							 String amt=shardedJedis.hget(key, toactacn);
							 if(amt!=null && !"".equals(amt)){
									BigDecimal bd = new BigDecimal(amt);
						            BigDecimal bd2 = new BigDecimal(payAmt);
						            Double result=bd.add(bd2).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
								    shardedJedis.hset(key, toactacn, String.valueOf(result));
							 }else{
								 /**
								  * 存在卡号-金额 但是value为空
								  */
								 shardedJedis.hset(key, toactacn, payAmt.toString());
							 } 
							 // 释放锁
							 jedisLock.release(shardedJedis, toactacn);
						 }
			 }
	        } catch (BusinessException e) {
	            log.info("商户号-"+toactacn+"---->"+e.getCode()+":"+e.getMessage());
	            broken = true;
	        } finally {
			    if (jedisLock != null) {
			        try {  
			        	jedisLock.release(shardedJedis, toactacn);// 则解锁  
			        } catch (Exception e) {  
			        	
			        }
			    }
			    if(broken){
		            pool.returnBrokenResource(shardedJedis);
			    }else{
		            pool.returnResource(shardedJedis);
			    }
	            
	        }
			 
		}

      
		public static boolean cutRiskAmt(Double payAmt ,String toactacn) throws BusinessException{
			
			if(!getRedisFlag())
				return true;
			
			ShardedJedis shardedJedis = pool.getResource();
			if(shardedJedis==null ){
				throw new BusinessException(9000,"获取redis客户端异常");
			}
	    	Collection<Jedis> collection=shardedJedis.getAllShards();
	    	Iterator<Jedis> jedis = collection.iterator();
	    	while(jedis.hasNext()){
	    		jedis.next().select(0);
	    	}
			boolean rtnFlag=false;
			boolean broken = false;
			 //JedisLock jedisLock = new JedisLock();
			try {
				boolean flag=shardedJedis.hexists("limitAmt", toactacn);
				 if(flag){
						 boolean lockFlag= jedisLock.acquire(shardedJedis, toactacn);
						 if(lockFlag){
							 /**
							  * 存在卡号-金额 键值对 更新
							  */
							 String amt=shardedJedis.hget("limitAmt", toactacn);
							 log.info("redis中存在"+toactacn+"该mid,余额为："+amt+" 增加金额："+payAmt);
							 if(amt!=null && !"".equals(amt)){
					              BigDecimal bd = new BigDecimal(amt);
					              BigDecimal bd2 = new BigDecimal(payAmt);
					              Double result=bd.subtract(bd2).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
					              if(result>=0){
					            	  shardedJedis.hset("limitAmt", toactacn, result.toString());
					            	  rtnFlag=true;
					              }
							 }
							 
							 // 释放锁
							 jedisLock.release(shardedJedis, toactacn);
//							 foo=false;
						 }
//						 else{
//							 Thread.sleep(2000);
//						 } 
//					 }
				 }
			} catch (Exception e) {
	            log.error(e.getMessage(), e);
	            broken = true;
			}finally {
				 if (jedisLock != null) {
				      try {  
				    	  jedisLock.release(shardedJedis, toactacn);// 则解锁  
				      } catch (Exception e) {  
				        	
				      }
				    }
				    if(broken){
			            pool.returnBrokenResource(shardedJedis);
				    }else{
			            pool.returnResource(shardedJedis);
				    }
			}
			return rtnFlag;	 
		}
		
		public static boolean getRedisFlag() throws BusinessException{
			ShardedJedis shardedJedis = pool.getResource();
			if(shardedJedis==null ){
				throw new BusinessException(9000,"获取redis客户端异常");
			}
	    	Collection<Jedis> collection=shardedJedis.getAllShards();
	    	Iterator<Jedis> jedis = collection.iterator();
	    	while(jedis.hasNext()){
	    		jedis.next().select(0);
	    	}
			boolean broken = false;
			try {
				boolean flag=shardedJedis.exists("redisFlag");
				 if(flag){
					 String redisFlagStr=shardedJedis.get("redisFlag");
					 if("true".equals(redisFlagStr)){
						 return true;
					 }else{
						 return false;
					 }
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
			return false;
		}


//		public static void addUnnoLimit(String unno, Double amt,Double dayUnnoAmt) throws BusinessException{
//
//			ShardedJedis shardedJedis = pool.getResource();
//			if(shardedJedis==null ){
//				throw new BusinessException(9000,"获取redis客户端异常");
//			}
//	    	Collection<Jedis> collection=shardedJedis.getAllShards();
//	    	Iterator<Jedis> jedis = collection.iterator();
//	    	while(jedis.hasNext()){
//	    		jedis.next().select(0);
//	    	}
//			 boolean broken = false;
//			// JedisLock jedisLock = new JedisLock();
//	        try {
//	        	long flag=shardedJedis.hsetnx("unnoLimitAmt", unno, String.valueOf(amt));
//	   		 	if(flag==0){
//						 boolean lockFlag= jedisLock.acquire(shardedJedis, unno);
//						 if(lockFlag){
//							 /**
//							  * 存在卡号-金额 键值对 更新
//							  */
//							 String unnoAmt=shardedJedis.hget("unnoLimitAmt", unno);
//							 if(unnoAmt!=null && !"".equals(unnoAmt)){
//									BigDecimal bd = new BigDecimal(unnoAmt);
//						            BigDecimal bd2 = new BigDecimal(amt);
//						            Double result=bd.add(bd2).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
//						            if(result>dayUnnoAmt){
//						            	throw new BusinessException(9001, "单日机构限额不允许交易！");
//						            }
//								 shardedJedis.hset("unnoLimitAmt", unno, String.valueOf(result));
//							 }else{
//								 /**
//								  * 存在卡号-金额 但是value为空
//								  */
//								 shardedJedis.hset("unnoLimitAmt", unno, amt.toString());
//							 } 
//							 // 释放锁
//							 jedisLock.release(shardedJedis, unno);
//						 }
//			 }
//	        } catch (BusinessException e) {
//	            log.info("机构号-"+unno+"---->"+e.getCode()+":"+e.getMessage());
//	            broken = true;
//	            throw new BusinessException(e.getCode(), e.getMessage());
//	        } finally {
//			    if (jedisLock != null) {
//			        try {  
//			        	jedisLock.release(shardedJedis, unno);// 则解锁  
//			        } catch (Exception e) {  
//			        	
//			        }
//			    }
//			    if(broken){
//		            pool.returnBrokenResource(shardedJedis);
//			    }else{
//		            pool.returnResource(shardedJedis);
//			    }
//	            
//	        }
//			 
//		
//		}
		
		
		public static boolean checkQuickPayDayCount(String key ,String toaccno) throws BusinessException{
			ShardedJedis shardedJedis = pool.getResource();
			if(shardedJedis==null ){
				throw new BusinessException(9000,"获取redis客户端异常");
			}
	    	Collection<Jedis> collection=shardedJedis.getAllShards();
	    	Iterator<Jedis> jedis = collection.iterator();
	    	while(jedis.hasNext()){
	    		jedis.next().select(0);
	    	}
			boolean broken = false;
			try {
				boolean flag=shardedJedis.hexists(key, toaccno);
				 if(flag){
					 String count=shardedJedis.hget(key, toaccno);
					 Integer qkCount= new Double(count).intValue();
					 if(qkCount>=5){
						 return false;
					 }
				 }
				 return true;
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
			return false;
			 
	}
		
		
	public static void addFailCountByRedis(long increase){
		try {
			ShardedJedis shardedJedis = pool.getResource();
			if(shardedJedis==null ){
				throw new BusinessException(9000,"获取redis客户端异常");
			}
	    	Collection<Jedis> collection=shardedJedis.getAllShards();
	    	Iterator<Jedis> jedis = collection.iterator();
	    	while(jedis.hasNext()){
	    		jedis.next().select(0);
	    	}
			boolean broken = false;
			try {
				long flag=shardedJedis.hsetnx("TxnNum", "xpay", "1");
				 if(flag==0){
					 shardedJedis.hincrBy("TxnNum", "xpay", increase);
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
		} catch (Exception e) {
			log.error("redis累增异常次数异常：", e);
		}

	}
}
