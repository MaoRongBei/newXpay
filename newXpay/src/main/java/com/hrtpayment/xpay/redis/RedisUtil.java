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

public class RedisUtil  extends AbstractRedisCheck {

	private static Log log = LogFactory.getLog(RedisUtil.class);
	
//	private static String redisHost =PropertiesUtil.getProperties("application.properties").getProperty("redis.redishost");
//	private static String redisPwd =PropertiesUtil.getProperties("application.properties").getProperty("redis.redispwd");
//	private static String redisPort =PropertiesUtil.getProperties("application.properties").getProperty("redis.redisport");
	 
	private static String redisHost =PropertiesUtil.getProperties("source/application.properties").getProperty("redis.redishost");
	private static String redisPwd =PropertiesUtil.getProperties("source/application.properties").getProperty("redis.redispwd");
	private static String redisPort =PropertiesUtil.getProperties("source/application.properties").getProperty("redis.redisport");

	
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
	 * 
	 * 获取基础数据
	 * 
	 * @param key
	 * @return
	 * @throws BusinessException
	 */
	public static String getProperties(String key ) throws BusinessException{
		ShardedJedis shardedJedis = pool.getResource();
		if(shardedJedis==null ){
			throw new BusinessException(9000,"获取redis客户端异常");
		}
    	Collection<Jedis> collection=shardedJedis.getAllShards();
    	Iterator<Jedis> jedis = collection.iterator();
    	while(jedis.hasNext()){
    		jedis.next().select(0);
    	}
    	String msg="";
    	boolean broken=false;
		try {
			boolean flag=shardedJedis.hexists("Properties",key);
			if(flag){
				msg=shardedJedis.hget("Properties", key);
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
		return msg;
		 
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
	
		/**
		 * 
		 * 2018-12-07 新增
		 * 
		 * 按照轮询组名不累增  
		 * 微信支付宝金额
		 * 到额度提示限额
		 * 
		 * @param key
		 * @param payAmt
		 * @param toactacn
		 * @param daySumAmt
		 * @throws BusinessException
		 */
		public static void checkAmtByGroupName(String key ,Double payAmt,long dayMaxAmout)
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
				String amt=shardedJedis.hget("limitForTerms", key);
				if(amt!=null && !"".equals(amt)){
					BigDecimal bd = new BigDecimal(amt);
					BigDecimal bd2 = new BigDecimal(payAmt);
					Double result=bd.add(bd2).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
					try{
						dayMaxAmout=Long.valueOf(shardedJedis.hget("Properties", key) );
					 }catch(Exception e){
						 log.error("redis获取Properties数据微信支付宝限额异常："+e.getMessage()); 
					 }
					if(result>dayMaxAmout){
						if ("WX".equals(key)) {
		            		throw new BusinessException(9001, "当日微信额度已满，请选择支付宝支付");
						}else if ("ZFB".equals(key)){
							throw new BusinessException(9001, "当日支付宝额度已满，请选择微信支付");
						} 
					}
				}
	        } catch (BusinessException e) {
	            log.info(key+"交易---->"+e.getCode()+":"+e.getMessage());
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
	 * 
	 * 2018-12-07 新增
	 *  
	 * 按照轮询组名累增 微信支付宝金额
	 * 到额度提示限额
	 * 
	 * @param key
	 * @param payAmt
	 * @throws BusinessException
	 */
	public static void addAmtByGroupName(String key ,Double payAmt,long dayMaxAmout) throws BusinessException{
			
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
	        	long flag=shardedJedis.hsetnx("limitForTerms", key, payAmt.toString());
	   		 	if(flag==0){
						 boolean lockFlag= jedisLock.acquire(shardedJedis, key);
						 if(lockFlag){
							 /**
							  * 存在卡号-金额 键值对 更新
							  */
							 String amt=shardedJedis.hget("limitForTerms", key);
							 if(amt!=null && !"".equals(amt)){
									BigDecimal bd = new BigDecimal(amt);
						            BigDecimal bd2 = new BigDecimal(payAmt);
						            Double result=bd.add(bd2).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
						            /*
						             * 2018-12-11 修改
						             * 
						             * 本方法仅用于累增，不进行金额判断
						             */
//						            if(result>dayMaxAmout){
//						            	if ("WX".equals(key)) {
//						            		throw new BusinessException(9001, "当日微信额度已满，请选择支付宝支付");
//										}else if ("ZFB".equals(key)){
//											throw new BusinessException(9001, "当日支付宝额度已满，请选择微信支付");
//										} 
//						            }
								 shardedJedis.hset("limitForTerms", key, String.valueOf(result));
							 }else{
								 /**
								  * 存在卡号-金额 但是value为空
								  */
								 shardedJedis.hset("limitForTerms", key, payAmt.toString());
							 } 
							 // 释放锁
							 jedisLock.release(shardedJedis, key);
						 }
			 }
	        } catch (BusinessException e) {
	            log.error("轮询组-"+key+"累增金额异常---->"+e.getCode()+":"+e.getMessage());
	            broken = true;
	            throw new BusinessException(e.getCode(), e.getMessage());
	        } finally {
			    if (jedisLock != null) {
			        try {  
			        	jedisLock.release(shardedJedis, key);// 则解锁  
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
	 * 2018-12-06 修改
	 * 
	 * 秒到APP交易校验
	 * 订单交易成功后  redis内未支付笔数加一  
	 * 
	 * @param mid
	 * @return
	 */
	public static boolean addCountPayByMid(String mid,Integer maxCon){
		try {
			ShardedJedis shardedJedis=pool.getResource();
			if (shardedJedis==null) {
				throw new BusinessException(9000, "获取redis客户端异常");
			}
			Collection<Jedis> collection=shardedJedis.getAllShards();
			Iterator<Jedis> jedis=collection.iterator();
			while (jedis.hasNext()) {
				 jedis.next().select(0);
			}
			boolean broken =false;
			try{
				boolean flag=shardedJedis.hexists("payLimOrd", mid);
				if(flag){
					 boolean lockFlag= jedisLock.acquire(shardedJedis, mid);
					 if(lockFlag){
						 String count=shardedJedis.hget("payLimOrd", mid);
						 Integer qkCount= new Double(count).intValue();
//						 Integer maxCon=Integer.valueOf(shardedJedis.hget("Properties", "maxCon"));
						 if(qkCount>=(maxCon==0?3:maxCon)){
							 return false;
						 }else{
							 long con=shardedJedis.hsetnx("payLimOrd", mid, "1");
							 if(con==0){
								 shardedJedis.hincrBy("payLimOrd", mid, (long)1);
							 }
						 }
					 }
					 // 释放锁
					 jedisLock.release(shardedJedis, mid);
				 }else{
					 long con=shardedJedis.hsetnx("payLimOrd", mid, "1");
					 if(con==0){
						 shardedJedis.hincrBy("payLimOrd", mid, (long)1);
					 }
				 }
				
			}catch (Exception e) {
	            log.error(e.getMessage(), e);
	            broken = true;
			}finally {
				 if (jedisLock != null) {
				        try {  
				        	jedisLock.release(shardedJedis, mid);// 则解锁  
				        } catch (Exception e) {  
				        	
				        }
				   }
			    if(broken){
		            pool.returnBrokenResource(shardedJedis);
			    }else{
		            pool.returnResource(shardedJedis);
			    }
			}
		} catch (BusinessException e) {
			log.error("redis累增商户交易次数异常：", e);
		}
		return true;
	}
	
	/**
	 * 2018-12-06 修改
	 * 
	 * 秒到APP交易校验
	 * 订单交易成功后  redis内未支付笔数减一 
	 * 
	 * @param mid
	 * @throws BusinessException
	 */
    public static void cutCountPayByMid(String mid) throws BusinessException{
		
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
			boolean flag=shardedJedis.hexists("payLimOrd", mid);
			 if(flag){
					 boolean lockFlag= jedisLock.acquire(shardedJedis, mid);
					 if(lockFlag){
						 /**
						  * 存在卡号-金额 键值对 更新
						  */
						 String cont=shardedJedis.hget("payLimOrd", mid);
						 log.info("redis中存在"+mid+"该mid,累计笔数为："+cont);
						 if(cont!=null && !"".equals(cont)){
							  Integer intc=new Double(cont).intValue();
				              if(intc>=0){
				            	  shardedJedis.hset("payLimOrd", mid, String.valueOf(intc-1));
				            	  rtnFlag=true;
				              }
				            rtnFlag=true; 
						 }
						 // 释放锁
						 jedisLock.release(shardedJedis, mid);
					 }
			 }
		} catch (Exception e) {
            log.error(e.getMessage(), e);
            broken = true;
		}finally {
			 if (jedisLock != null) {
			      try {  
			    	  jedisLock.release(shardedJedis, mid);// 则解锁  
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
      * 2019-01-17 新增
      * 
      * 校验交易是否归属风控交易
      * 
      */
    public static Map<String, Object> checkMid(String mid,String orderid ) throws BusinessException{
    	ShardedJedis shardedJedis = pool.getResource();
		if(shardedJedis==null ){
			throw new BusinessException(9000,"获取redis客户端异常");
		}
    	Collection<Jedis> collection=shardedJedis.getAllShards();
    	Iterator<Jedis> jedis = collection.iterator();
    	while(jedis.hasNext()){
    		jedis.next().select(3);
    	}
    	boolean rtnFlag=false;
    	boolean broken = false;
    	Map<String , Object> midOrdInfo=new HashMap<String,Object>();
    	midOrdInfo.put("mid",mid);
		midOrdInfo.put("newOrderid", orderid);
		String midTTL=getProperties("midTTL");
		String midLimitTTL=getProperties("midLimitTime");
		int midTTLTime=Integer.parseInt("".equals(midTTL)?"240":midTTL);
		int midLimitTime=Integer.parseInt("".equals(midLimitTTL)?"180":midLimitTTL);
		try {
			//是否存在key=mid
			boolean flag=shardedJedis.exists(mid);
			 if(flag){
			
				 /**
				  * 存在卡号-金额 键值对 更新
				  */
				 String cont=shardedJedis.get(mid);
				 midOrdInfo.put("oldOrderid", cont);
				 if(cont!=null && !"".equals(cont)){
					 long time= shardedJedis.ttl(mid);
					 if(time<=(midTTLTime-midLimitTime)&&(!"".equals(orderid)&&null != orderid)){
						 log.error(mid+":"+orderid+"进风控"+cont);
					     shardedJedis.set(mid, orderid,"XX","EX",Long.parseLong(String.valueOf(midTTLTime)));//"240"
					     rtnFlag=true;
					  }else  if(time>(midTTLTime-midLimitTime)&&("".equals(orderid)||null == orderid)){
					     log.error(mid+":"+orderid+"交易时间间隔小于"+midLimitTime/60+"分钟，请稍后重试");
//					     throw new BusinessException(8000, "交易时间间隔小于3分钟，请稍后重试");
					  }else{
						  rtnFlag=true;
					  }
				 }else{
					 shardedJedis.set(mid, orderid,"XX","EX",Long.parseLong(String.valueOf(midTTLTime)));//"240"
				     rtnFlag=true;
				}	 
			 }else{
				 shardedJedis.set(mid, orderid,"NX","EX",Long.parseLong(String.valueOf(midTTLTime)));
				 rtnFlag=true;
			 }
			 midOrdInfo.put("rtnFlag", rtnFlag);
			 midOrdInfo.put("midLimitTime", midLimitTime/60);
			 return  midOrdInfo;
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
		 return  midOrdInfo;
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
    
    
    
    
    /**
     * 
     * @param mid
     * @param orderid
     * @return
     * @throws BusinessException
     */
    public static boolean checkSpecialGroupTime(String groupname,String nowTime,String afterTime ) throws BusinessException{
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
//    	Map<String , Object> midOrdInfo=new HashMap<String,Object>();
//    	midOrdInfo.put("groupname",groupname);
		try {
			//是否存在key=mid
			boolean flag=shardedJedis.hexists("Properties",groupname);
			 if(flag){
			
				 /**
				  * 存在卡号-金额 键值对 更新
				  */
				 String groupLimitTime=shardedJedis.hget("Properties",groupname);
//			     shardedJedis.set(groupname, txncount,"XX","EX",Long.parseLong(String.valueOf(groupTTL)));//"240"
				 if (null==groupLimitTime||"".equals(groupLimitTime)) {
					 shardedJedis.hset("Properties", groupname, nowTime);
					 rtnFlag=false;
				 }else{
					 if (nowTime.compareTo(groupLimitTime)>0) { 
						 shardedJedis.hset("Properties", groupname, afterTime);
						 rtnFlag=false;
					 }else{
						 log.error(groupname+"组 限制时间截止至"+groupLimitTime+"，当前时间 为："+nowTime +"。");
						 rtnFlag=true;
					 }
				 }
			 }else{
				 shardedJedis.hset("Properties", groupname, nowTime);
				 rtnFlag=true; 
			 }
			 return  rtnFlag;
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
		 return  rtnFlag;
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
