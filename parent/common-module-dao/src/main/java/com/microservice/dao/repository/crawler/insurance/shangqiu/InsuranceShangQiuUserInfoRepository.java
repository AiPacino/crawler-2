package com.microservice.dao.repository.crawler.insurance.shangqiu;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.microservice.dao.entity.crawler.insurance.shangqiu.InsuranceShangQiuUserInfo;

/**
 * @description:
 * @author: sln 
 * @date: 2018年1月9日 下午3:44:00 
 */
@Repository
public interface InsuranceShangQiuUserInfoRepository extends JpaRepository<InsuranceShangQiuUserInfo, Long> {

}
