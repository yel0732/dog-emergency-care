package com.ssafy.rescuemungz;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@MapperScan(value = "com.ssafy.rescuemungz", annotationClass = Mapper.class)
@ConfigurationPropertiesScan
@SpringBootApplication
public class RescueMungzApplication {

	public static void main(String[] args) {
		SpringApplication.run(RescueMungzApplication.class, args);
	}

}
