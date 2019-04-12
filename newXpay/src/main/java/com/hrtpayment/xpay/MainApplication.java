package com.hrtpayment.xpay;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.hrtpayment.xpay.cups.sdk.SDKConfig;




@SpringBootApplication
@EnableScheduling
public class MainApplication {
	
	public static void main(String[] args) { 
		SpringApplication.run(MainApplication.class, args);
		SDKConfig.getConfig();
	}
}

