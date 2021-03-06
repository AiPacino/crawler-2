package com.microservice.dao.entity.crawler.insurance.pingdingshan;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;

import com.microservice.dao.entity.IdEntity;
@Entity
@Table(name="insurance_pingdingshan_html",indexes = {@Index(name = "index_insurance_pingdingshan_html_taskid", columnList = "taskid")}) 
public class InsurancePingDingShanHtml extends IdEntity{
	private String taskid;							//uuid 前端通过uuid访问状态结果
	private String type;                            //类型
	private Integer pageCount;	                    // 01
	private String url;	                            //地址
	private String html;                            //源码
	
	@Override
	public String toString() {
		return "InsuranceBeijingHtml [taskid=" + taskid + ", type=" + type + ", pageCount=" + pageCount + ", url=" + url
				+ ", html=" + html + "]";
	}
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
	public Integer getPageCount() {
		return pageCount;
	}
	public void setPageCount(Integer pageCount) {
		this.pageCount = pageCount;
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
