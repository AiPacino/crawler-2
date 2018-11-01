﻿CREATE OR REPLACE FUNCTION "public"."pro_cmbc_debit_etl"("taskid" text)
  RETURNS "pg_catalog"."text" AS $BODY$BEGIN  
    
  DECLARE
		this_id text;
		p_etl_status text DEFAULT 'success';
		p_etl_error_detail text;
		p_etl_error_content text;
			
	BEGIN

		this_id = taskid;

		delete from pro_bank_debit_user_info where task_id = this_id;
		delete from pro_bank_debit_tran_detail where task_id = this_id;
		delete from pro_bank_debit_deposit_info where task_id = this_id;


--用户基本信息

		INSERT INTO pro_bank_debit_user_info
		(
			resource,task_id,createtime,basic_id_num,basic_user_name,balance,
			user_name,open_bank,card_num,account_status,open_date
		)
			select 
				'cmbcchina_debitcard_userinfo:'|| a.id::TEXT as resource,
				a.taskid as task_id,
				a.createtime as createtime,
				c.idnum as basic_id_num,
				c.name as basic_user_name,
				d.balance as balance,
				a.cifname as user_name,
				a.deptname as open_bank,
				d.acno as card_num,
				d.acstatename as account_status,
				to_char(d.opendate::timestamp,'yyyy-mm-dd') as open_date
			from cmbcchina_debitcard_userinfo a left join task_bank b
				on a.taskid = b.taskid left join basic_user_bank c
				on b.basic_user_bank_id = c.id left join cmbcchina_debitcard_depositinfo d
				on a.taskid = d.taskid
			where a.taskid = this_id
			and d.savetypename = '活期';

--交易流水信息

		INSERT INTO pro_bank_debit_tran_detail
		(
			resource,task_id,createtime,tran_type,tran_description,account_num,
			money,balance,opposite_account,tran_date
		)
			select
				'cmbcchina_debitcard_transflow:'|| a.id::TEXT as resource,
				a.taskid as task_id,
				a.createtime as createtime,
				pro_handle_debit_type(a.remark) as tran_type,
				a.remark as tran_description,
				a.acno as account_num,
				a.amount as money,
				a.balance as balance,
				a.payeeaccount as opposite_account,
				to_char(a.transdate::timestamp,'yyyy-mm-dd') as tran_date
			from cmbcchina_debitcard_transflow a 
			where a.taskid = this_id;

--定期存款信息

		INSERT INTO pro_bank_debit_deposit_info
		(
			resource,task_id,createtime,card_num,balance,currency,
			interest_begindate,interest_enddate,deposit_type,interest_rate
		)
			select
				'cmbcchina_debitcard_depositinfo:'|| a.id::TEXT as resource,
				a.taskid as task_id,
				a.createtime as createtime,
				a.acno as card_num,
				a.balance as balance,
				a.currencyname as currency,
				to_char(a.opendate::timestamp,'yyyy-mm-dd') as interest_begindate,
				to_char(a.expiredate::timestamp,'yyyy-mm-dd') as interest_enddate,
				a.savetypename as deposit_type,
				concat((a.rate::numeric)::text,'%') as interest_rate 
			from cmbcchina_debitcard_depositinfo a 
			where a.taskid = this_id
			and a.savetypename <> '活期';

--更新此任务状态

--			UPDATE task_mobile a set a.etltime = now(),a.etl_status = p_etl_status where a.taskid = this_id;

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

--ALTER FUNCTION "public"."pro_cmbc_debit_etl"("taskid" text) OWNER TO "lvyuxin";