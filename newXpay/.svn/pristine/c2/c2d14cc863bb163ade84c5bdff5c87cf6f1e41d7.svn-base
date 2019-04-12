package com.hrtpayment.xpay.quickpay.common.service;



import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.hrtpayment.xpay.common.dao.JdbcDao;
import com.hrtpayment.xpay.quickpay.common.bean.QuickPayBean;
import com.hrtpayment.xpay.quickpay.cups.service.CupsQuickPayService;

/**
 * 快捷支付
 * @author songbeibei
 * 2017年10月26日
 */
@Service
public class QuickPayRtnHtmlService {
	Logger logger = LogManager.getLogger();
	@Autowired
	JdbcDao dao;
	@Value("${quick.htmlsendurl}") // 快捷支付 url
	private String url;
	
	@Autowired 
	CupsQuickPayService cupsQuickPayService;
 
	/**
	 * 组装一个返回给APP的html页面 type=1：成功 html内含不可修改项有卡号、手机、商户名称、身份证号、fiid（隐藏，不显示）
	 * 、金额（隐藏，不显示） 、银行行号（隐藏，不显示）、银行名称 可修改项有CVV、有效期 type=2：成功 显示成功标识 详细参考银联成功返回页面
	 * type=3：失败 页面待定异常 type=4： 处理中页面待定
	 * 
	 * @param type
	 * @return
	 */
	public String returnHtml(String type, QuickPayBean bean) {
		StringBuffer html = new StringBuffer();
		if ("3".equals(type)) {
			html.append("<!DOCTYPE html><html>");
			html.append("<body onload=\"_do();\">"); 
			html.append("<p value=\" test\">");
		    html.append("<script>");
		    html.append(" function _do(){");//bean.getBankName()
		    html.append(" var err =\"?status=3&msg=" + bean.getMsg() + "\";");

		    html.append(" location.href=\""+url+"/xpay/orderPayCus.html\"+err;");
		    html.append("}</script></body></html>");
		} else if ("2".equals(type) || "4".equals(type)) {
			html.append("<!DOCTYPE html><html>");
			html.append("<body onload=\"_do();\">"); 
			html.append("<p value=\" test\">");
		    html.append("<script>");
		    html.append(" function _do(){");
			html.append(" var err =\"?status=" + type + "&amount=" + bean.getAmount() + "&accNo=" + bean.getAccNo()
					+ "&accName=" + bean.getAccName() + "&orderTime=" + bean.getOrderTime() + "&orderId="
					+ bean.getOrderId() + "\";");//
			html.append(" location.href=\""+url+"/xpay/orderPayCus.html\"+err;");//
			html.append("}</script></body></html>");
		}else{
			html.append(
					"<!DOCTYPE html><html><head><meta charset=\"utf-8\"><meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge,chrome=1\">");
			html.append(
					"<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\">");
			html.append(
					"<link href=\""+url+"/xpay/loading.css\" rel=\"stylesheet\">");
			
			html.append("<title>订单支付</title><style type=\"text/css\">");
			html.append(
					"*{margin: 0;padding: 0;box-sizing: border-box;}body{font-size: 14px;background-color: #f2f2f2;color: #333;}");
			html.append(
					"input{box-sizing: border-box;-webkit-box-sizing:border-box;-webkit-appearance: none;border: 0;outline:none;height: 38px;}");
			html.append(
					"*:focus { outline: none; }header,section{background-color: #fff;padding: 10px 15px;border-top: 1px solid #ebeced;border-bottom: 1px solid #ebeced;}");
			html.append(
					"header{border-top: 0;}header p{line-height: 30px;}section{margin-top: 15px;padding: 0 15px;}section ul{list-style: none;}");
			html.append(
					"ul li{border-bottom: solid 1px #f5f5f6;height: 40px;line-height: 40px;}#orderNum{color: #f08a46;}");
			html.append(".item-color{color: #7f7f7f;}.item-left{width: 25%;display: inline-block;text-align: justify; }");
			html.append(".item-right{width: 73%;display: inline-block;font-size: 14px;}");
			html.append(".captcha-right{width: 42%;display: inline-block;font-size: 14px;}");
			html.append(".submit-right{width: 33%; height: 35px;display: inline-left;font-size: 14px; background-color:white; color: #8E8E8E ;}");// #ADADAD #328cf4 #fff
			html.append(
					"button{width: 90%;background-color: #328cf4;display: block;margin: 80px auto 0;height: 44px;border-radius: 3px;font-size: 16px;color: #fff;border: 0;}");
			html.append("</style></head><body>");
			html.append(buildRedirectHtml(bean,type));
			html.append("<script>   ");
			html.append("var countdown=60; ");
			html.append("function  getCaptcha(obj){ var flag=setMessage(); if(flag===false){}else{settime(obj);}}");
			html.append("function setMessage(){");
			html.append("var fiid =document.querySelector(\"#fiid\").value;");
			html.append("var mid =document.querySelector(\"#mid\").value;");
			html.append("var qpcid =document.querySelector(\"#qpcid\").value;");
			html.append("var amount =document.getElementById(\"orderNum\").innerText;");
			html.append("var orderid =document.querySelector(\"#orderid\").value;");
			html.append("var cvn ='123';");
			html.append("var effective='1234';");
			html.append("if(cvn===''||cvn.length!==3){alert(\"请正确填写CVN2\");return false;} ");
			html.append("if (effective===''||effective.length!==4) {alert(\"请正确填写有效期\");return false;} ");
			html.append("var params=\"fiid=\"+fiid+\"&mid=\"+mid+\"&qpcid=\"+qpcid+\"&amount=\"+amount+\"&orderid=\"+orderid+\"&cvn=\"+cvn+\"&effective=\"+effective;");
			html.append("var request = new XMLHttpRequest();");
			html.append("request.open('POST', '"+url+"/xpay/getMessage', true);"); 
			html.append("request.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');");
			html.append("request.onreadystatechange = function(data) { if (request.readyState == XMLHttpRequest.DONE && request.status == 200)"); 
			html.append("{console.log(data.target.response);console.log(request.responseText); json=JSON.parse(request.responseText);");
			html.append(" if(json.preSerial!=''&&json.preSerial!=null)");
			html.append("{document.querySelector(\"#preSerial\").value=json.preSerial; document.querySelector(\"#bankMid\").value=json.bankmid;}");
			html.append(" else{var err =\"?status=\"+3+\"&msg=\"+json.rtnHtml;location.href=\""+url+"/xpay/orderPayCus.html\"+err;}}");
			html.append("else {console.log(\"Request was unsuccessful: \" + request.status)}};request.send(params); }");
			html.append("function settime(obj) { ");
		    html.append("if (countdown == 0) {obj.removeAttribute(\"disabled\"); obj.value=\"重获验证码\"; countdown = 60;  return;");
			html.append(" } else {  obj.setAttribute(\"disabled\", true);  obj.value=\"重新发送(\" + countdown + \")\"; countdown--; } ");
			html.append(" setTimeout(function() { settime(obj) },1000) } ");
			html.append("function submitOrderPay(_this) { ");
			html.append( "setTimeout(\"alert('请求超时！');hidenLoading();location.href='retuenMainApp';\", 60000 );");
			html.append( "showLoading();var cvn ='123';");
			html.append("var effective='1234';");
			
			html.append("if (cvn===''||cvn.length!==3) {alert(\"请正确填写CVN2\");return}");
			html.append("if (effective===''||effective.length!==4) {alert(\"请正确填写有效期\");return}");
			if ("0".equals(type)) {
				html.append("var captcha=document.querySelector(\"#captcha\").value;");
				html.append("if(cvn===''||cvn.length!==3){alert(\"请正确填写CVN2\");return} ");
				html.append("if (effective===''||effective.length!==4) {alert(\"请正确填写有效期\");return} ");
				html.append("if (captcha===''||captcha.length!==6) {alert(\"请正确填写验证码\");return}");
			}
			html.append("_this.style.cssText=\"background-color:#ccc;\";_this.innerHTML=\"提交中···\";");
			html.append("_this.setAttribute(\"disabled\",\"disabled\");");
			html.append(" _post();}");
			html.append("function _post(){var fiid =document.querySelector(\"#fiid\").value;");
			html.append("var cvn ='123';");
			html.append("var effective='1234';");
			html.append("var mid =document.querySelector(\"#mid\").value;");
			html.append("var qpcid =document.querySelector(\"#qpcid\").value;");
			html.append("var isPoint =document.querySelector(\"#isPoint\").value;");
			html.append("var unno =document.querySelector(\"#unno\").value;");
			html.append("var orderid =document.querySelector(\"#orderid\").value;");
			html.append("var accNo =document.getElementById(\"accNo\").innerText;");
			html.append("var amount =document.getElementById(\"orderNum\").innerText;");
			html.append("var bankName =document.getElementById(\"bankName\").innerText;");
			html.append("var preSerial =document.querySelector(\"#preSerial\").value;");
			html.append("var bankMid =document.querySelector(\"#bankMid\").value;");
			html.append("var deviceId =document.querySelector(\"#deviceId\").value;");
			html.append("var position =document.querySelector(\"#position\").value;");
			html.append("var json ;");
			html.append("var params=\"fiid=\"+fiid+\"&mid=\"+mid+\"&qpcid=\"+qpcid+\"&");
			html.append("accNo=\"+accNo+\"&amount=\"+amount+\"&bankName=\"+bankName+\"&isPoint=\"+isPoint+\"&preSerial=\"+preSerial+\"&bankMid=\"+bankMid+\"&unno=\"+unno+\"&orderid=\"+orderid+\"&cvn=\"+cvn"
					+ "+\"&effective=\"+effective+\"&deviceId=\"+deviceId+\"&position=\"+position;");
			if ("0".equals(type)) {
				html.append("var captcha=document.querySelector(\"#captcha\").value;");
				html.append("var params=params+\"&captcha=\"+captcha+\"&iscaptcha=\"+0;");
			}else{
				html.append("var params=params+\"&iscaptcha=\"+1;");
			}
			html.append("var request = new XMLHttpRequest();");
			html.append("request.open('POST', '"+url+"/xpay/setpayinfo', true); ");
			html.append("request.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');");
			html.append("request.onreadystatechange = function(data) { ");
			html.append("if (request.readyState == XMLHttpRequest.DONE && request.status == 200) {hidenLoading() ;");
			html.append("console.log(data.target.response); json=JSON.parse(request.responseText);  ");
	//		if (!"3".equals(type)&&!"2".equals(type) &&!"4".equals(type)) {
//			html.append("  alert(request.responseText);  ");
			html.append(" var err =\"?status=\"+json.htnlStatus+\"&amount=\"+json.amount+\"&accNo=\"+json.accNo+\"&accName=\"+json.accName+\"&orderTime=\"+json.orderTime+\"&orderId=\"+json.orderId+\"&msg=\"+json.msg;");////
			html.append(" location.href=\""+url+"/xpay/orderPayCus.html\"+err;");//
	//		}
			html.append("} else {hidenLoading() ;console.log(\"Request was unsuccessful: \" + request.status)}};");
			html.append("request.send(params); } ");
			html.append("function hidenLoading() {document.querySelector(\"#loadingToast\").style.cssText=\"display:none\";} ");
			html.append("function showLoading() {document.querySelector(\"#loadingToast\").style.cssText=\"display:block\";} ");
			html.append("</script> ");
			html.append("</body></html>");
		}
		return html.toString();
	}

	private String buildRedirectHtml(QuickPayBean bean,String type) {
		StringBuffer html = new StringBuffer();
		html.append("<header><p>订单金额：<span id=\"orderNum\">" + bean.getAmount()
				+ "</span>元</p><p>商户名称：<span id=\"accName\">" + bean.getAccName() + "</span></p></header>");
		html.append("<section><ul>");
		html.append("<li><p class=\"item-left\" id=\"bankName\">" + bean.getBankName()
				+ "</p><span class=\"item-right\" style=\"padding-left: 6px;\">信用卡<span id=\"accNo\">" + bean.getAccNo()
				+ "</span></span></li>");
//		html.append(
//				"<li><label class=\"item-left\">CVN2</label><input id=\"cvn\" class=\"item-right\" type=\"tel\" maxlength=\"3\" name=\"CVV\" placeholder=\"卡背面末三位\"></li>");
//		html.append(
//				"<li ><label class=\"item-left\">有效期</label><input id=\"effective\" class=\"item-right\" type=\"tel\" maxlength=\"4\" name=\"validityPeriod\" placeholder=\"示例：09/15，输入0915\"></li>");

		if ("0".equals(type)) {
 			// 出方法传fiid
			html.append("<li style=\"border: 0;\"><label class=\"item-left\">验证码</label><input  id=\"captcha\" class=\"captcha-right\" type=\"tel\" maxlength=\"6\" name=\"captcha\" placeholder=\"6位验证码\">");
			html.append("<input  type=\"submit\" id=\"getcaptcha\" class=\"submit-right\"   name=\"getcaptcha\" value=\"获取验证码\" onclick='getCaptcha(this);'></li>");
	//		html.append("<button id=\"getcaptcha\" class=\"submit-right\"   name=\"getcaptcha\"onclick='getCaptcha(this);'>获取验证码</button></li>");
		}
		html.append("	</ul>");
		html.append(" <input type=\"hidden\"   id=\"fiid\" value=\"" + bean.getFiid() + "\"/>");
		html.append(" <input type=\"hidden\"   id=\"mid\" value=\"" + bean.getMid() + "\"/>");
		html.append(" <input type=\"hidden\"   id=\"qpcid\" value=\"" + bean.getQpcid() + "\"/>");
		html.append(" <input type=\"hidden\"   id=\"isPoint\" value=\"" + bean.getIsPoint() + "\"/>");
		html.append(" <input type=\"hidden\"   id=\"unno\" value=\"" + bean.getUnno() + "\"/>");
		html.append(" <input type=\"hidden\"   id=\"orderid\" value=\"" + bean.getOrderId() + "\"/>");
		html.append(" <input type=\"hidden\"   id=\"deviceId\" value=\"" + bean.getDeviceId() + "\"/>");
		html.append(" <input type=\"hidden\"   id=\"position\" value=\"" + bean.getPosition() + "\"/>");
		html.append(" <input type=\"hidden\"   id=\"preSerial\" value=''/>");
		html.append(" <input type=\"hidden\"   id=\"bankMid\" value=''/>");
		html.append(" </section><button onclick='submitOrderPay(this);'>确认支付</button>");
		html.append(" <div id=\"loadingToast\" style=\"display:none;font-size:14px;\">");
		html.append(" <div class=\"weui-mask_transparent\"></div>");
		html.append(" <div class=\"weui-toast\">");
		html.append("  <i class=\"weui-loading weui-icon_toast\"></i>");
		html.append("  <p class=\"weui-toast__content\">数据加载中</p>");
		html.append("  </div></div>");
		return html.toString();

	}
}
