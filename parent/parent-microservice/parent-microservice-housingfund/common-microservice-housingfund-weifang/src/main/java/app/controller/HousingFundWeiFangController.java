package app.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.crawler.housingfund.json.MessageLoginForHousing;
import com.crawler.mobile.json.ResultData;
import com.crawler.mobile.json.StatusCodeEnum;
import com.microservice.dao.entity.crawler.housing.basic.TaskHousing;

import app.service.HousingWeiFangService;

@RestController
@Configuration
@RequestMapping("/housing/weifang") 
public class HousingFundWeiFangController extends HousingBasicController {
	
	@Autowired
	private HousingWeiFangService housingWeiFangService;
	
	@RequestMapping(value = "/login", method = RequestMethod.POST)
	public ResultData<TaskHousing> login(@RequestBody MessageLoginForHousing messageLoginForHousing) {
		tracer.addTag("crawler.housingFund.login.taskid", messageLoginForHousing.getTask_id());
		TaskHousing taskHousing = findTaskHousing(messageLoginForHousing.getTask_id());

		ResultData<TaskHousing> result = new ResultData<TaskHousing>();

		taskHousing = housingWeiFangService.sendSms(messageLoginForHousing);
		
		result.setData(taskHousing);
		return result;
		
	}
	
	@RequestMapping(value = "/loginCombo", method = RequestMethod.POST)
	public ResultData<TaskHousing> loginCombo(@RequestBody MessageLoginForHousing messageLoginForHousing) {
		tracer.addTag("crawler.housingFund.loginCombo.taskid", messageLoginForHousing.getTask_id());
		TaskHousing taskHousing = findTaskHousing(messageLoginForHousing.getTask_id());

		ResultData<TaskHousing> result = new ResultData<TaskHousing>();

		taskHousing = housingWeiFangService.verifySms(messageLoginForHousing);
		
		result.setData(taskHousing);
		return result;
		
	}
	
	@RequestMapping(value = "/crawler", method = RequestMethod.POST)
	public ResultData<TaskHousing> getData(@RequestBody MessageLoginForHousing messageLoginForHousing) {
		TaskHousing taskHousing = findTaskHousing(messageLoginForHousing.getTask_id());

		ResultData<TaskHousing> result = new ResultData<TaskHousing>();

		taskHousing = housingWeiFangService.getAllData(messageLoginForHousing);
		
		result.setData(taskHousing);
		return result;
		
	}
}
