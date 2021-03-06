package com.microservice.dao.entity.crawler.housing.guangzhou;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;

import com.microservice.dao.entity.IdEntity;

/**
 * @description: 广州公积金信息页面存储公用html
 * @author: sln 
 * @date: 2017年9月29日 上午9:58:45 
 */
@Entity
@Table(name="housing_guangzhou_html",indexes = {@Index(name = "index_housing_guangzhou_html_taskid", columnList = "taskid")})
public class HousingGuangzhouHtml extends IdEntity implements Serializable {
	private static final long serialVersionUID = -281963208057393328L;
	private String taskid;							
	private String type;
	private Integer pagenumber;	
	private String url;	
	private String html;
	public String getTaskid() {
		return taskid;
	}
	public void setTaskid(String taskid) {
		this.taskid = taskid;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public Integer getPagenumber() {
		return pagenumber;
	}
	public void setPagenumber(Integer pagenumber) {
		this.pagenumber = pagenumber;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	@Column(columnDefinition="text")
	public String getHtml() {
		return html;
	}
	public void setHtml(String html) {
		this.html = html;
	}
	
}
