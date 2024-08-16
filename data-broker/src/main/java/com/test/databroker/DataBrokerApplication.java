package com.test.databroker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class DataBrokerApplication extends SpringBootServletInitializer {

	public DataBrokerApplication() {
		System.out.println("DataBrokerApplication being created !!");
		// TODO Auto-generated constructor stub
	}

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
    	return application.sources(DataBrokerApplication.class);
	}
	
	public static void main(String[] args) {
		SpringApplication.run(DataBrokerApplication.class, args);
	}
}
