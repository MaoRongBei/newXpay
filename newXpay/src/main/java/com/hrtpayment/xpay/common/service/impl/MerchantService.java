package com.hrtpayment.xpay.common.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hrtpayment.xpay.common.dao.JdbcDao;

@Service
public class MerchantService {
	@Autowired
	JdbcDao dao;
	
	public String queryMerName(String mid) {
		List<Map<String, Object>> list = dao.queryForList("select tname from Hrt_Merchacc where hrt_MID =?", mid);
		if (list.size()>0) {
			return (String) list.get(0).get("TNAME");
		}
		return null;
	}
	
	
	public String queryMerNameByTid(String tid) {
		List<Map<String, Object>> list = dao.queryForList("select  mid  from Hrt_Aggpaytermsub where qrtid=?", tid);
		if (list.size()>0) {
			return (String) list.get(0).get("MID");
		}
		return null;
	}
}
