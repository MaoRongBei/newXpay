package com.hrtpayment.xpay.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
 
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.hrtpayment.db.DBConstant;
import com.hrtpayment.java.FastByteArrayOutputStream;
import com.hrtpayment.model.GenericORMModel;
import com.hrtpayment.model.ResultModel;
import com.hrtpayment.rockdb.BigDataDaoFactory;
import com.hrtpayment.rockdb.GenericBigDataUtil;
import com.hrtpayment.util.JsonHelper;

import net.sf.json.JSONObject;

@Controller
@RequestMapping("/testCoreJar")
public class GenericBigDataUtilTest {
	@ResponseBody
	@RequestMapping("/normaalCase.do")
	public String normaalCase() throws IOException{
		GenericORMModel model = new GenericORMModel();
        model.setTable("hy_tab_jhpay_order_test");
        model.addValue("id", 1001);
        model.setType(DBConstant.INSERT);
        model.addValue("mer_id", "9ae3d585-dd9d-4729-b4f9-2ec65569e2e7").addValue("mem_id", "b74e74fc-a48c-4f4f-af3e-6fdc5578feaf");

        List<GenericORMModel> modelList = new ArrayList<>();
        modelList.add(model);

        model = new GenericORMModel();
        model.setTable("hy_tab_jhpay_order_test");
        model.addValue("id", 1002);
        model.setType(DBConstant.INSERT);
        model.addValue("mer_id", "db3be2e6-7571-4b6a-a748-82be926130dd").addValue("mem_id", "4A9A15FEC19B3628E050330A7A841388");
        modelList.add(model);

        FastByteArrayOutputStream outputStream = new FastByteArrayOutputStream();
        JsonHelper.toJson(modelList, outputStream);
        System.out.println(outputStream.toString());


        ResultModel result = GenericBigDataUtil.saveOrUpdateBatch(modelList);

        System.out.println(result.getCode()== ResultModel.SUCCESS_CODE);
        System.out.println(result.getAffectRows()== 2);
        FastByteArrayOutputStream output = new FastByteArrayOutputStream();
        result.toJSON(output);
        System.out.println("insert two records: " + output.toString());


        model = new GenericORMModel();
        model.setTable("hy_tab_jhpay_order_test");
        model.addValue("id", 1001);

        result = GenericBigDataUtil.getByPK(model);
        System.out.println(result.getCode()== ResultModel.SUCCESS_CODE);
        System.out.println(result.getRows().size()== 1);

        output = new FastByteArrayOutputStream();
        result.toJSON(output);
        System.out.println("getByPK 1: " + output.toString());

        model = new GenericORMModel();
        model.setTable("hy_tab_jhpay_order_test");
        model.addValue("id", 1002);

        result = GenericBigDataUtil.getByPK(model);
        System.out.println(result.getCode()== ResultModel.SUCCESS_CODE);
        System.out.println(result.getRows().size()== 1);

        output = new FastByteArrayOutputStream();
        result.toJSON(output);
        System.out.println("getByPK 2: " + output.toString());

        //update a column.

        model = new GenericORMModel();
        model.setTable("hy_tab_jhpay_order_test");
        model.setType(DBConstant.UPDATE);
        model.addValue("id", 1001);
        model.addValue("mer_id", "9ae3d585-dd9d-4729-b4f9-2ec65569e2e7(修改)");

        modelList = new ArrayList<>();
        modelList.add(model);
        result = GenericBigDataUtil.saveOrUpdate(model);
        output = new FastByteArrayOutputStream();
        result.toJSON(output);
        System.out.println("saveOrUpdate 1: " + output.toString());

        model = new GenericORMModel();
        model.setTable("hy_tab_jhpay_order_test");
        model.addValue("id", 1001);

        result = GenericBigDataUtil.getByPK(model);
        System.out.println(result.getCode()== ResultModel.SUCCESS_CODE);
        System.out.println(result.getRows().size()== 1);
        output = new FastByteArrayOutputStream();
        result.toJSON(output);
        System.out.println("getByPK 1 again: " + output.toString());

        System.out.println("0=" +  (int)'0' + " a=" + (int)'a' + " A="+ (int) 'A');
        byte[] data = new byte[] {Byte.MIN_VALUE};

        BigDataDaoFactory.init();
	
		return new JSONObject().accumulate("status", "false").toString();
	}
}
