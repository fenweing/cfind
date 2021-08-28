package com.parrer.cfind;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CfindApplication {
//public class CfindApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(CfindApplication.class, args);
    }
//    @Bean//注册视图解析器
//    public InternalResourceViewResolver setupViewResolver() {
//        InternalResourceViewResolver resolver = new InternalResourceViewResolver();
//        resolver.setPrefix("/WEB-INF/jsp/");//自动添加前缀
//        resolver.setSuffix(".jsp");//自动添加后缀
//        return resolver;
//    }
}
