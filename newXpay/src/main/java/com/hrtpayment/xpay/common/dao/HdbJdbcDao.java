package com.hrtpayment.xpay.common.dao;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidDataSourceFactory;

@Component
public class HdbJdbcDao implements InitializingBean{
	private final Logger logger = LogManager.getLogger();
    private JdbcTemplate jdbcTemplate;

    public HdbJdbcDao() {
		Properties properties = new Properties();
		File file = new File(System.getProperty("user.dir")+File.separator+"conf"+File.separator+"druid_hdb.properties");
		InputStream input = null;
		try {
			input = new FileInputStream(file);
			properties.load(input);
		} catch (IOException e) {
			logger.error("载入手刷Properties出现异常", e);
		} finally {
			try {
				if (input != null) {
					input.close();
				}
			} catch (IOException e) {
				logger.error("关闭InputStream出现异常", e);
			}
		}
		try {
			DruidDataSource dds = (DruidDataSource) DruidDataSourceFactory
					.createDataSource(properties);
	        this.jdbcTemplate = new JdbcTemplate(dds);
		} catch (Exception e) {
			logger.error("手刷连接池初始化出错……", e);
		}
		logger.info("init手刷连接池初始化成功");
    }
	
	public void test(){
		Map<String, Object> m = jdbcTemplate.queryForMap("select 1 from dual");
		logger.info(m);
	}
	public List<Map<String, Object>> queryForList(String sql, Object...args){
		return jdbcTemplate.queryForList(sql, args);
	}
	
	public Map<String, Object>  queryForMap(String sql,Object...args) {
		return jdbcTemplate.queryForMap(sql,args);
	}
	public int update(String sql, Object...args) throws DataAccessException{
		return jdbcTemplate.update(sql, args);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		jdbcTemplate.queryForList("select 1 from dual");
	}
}
