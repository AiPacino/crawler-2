package app.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import com.crawler.housingfund.json.MessageLoginForHousing;
import com.microservice.dao.entity.crawler.housing.basic.TaskHousing;

import app.service.common.HousingBasicService;

@Component
@Service
@EnableAsync
@EntityScan(basePackages = "com.microservice.dao.entity.crawler.housing.songyuan")
@EnableJpaRepositories(basePackages = "com.microservice.dao.repository.crawler.housing.songyuan")
public class HousingFundSongyuanService extends HousingBasicService{
	@Autowired
	private HousingFundSongyuanCommonService housingFundSongyuanCommonService;

	public void login(MessageLoginForHousing messageLoginForHousing, TaskHousing taskHousing)throws Exception  {
		housingFundSongyuanCommonService.login(messageLoginForHousing,taskHousing);
	}

	public void crawler(MessageLoginForHousing messageLoginForHousing, TaskHousing taskHousing) {
		housingFundSongyuanCommonService.getAccountInfo(messageLoginForHousing,taskHousing);
		housingFundSongyuanCommonService.getDetailInfo(messageLoginForHousing,taskHousing);
	}

}
