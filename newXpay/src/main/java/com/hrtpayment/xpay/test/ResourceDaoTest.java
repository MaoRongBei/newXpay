package com.hrtpayment.xpay.test;

import java.util.List;

import org.junit.Assert;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSONObject;
import com.hrtpayment.dao.ResourceDao;
import com.hrtpayment.dao.ResourceDaoImpl;
import com.hrtpayment.model.ResourceModel;

@Controller
@RequestMapping("/ResourceDaoTest")
public class ResourceDaoTest {
	@ResponseBody
	@RequestMapping("/checkResourceConfig1.do")
	public String checkResourceConfig1(){
		ResourceDao resourceDao = new ResourceDaoImpl();
        int changeStatus = resourceDao.checkResourceConfig(null);
        System.out.println("Having data change "+ (changeStatus > 0));
        changeStatus = resourceDao.checkResourceConfig(100000l);
        System.out.println("No data change "+( changeStatus == 0));
        JSONObject test =new JSONObject() ;
        test.put("status", "success");
        return test.toJSONString();
	}
	@ResponseBody
	@RequestMapping("/getResourceListByLastVersion.do")
	public String getResourceListByLastVersion() throws  Exception {
        ResourceDao resourceDao = new ResourceDaoImpl();
        List<ResourceModel> list = resourceDao.getResourceListByLastVersion();
        System.out.println("Get all resource"+(list != null && !list.isEmpty()));
//        return new JSONObject().put("status", "sucess").toString();
        JSONObject test =new JSONObject() ;
        test.put("status", "success");
        return test.toJSONString();
    }
}
