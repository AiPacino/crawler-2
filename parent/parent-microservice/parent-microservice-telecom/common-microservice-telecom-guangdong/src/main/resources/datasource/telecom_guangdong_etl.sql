CREATE OR REPLACE FUNCTION "public"."telecom_guangdong_etl"("taskid" text)
  RETURNS "pg_catalog"."text" AS $BODY$   
BEGIN  
  DECLARE
		this_id text;
		p_etl_status text DEFAULT 'success';
		p_etl_error_detail text;
		p_etl_error_content text;
		
	BEGIN
		this_id = taskid;


--清除此taskid的数据

		delete from pro_mobile_bill_info where task_id = this_id;
		delete from pro_mobile_call_info where task_id = this_id;
		delete from pro_mobile_pay_info where task_id = this_id;
		delete from pro_mobile_service_info where task_id = this_id;
		delete from pro_mobile_sms_info where task_id = this_id;
		delete from pro_mobile_user_info where task_id = this_id;



-------------------------开始插入数据--------------------------------



---用户基本信息

		INSERT INTO pro_mobile_user_info
		(
			resource,task_id,createtime,carrier,basic_user_name,basic_id_num,city,
			province,phone_num,cur_balance,points,cus_level,certificate_type,education,
			email,id_num,postcode,qq,user_name,address
		)
			select 
				'telecom_guangdong_userinfo:'|| a.id::TEXT as resource,
				a.taskid as task_id,
				a.createtime as createtime,
				'中国电信' as carrier,
				e.name as basic_user_name,
				e.idnum as basic_id_num,
				b.city as city,
				b.province as province,
				b.phonenum as phone_num,
				d.month_charge as cur_balance,
				d.myjifen as points,
				f.item_name as cus_level,
				g.item_name as certificate_type,
				a.education as education,
				a.email as email,
				a.indent_code as id_num,
				a.post_code as postcode,
				a.qq_number as qq,
				a.username as user_name,
				a.user_address as address
			from 
				telecom_guangdong_userinfo a left join task_mobile b
				on a.taskid = b.taskid left join telecom_common_starlevel c
				on a.taskid = c.taskid left join telecom_common_pointsandcharges d
				on a.taskid = d.taskid left join basic_user e
				on b.basic_user_id = e.id left join userinfo_cuslevel_item_code f
				on c.membership_level = f.source_name left join userinfo_certificatetype_item_code g
				on a.indent_nbr_type = g.source_name
			where a.taskid = this_id;

--缴费信息

		INSERT INTO pro_mobile_pay_info
		(
			resource,task_id,createtime,payfee,paytime,paymonth,payway
		)
			select 
				'telecom_guangdong_payment:' || a.id::text as resource,
				a.taskid as task_id,
				a.createtime as createtime,
				a.money as payfee,
				to_char(a.pretime::TIMESTAMP,'yyyy-mm-dd') as paytime,
				to_char(a.pretime::TIMESTAMP,'yyyy-mm') as paymonth,
				b.item_name as payway
			from telecom_guangdong_payment a left join payinfo_paytype_item_code b
				on a.paymode = b.source_name
			where a.taskid = this_id;

--短信信息

		INSERT INTO pro_mobile_sms_info
		(
			resource,task_id,createtime,host_num,other_num,fee,sms_type,sms_way,send_time
		)
			select 
				'telecom_guangdong_smsthrem:' || a.id::text as resource,
				a.taskid as task_id,
				a.createtime as createtime,
				b.phonenum as host_num,
				a.dnumber as other_num,
				a.smsmoney as fee,
				c.item_name as sms_type,
				null as sms_way,
				a.smsdate as send_time
			from telecom_guangdong_smsthrem a left join task_mobile b
				on a.taskid = b.taskid left join smsinfo_smstype_item_code c
				on a.smstype = c.source_name 
			where a.taskid = this_id;

--通话记录信息

		INSERT INTO pro_mobile_call_info
		(
			resource,task_id,createtime,phone_num,his_num,fee,call_location,call_time,call_type,charge_type,
			call_duration
		)
			select 
				'telecom_guangdong_callthrem:'||a.id::text as resource,
				a.taskid as task_id,
				a.createtime as createtime,
				b.phonenum as phone_num,
				a.dialnumber as his_num,
				a.callmoney as fee,
				a.callland as call_location,
				a.calldate as call_time,
				c.item_name as call_type,
				d.item_name as charge_type,
				handle_timeformat('guangdong',duration) as call_duration
			from telecom_guangdong_callthrem a left join task_mobile b
				on a.taskid = b.taskid left join callinfo_calltype_item_code c
				on a.calltype = c.source_name left join callinfo_chargetype_item_code d
				on a.datecalltype = d.source_name 
			where a.taskid = this_id;

--更新此任务状态

	--		UPDATE task_mobile a set a.etltime = now(),a.etl_status = p_etl_status where a.taskid = this_id;

			RETURN p_etl_status;
		
--异常处理
	EXCEPTION
		WHEN QUERY_CANCELED THEN 
--获取错误详情
			GET STACKED DIAGNOSTICS p_etl_error_detail = MESSAGE_TEXT;
			GET STACKED DIAGNOSTICS p_etl_error_content = PG_EXCEPTION_CONTEXT;
--记录错误到task表
--			UPDATE task_mobile a set a.etl_status = p_etl_status where a.taskid = this_id;
--返回失败
			p_etl_status = p_etl_error_content ||' : '|| p_etl_error_detail;

			RETURN p_etl_status;
			

		WHEN OTHERS THEN 
--获取错误详情
			GET STACKED DIAGNOSTICS p_etl_error_detail = MESSAGE_TEXT;
			GET STACKED DIAGNOSTICS p_etl_error_content = PG_EXCEPTION_CONTEXT;
--记录错误到task表
--			UPDATE task_mobile a set a.etl_status = p_etl_status where a.taskid = this_id;
--返回失败
			p_etl_status = p_etl_error_content ||' : '|| p_etl_error_detail;

			RETURN p_etl_status;

	END;
END;   
$BODY$
  LANGUAGE 'plpgsql' VOLATILE COST 100
;

;;--ALTER FUNCTION "public"."telecom_guangdong_etl"("taskid" text) OWNER TO "TXDB";