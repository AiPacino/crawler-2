package app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.retry.annotation.EnableRetry;

@EnableEurekaClient
@SpringBootApplication
@EnableCircuitBreaker
@EnableFeignClients
@EnableRetry
public class EurekaExecutorApplication {
	
//	@Autowired 
//	private RestTemplateBuilder builder; 
//
//	// 使用RestTemplateBuilder来实例化RestTemplate对象，spring默认已经注入了RestTemplateBuilder实例 
//	@Bean 
//	public RestTemplate restTemplate() { 
//		return builder.build(); 
//	} 
	
	public static void main(String[] args) {
		SpringApplication.run(EurekaExecutorApplication.class, args);
	}
}
