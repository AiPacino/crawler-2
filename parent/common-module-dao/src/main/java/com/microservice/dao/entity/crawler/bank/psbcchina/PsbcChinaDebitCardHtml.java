package com.microservice.dao.entity.crawler.bank.psbcchina;


import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;

import com.microservice.dao.entity.IdEntity;

/**
 * html源码
 * @author tz
 *
 */
@Entity
@Table(name = "psbcchina_debitcard_html" ,indexes = {@Index(name = "index_psbcchina_debitcard_html_taskid", columnList = "taskid")})
public class PsbcChinaDebitCardHtml extends IdEntity implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 9216306926854453870L;

	/**
	 * taskid  uuid 前端通过uuid访问状态结果
	 */
	private String taskid;
	
	private String type;
	private String pageCount;	
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
	public String getPageCount() {
		return pageCount;
	}
	public void setPageCount(String pageCount) {
		this.pageCount = pageCount;
	}
	
	@Column(columnDefinition="text")
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
	
	public PsbcChinaDebitCardHtml(String taskid, String type, String pageCount, String url, String html) {
		super();
		this.taskid = taskid;
		this.type = type;
		this.pageCount = pageCount;
		this.url = url;
		this.html = html;
	}
	public PsbcChinaDebitCardHtml() {
		super();
		// TODO Auto-generated constructor stub
	}
	
	

}
