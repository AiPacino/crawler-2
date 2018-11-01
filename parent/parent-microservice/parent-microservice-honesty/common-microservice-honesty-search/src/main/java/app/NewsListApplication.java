package app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableEurekaClient
@EnableFeignClients
@EnableScheduling
// @EnableScheduling 注解的作用是发现注解@Scheduled的任务并后台执行。
@EnableJpaAuditing // 启动jpaupdatetime自动添加
@EnableAsync
public class NewsListApplication { 

	public static void main(String[] args) {
		SpringApplication.run(NewsListApplication.class, args);
	}

}
