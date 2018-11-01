package app.service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.NoSuchWindowException;
import org.openqa.selenium.Point;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.crawler.bank.json.BankJsonBean;
import com.crawler.bank.json.BankStatusCode;
import com.crawler.mobile.json.CookieJson;
import com.crawler.mobile.json.StatusCodeLogin;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.util.Cookie;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.microservice.dao.entity.crawler.bank.basic.TaskBank;
import com.microservice.dao.entity.crawler.bank.cibchina.CibCreditcardBill;
import com.microservice.dao.entity.crawler.bank.cibchina.CibCreditcardInstallment;
import com.microservice.dao.entity.crawler.bank.cibchina.CibCreditcardTransFlow;
import com.microservice.dao.entity.crawler.bank.cibchina.CibCreditcardUserInfo;
import com.microservice.dao.repository.crawler.bank.cibchina.CibCreditcardBillRepository;
import com.microservice.dao.repository.crawler.bank.cibchina.CibCreditcardInstallmentRepository;
import com.microservice.dao.repository.crawler.bank.cibchina.CibCreditcardTransFlowRepository;
import com.microservice.dao.repository.crawler.bank.cibchina.CibCreditcardUserInfoRepository;
import com.module.htmlunit.WebCrawler;
import com.module.jna.webdriver.WebDriverUnit;

import app.commontracerlog.TracerLog;
import app.service.aop.ICrawlerLogin;
import app.service.aop.ISms;

@Component
@EntityScan(basePackages = { "com.microservice.dao.entity.crawler.bank.basic","com.microservice.dao.entity.crawler.bank.cibchina"})
@EnableJpaRepositories(basePackages = { "com.microservice.dao.repository.crawler.bank.basic","com.microservice.dao.repository.crawler.bank.cibchina"})
public class CibChinaCreditService implements ICrawlerLogin, ISms{
	/**
	 * @Des 兴业登录
	 * @param bankJsonBean
	 * @author zh
	 * @throws Exception 
	 * @throws NativeException 
	 * @throws IllegalAccessException 
	 */
	@Autowired 
	private TracerLog tracerLog;
	@Autowired
	private ParserService parserService;
	@Autowired
	private TaskBankStatusService taskBankStatusService;
	@Autowired
	private CibCreditcardUserInfoRepository cibCreditcardUserInfoRepository;
	@Autowired
	private CibCreditcardInstallmentRepository cibCreditcardInstallmentRepository;
	@Autowired
	private CibCreditcardBillRepository cibCreditcardBillRepository;
	@Autowired
	private CibCreditcardTransFlowRepository cibCreditcardTransFlowRepository;
	@Autowired
	private AgentService agentService;
	@Autowired
	private CibChinaserviceLogin cibChinaserviceLogin;
	private WebDriver driver;
	
	static String LoginPage = "https://personalbank.cib.com.cn/pers/main/login.do";
	
	@Value("${webdriver.chrome.driver.path}")
	public String driverPathChrome;
	@Value("${imagePath}")
	public String OCR_FILE_PATH;

	//登陆
	@Override
	public TaskBank login(BankJsonBean bankJsonBean) {
		// TODO Auto-generated method stub
		TaskBank taskBank = taskBankStatusService.changeStatusLoginDoing(bankJsonBean);
		try {
			//手机号登陆
			if (bankJsonBean.getLoginType().equals(StatusCodeLogin.PHONE_NUM)) {
				System.out.println("手机号登陆");
				
				
			}
			//卡号登录
			if (bankJsonBean.getLoginType().equals(StatusCodeLogin.CARD_NUM)) {
				System.out.println("卡号登录");
				tracerLog.qryKeyValue("loginCombo","开始登陆兴业银行"+bankJsonBean.getLoginName());
				//打开谷歌游览器，访问兴业银行的登录页面
				WebDriver webDriver = openloginCmbChina(bankJsonBean); 
				if (webDriver==null){
					taskBank = taskBankStatusService.changeStatus(BankStatusCode.BANK_LOGIN_TIMEOUT_ERROR.getPhase(), 
							BankStatusCode.BANK_LOGIN_TIMEOUT_ERROR.getPhasestatus(),
							BankStatusCode.BANK_LOGIN_TIMEOUT_ERROR.getDescription(), 
							BankStatusCode.BANK_LOGIN_TIMEOUT_ERROR.getError_code(),true, bankJsonBean.getTaskid());
					String path = WebDriverUnit.saveScreenshotByPath(webDriver,this.getClass());
					tracerLog.addTag("网络超时,截图路径：",path);
					tracerLog.addTag("网络超时!",bankJsonBean.getTaskid());
					agentService.releaseInstance(taskBank.getCrawlerHost(), driver);
				}else{
					String windowHandle = webDriver.getWindowHandle();
					
					tracerLog.addTag("登录兴业银行，打开网页获取网页handler",windowHandle); 
					
					bankJsonBean.setWebdriverHandle(windowHandle);
					try {
						Thread.sleep(1000);
						webDriver.findElement(By.cssSelector("label[for='logintype1']")).click();
						Thread.sleep(1000);
						webDriver.findElement(By.id("loginNameTemp")).sendKeys(bankJsonBean.getLoginName());//卡号
						Thread.sleep(1000);
						webDriver.findElement(By.id("loginNextBtn")).click();
						String htmlsource2 = null;
						Pattern pattern = Pattern.compile("^[-\\+]?[\\d]*$");
						boolean a=pattern.matcher(bankJsonBean.getLoginName()).matches();
						if(bankJsonBean.getLoginName().length()>9&&a ==true){
							htmlsource2 = webDriver.getPageSource();
						}else{
							htmlsource2 = "银行账号不能少于10位！";
						}
						
						if (htmlsource2.contains("证件号码")){
							taskBank=taskBankStatusService.changeStatus(BankStatusCode.BANK_LOGIN_ERROR2.getPhase(), 
									BankStatusCode.BANK_LOGIN_ERROR2.getPhasestatus(),
									"您的账户没有开通网银，请您到兴业银行官网补全个人信息后重试.", 
									BankStatusCode.BANK_LOGIN_ERROR2.getError_code(),true, bankJsonBean.getTaskid());
							String path = WebDriverUnit.saveScreenshotByPath(webDriver,this.getClass());
							tracerLog.addTag("您的账户没有开通网银，请您到兴业银行官网补全个人信息后重试.",path);
							System.out.println("为开通网银");
							tracerLog.addTag("您的账户没有开通网银，请您到兴业银行官网补全个人信息后重试.",bankJsonBean.getTaskid());
							
						}else if (htmlsource2.contains("银行账号不能少于10位！")){
							taskBank=taskBankStatusService.changeStatus(BankStatusCode.BANK_LOGIN_LOGINNAME_ERROR.getPhase(), 
									BankStatusCode.BANK_LOGIN_LOGINNAME_ERROR.getPhasestatus(),
									BankStatusCode.BANK_LOGIN_LOGINNAME_ERROR.getDescription(), 
									BankStatusCode.BANK_LOGIN_LOGINNAME_ERROR.getError_code(),true, bankJsonBean.getTaskid());
							
							String path = WebDriverUnit.saveScreenshotByPath(webDriver,this.getClass());
							tracerLog.addTag("银行账号不能少于10位,截图路径：",path);
							tracerLog.addTag("银行账号不能少于10位！",bankJsonBean.getTaskid());
						}else if(htmlsource2.contains("自定义登录名")||htmlsource2.contains("客户号*")){
							Thread.sleep(1000);
							webDriver.findElement(By.id("iloginPwd")).sendKeys(bankJsonBean.getPassword());
							webDriver.findElement(By.id("loginSubmitBtn")).click();
							Thread.sleep(1000);
							String html = webDriver.getPageSource();
							//System.out.println(html);
							if (html.contains("您登录网上银行")){
								String path = WebDriverUnit.saveScreenshotByPath(webDriver,this.getClass());
								tracerLog.addTag("登陆成功,截图路径：",path);
								tracerLog.addTag("登陆成功",bankJsonBean.getTaskid());
//								getDateCombo(bankJsonBean);
//								tracerLog.addTag("爬取成功",bankJsonBean.getTaskid());
							}else{
								Document doc = Jsoup.parse(html);
								String eles = doc.select("div.cib-dialog-msg").text();
								System.out.println(eles);
								if(eles.contains("对不起，登录方式、登录名或登录密码错误")){
									tracerLog.addTag("页面弹框",eles);
									eles = eles.substring(0,eles.indexOf("。"));
									System.out.println(eles);
									taskBank=taskBankStatusService.changeStatus(BankStatusCode.BANK_LOGIN_LOGINNAME_PWD_ERROR.getPhase(), 
											BankStatusCode.BANK_LOGIN_LOGINNAME_PWD_ERROR.getPhasestatus(),
											eles,
											BankStatusCode.BANK_LOGIN_LOGINNAME_PWD_ERROR.getError_code(),true, bankJsonBean.getTaskid());
									String path = WebDriverUnit.saveScreenshotByPath(webDriver,this.getClass());
									tracerLog.addTag("您输入的登录名或密码错误,截图路径：",path);
									tracerLog.addTag("您输入的登录名或密码错误!",bankJsonBean.getTaskid());
									
								}else if(eles.contains("您的登录密码连续输错6次")){
									tracerLog.addTag("页面弹框",eles);
									eles = eles.substring(0,eles.indexOf("。"));
									System.out.println(eles);
									taskBank=taskBankStatusService.changeStatus(BankStatusCode.BANK_LOGIN_LOGINNAME_PWD_ERROR.getPhase(), 
											BankStatusCode.BANK_LOGIN_LOGINNAME_PWD_ERROR.getPhasestatus(),
											eles, 
											BankStatusCode.BANK_LOGIN_LOGINNAME_PWD_ERROR.getError_code(),true, bankJsonBean.getTaskid());
									String path = WebDriverUnit.saveScreenshotByPath(webDriver,this.getClass());
									tracerLog.addTag("您输入的登录名或密码错误,截图路径：",path);
									tracerLog.addTag("您输入的登录名或密码错误!",bankJsonBean.getTaskid());
								}else{
									tracerLog.addTag("页面弹框",eles);
									taskBank=taskBankStatusService.changeStatus(BankStatusCode.BANK_LOGIN_LOGINNAME_PWD_ERROR.getPhase(), 
											BankStatusCode.BANK_LOGIN_LOGINNAME_PWD_ERROR.getPhasestatus(),
											"您输入的银行卡账号或密码错误!",
											BankStatusCode.BANK_LOGIN_LOGINNAME_PWD_ERROR.getError_code(),true, bankJsonBean.getTaskid());
									String path = WebDriverUnit.saveScreenshotByPath(webDriver,this.getClass());
									tracerLog.addTag("您输入的登录名或密码错误,截图路径：",path);
									tracerLog.addTag("您输入的登录名或密码错误!",bankJsonBean.getTaskid());
								}
								
							}
							
							
						}else{
							taskBank=taskBankStatusService.changeStatus(BankStatusCode.BANK_LOGIN_LOGINNAME_ERROR.getPhase(), 
									BankStatusCode.BANK_LOGIN_LOGINNAME_ERROR.getPhasestatus(),
									"您输入的银行卡账号有误!", 
									BankStatusCode.BANK_LOGIN_LOGINNAME_ERROR.getError_code(),true, bankJsonBean.getTaskid());
							String path = WebDriverUnit.saveScreenshotByPath(webDriver,this.getClass());
							tracerLog.addTag("银行账号未遇到过的情况,截图路径：",path);
							tracerLog.addTag("银行账号未遇到过的情况",bankJsonBean.getTaskid());

						}
						agentService.releaseInstance(taskBank.getCrawlerHost(), driver);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
//						//截图 
//						try {
//							String path = WebDriverUnit.saveScreenshotByPath(webDriver,this.getClass());
//						} catch (Exception e1) {
//							// TODO Auto-generated catch block
//							e1.printStackTrace();
//							agentService.releaseInstance(taskBank.getCrawlerHost(), driver);
//						}	
						String path = WebDriverUnit.saveScreenshotByPath(webDriver,this.getClass());
						tracerLog.addTag("打开网页获取网页异常,截图路径：",path);
						taskBankStatusService.changeStatus(BankStatusCode.BANK_VALIDATE_CODE_ERROR1.getPhase(), 
								BankStatusCode.BANK_VALIDATE_CODE_ERROR1.getPhasestatus(),
								"系统繁忙，请稍后再试！", 
								BankStatusCode.BANK_VALIDATE_CODE_ERROR1.getError_code(),true, bankJsonBean.getTaskid());
						tracerLog.addTag("登录兴业银行（信用卡），打开网页获取网页异常",e.toString()); 
						agentService.releaseInstance(taskBank.getCrawlerHost(), driver);
					}
				}
			}
			//登录名登陆
			if (bankJsonBean.getLoginType().equals(StatusCodeLogin.ACCOUNT_NUM)) {
				System.out.println("登录名登陆");
				tracerLog.qryKeyValue("loginCombo","开始登陆兴业银行"+bankJsonBean.getLoginName());
				//打开谷歌游览器，访问兴业银行的登录页面
				WebDriver webDriver = openloginCmbChina(bankJsonBean); 
				if (webDriver==null){
					taskBank = taskBankStatusService.changeStatus(BankStatusCode.BANK_LOGIN_TIMEOUT_ERROR.getPhase(), 
							BankStatusCode.BANK_LOGIN_TIMEOUT_ERROR.getPhasestatus(),
							BankStatusCode.BANK_LOGIN_TIMEOUT_ERROR.getDescription(), 
							BankStatusCode.BANK_LOGIN_TIMEOUT_ERROR.getError_code(),true, bankJsonBean.getTaskid());
					String path = WebDriverUnit.saveScreenshotByPath(webDriver,this.getClass());
					tracerLog.addTag("网络超时,截图路径：",path);
					tracerLog.addTag("网络超时!",bankJsonBean.getTaskid());
					agentService.releaseInstance(taskBank.getCrawlerHost(), driver);
				}else{
					try {
						
						String windowHandle = webDriver.getWindowHandle();
						bankJsonBean.setWebdriverHandle(windowHandle);
						tracerLog.addTag("登录兴业银行，打开网页获取网页handler",windowHandle); 
						Thread.sleep(1000);
						webDriver.findElement(By.cssSelector("label[for='logintype2']")).click();
						Thread.sleep(1000);
						webDriver.findElement(By.id("loginNameTemp")).sendKeys(bankJsonBean.getLoginName());//登录名
						Thread.sleep(1000);
						webDriver.findElement(By.id("iloginPwd")).sendKeys(bankJsonBean.getPassword());//登陆密码
						Thread.sleep(1000);
						webDriver.findElement(By.id("loginSubmitBtn")).click();
						Thread.sleep(1000);
						if (webDriver.getPageSource().contains("您登录网上银行")){
							String path = WebDriverUnit.saveScreenshotByPath(webDriver,this.getClass());
							tracerLog.addTag("登陆成功,截图路径：",path);
							tracerLog.addTag("登陆成功",bankJsonBean.getTaskid());
//							getDateCombo(bankJsonBean);
//							tracerLog.addTag("爬去完成",bankJsonBean.getTaskid());
						}else{
							Document doc = Jsoup.parse(webDriver.getPageSource());
							String eles = doc.select("div.cib-dialog-msg").text();
							System.out.println(eles);
							if(eles.contains("对不起，登录方式、登录名或登录密码错误")){
								tracerLog.addTag("页面弹框",eles);
								eles = eles.substring(0,eles.indexOf("。"));
								System.out.println(eles);
								taskBank=taskBankStatusService.changeStatus(BankStatusCode.BANK_LOGIN_LOGINNAME_PWD_ERROR.getPhase(), 
										BankStatusCode.BANK_LOGIN_LOGINNAME_PWD_ERROR.getPhasestatus(),
										eles,
										BankStatusCode.BANK_LOGIN_LOGINNAME_PWD_ERROR.getError_code(),true, bankJsonBean.getTaskid());
								String path = WebDriverUnit.saveScreenshotByPath(webDriver,this.getClass());
								tracerLog.addTag("您输入的登录名或密码错误,截图路径：",path);
								tracerLog.addTag("您输入的登录名或密码错误!",bankJsonBean.getTaskid());
								
							}else if(eles.contains("您的登录密码连续输错6次")){
								tracerLog.addTag("页面弹框",eles);
								eles = eles.substring(0,eles.indexOf("。"));
								System.out.println(eles);
								taskBank=taskBankStatusService.changeStatus(BankStatusCode.BANK_LOGIN_LOGINNAME_PWD_ERROR.getPhase(), 
										BankStatusCode.BANK_LOGIN_LOGINNAME_PWD_ERROR.getPhasestatus(),
										eles, 
										BankStatusCode.BANK_LOGIN_LOGINNAME_PWD_ERROR.getError_code(),true, bankJsonBean.getTaskid());
								String path = WebDriverUnit.saveScreenshotByPath(webDriver,this.getClass());
								tracerLog.addTag("您输入的登录名或密码错误,截图路径：",path);
								tracerLog.addTag("您输入的登录名或密码错误!",bankJsonBean.getTaskid());
							}else{
								tracerLog.addTag("页面弹框",eles);
								taskBank=taskBankStatusService.changeStatus(BankStatusCode.BANK_LOGIN_LOGINNAME_PWD_ERROR.getPhase(), 
										BankStatusCode.BANK_LOGIN_LOGINNAME_PWD_ERROR.getPhasestatus(),
										BankStatusCode.BANK_LOGIN_LOGINNAME_PWD_ERROR.getDescription(),
										BankStatusCode.BANK_LOGIN_LOGINNAME_PWD_ERROR.getError_code(),true, bankJsonBean.getTaskid());
								String path = WebDriverUnit.saveScreenshotByPath(webDriver,this.getClass());
								tracerLog.addTag("您输入的登录名或密码错误,截图路径：",path);
								tracerLog.addTag("您输入的登录名或密码错误!",bankJsonBean.getTaskid());
							}
							
						}
						agentService.releaseInstance(taskBank.getCrawlerHost(), driver);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
//						//截图 
//						try {
//							String path = WebDriverUnit.saveScreenshotByPath(webDriver,this.getClass());
//						} catch (Exception e1) {
//							// TODO Auto-generated catch block
//							e1.printStackTrace();
//							agentService.releaseInstance(taskBank.getCrawlerHost(), driver);
//						}
						String path = WebDriverUnit.saveScreenshotByPath(webDriver,this.getClass());
						tracerLog.addTag("打开网页获取网页异常,截图路径：",path);
						taskBankStatusService.changeStatus(BankStatusCode.BANK_VALIDATE_CODE_ERROR1.getPhase(), 
								BankStatusCode.BANK_VALIDATE_CODE_ERROR1.getPhasestatus(),
								"系统繁忙，请稍后再试！", 
								BankStatusCode.BANK_VALIDATE_CODE_ERROR1.getError_code(),true, bankJsonBean.getTaskid());
						tracerLog.addTag("登录兴业银行（信用卡），打开网页获取网页异常",e.toString()); 
						agentService.releaseInstance(taskBank.getCrawlerHost(), driver);
					}
				}
			}
			//客户号登陆
			if (bankJsonBean.getLoginType().equals(StatusCodeLogin.CO_BRANDED_CARD)) {
				System.out.println("客户号登陆");
				tracerLog.addTag("loginCombo","开始登陆兴业银行"+bankJsonBean.getLoginName());
				//打开谷歌游览器，访问兴业银行的登录页面
				WebDriver webDriver = openloginCmbChina(bankJsonBean); 
				
				if (webDriver==null){
					taskBank = taskBankStatusService.changeStatus(BankStatusCode.BANK_LOGIN_TIMEOUT_ERROR.getPhase(), 
							BankStatusCode.BANK_LOGIN_TIMEOUT_ERROR.getPhasestatus(),
							BankStatusCode.BANK_LOGIN_TIMEOUT_ERROR.getDescription(), 
							BankStatusCode.BANK_LOGIN_TIMEOUT_ERROR.getError_code(),true, bankJsonBean.getTaskid());
					String path = WebDriverUnit.saveScreenshotByPath(webDriver,this.getClass());
					tracerLog.addTag("网络超时,截图路径：",path);
					tracerLog.addTag("网络超时!",bankJsonBean.getTaskid());
					agentService.releaseInstance(taskBank.getCrawlerHost(), driver);
				}else{
					String windowHandle = webDriver.getWindowHandle();
					
					tracerLog.addTag("登录兴业银行，打开网页获取网页handler",windowHandle); 
					
					bankJsonBean.setWebdriverHandle(windowHandle);
					try {
						Thread.sleep(1000);
						
						webDriver.findElement(By.cssSelector("label[for='logintype0']")).click();
						Thread.sleep(1000);
						webDriver.findElement(By.id("loginNameTemp")).sendKeys(bankJsonBean.getLoginName());//登录名
						Thread.sleep(1000);
						webDriver.findElement(By.id("iloginPwd")).sendKeys(bankJsonBean.getPassword());//登陆密码
						Thread.sleep(1000);
						webDriver.findElement(By.id("loginSubmitBtn")).click();
						Thread.sleep(1000);
						if (webDriver.getPageSource().contains("您登录网上银行")){
							String path = WebDriverUnit.saveScreenshotByPath(webDriver,this.getClass());
							tracerLog.addTag("登陆成功,截图路径：",path);
							tracerLog.addTag("登陆成功",bankJsonBean.getTaskid());
//							getDateCombo(bankJsonBean);
//							tracerLog.addTag("爬去完成",bankJsonBean.getTaskid());
						}else{
							Document doc = Jsoup.parse(webDriver.getPageSource());
							String eles = doc.select("div.cib-dialog-msg").text();
							System.out.println(eles);
							if(eles.contains("对不起，登录方式、登录名或登录密码错误")){
								tracerLog.addTag("页面弹框",eles);
								eles = eles.substring(0,eles.indexOf("。"));
								System.out.println(eles);
								taskBank=taskBankStatusService.changeStatus(BankStatusCode.BANK_LOGIN_LOGINNAME_PWD_ERROR.getPhase(), 
										BankStatusCode.BANK_LOGIN_LOGINNAME_PWD_ERROR.getPhasestatus(),
										eles,
										BankStatusCode.BANK_LOGIN_LOGINNAME_PWD_ERROR.getError_code(),true, bankJsonBean.getTaskid());
								String path = WebDriverUnit.saveScreenshotByPath(webDriver,this.getClass());
								tracerLog.addTag("您输入的登录名或密码错误,截图路径：",path);
								tracerLog.addTag("您输入的登录名或密码错误!",bankJsonBean.getTaskid());
								
							}else if(eles.contains("您的登录密码连续输错6次")){
								tracerLog.addTag("页面弹框",eles);
								eles = eles.substring(0,eles.indexOf("。"));
								System.out.println(eles);
								taskBank=taskBankStatusService.changeStatus(BankStatusCode.BANK_LOGIN_LOGINNAME_PWD_ERROR.getPhase(), 
										BankStatusCode.BANK_LOGIN_LOGINNAME_PWD_ERROR.getPhasestatus(),
										eles, 
										BankStatusCode.BANK_LOGIN_LOGINNAME_PWD_ERROR.getError_code(),true, bankJsonBean.getTaskid());
								String path = WebDriverUnit.saveScreenshotByPath(webDriver,this.getClass());
								tracerLog.addTag("您输入的登录名或密码错误,截图路径：",path);
								tracerLog.addTag("您输入的登录名或密码错误!",bankJsonBean.getTaskid());
							}else{
								tracerLog.addTag("页面弹框",eles);
								taskBank=taskBankStatusService.changeStatus(BankStatusCode.BANK_LOGIN_LOGINNAME_PWD_ERROR.getPhase(), 
										BankStatusCode.BANK_LOGIN_LOGINNAME_PWD_ERROR.getPhasestatus(),
										"您输入的客户号或密码错误!",
										BankStatusCode.BANK_LOGIN_LOGINNAME_PWD_ERROR.getError_code(),true, bankJsonBean.getTaskid());
								String path = WebDriverUnit.saveScreenshotByPath(webDriver,this.getClass());
								tracerLog.addTag("您输入的登录名或密码错误,截图路径：",path);
								tracerLog.addTag("您输入的登录名或密码错误!",bankJsonBean.getTaskid());
							}
							
						}
						agentService.releaseInstance(taskBank.getCrawlerHost(), driver);
					} catch (Exception e) {
						e.printStackTrace();
//						//截图 
//						try {
//							String path = WebDriverUnit.saveScreenshotByPath(webDriver,this.getClass());
//						} catch (Exception e1) {
//							// TODO Auto-generated catch block
//							e1.printStackTrace();
//							agentService.releaseInstance(taskBank.getCrawlerHost(), driver);
//						}
						String path = WebDriverUnit.saveScreenshotByPath(webDriver,this.getClass());
						tracerLog.addTag("打开网页获取网页异常,截图路径：",path);
						taskBankStatusService.changeStatus(BankStatusCode.BANK_VALIDATE_CODE_ERROR1.getPhase(), 
								BankStatusCode.BANK_VALIDATE_CODE_ERROR1.getPhasestatus(),
								"系统繁忙，请稍后再试！", 
								BankStatusCode.BANK_VALIDATE_CODE_ERROR1.getError_code(),true, bankJsonBean.getTaskid());
						tracerLog.addTag("登录兴业银行（信用卡），打开网页获取网页异常",e.toString()); 
						agentService.releaseInstance(taskBank.getCrawlerHost(), driver);
					}
				}
			} 
    
		} catch (Exception e) {
			tracerLog.addTag("CibChinaService.login.getHtml", "error");
			tracerLog.addTag("login.getHtml.error", e.getMessage());
			taskBankStatusService.changeStatus(BankStatusCode.BANK_LOGIN_CAPTCHA_ERROR.getPhase(), 
					BankStatusCode.BANK_LOGIN_CAPTCHA_ERROR.getPhasestatus(), 
					BankStatusCode.BANK_VALIDATE_CODE_ERROR1.getDescription(), 
					BankStatusCode.BANK_LOGIN_CAPTCHA_ERROR.getError_code(),false,bankJsonBean.getTaskid());
			//释放机器，关闭driver进程
			agentService.releaseInstance(taskBank.getCrawlerHost(), driver);
		}
		return taskBank;
	} 
	

	//发送短信
	@Override
	public TaskBank sendSms(BankJsonBean bankJsonBean) {
		// TODO Auto-generated method stub
		TaskBank taskBank = taskBankStatusService.changeStatusLoginDoing(bankJsonBean);	
		try {
			driver = cibChinaserviceLogin.login(bankJsonBean);
		     if (driver!=null){
		    	 String htmlsource1 = driver.getPageSource();
					if(htmlsource1.contains("验证码不能为空")){
						taskBankStatusService.changeStatus(BankStatusCode.BANK_LOGIN_PWD_ERROR.getPhase(), 
								BankStatusCode.BANK_LOGIN_PWD_ERROR.getPhasestatus(),
								"系统繁忙，请稍后再试！", 
								BankStatusCode.BANK_LOGIN_PWD_ERROR.getError_code(),false, bankJsonBean.getTaskid());
						//释放机器，关闭driver进程
						agentService.releaseInstance(taskBank.getCrawlerHost(), driver);
					}else {
						Wait<WebDriver> wait = new FluentWait<WebDriver>(driver).withTimeout(20, TimeUnit.SECONDS).pollingEvery(5, TimeUnit.SECONDS).ignoring(NoSuchElementException.class);  
						String htmlsource2 = null;
						try {
							htmlsource2 = wait.until(new Function<WebDriver, String>() {  
					             public String apply(WebDriver driver) {   
					            	return driver.getPageSource(); //这是不需要短信验证码，直接进入主页面的div的 ID 
					             }  
							});  
				     	} catch (Exception e) {
//				     		//详单主页面未出现    截图 
//							String path = WebDriverUnit.saveScreenshotByPath(driver,this.getClass());
				     		taskBankStatusService.changeStatus(BankStatusCode.BANK_VALIDATE_CODE_ERROR1.getPhase(), 
									BankStatusCode.BANK_VALIDATE_CODE_ERROR1.getPhasestatus(),
									BankStatusCode.BANK_VALIDATE_CODE_ERROR1.getDescription(), 
									BankStatusCode.BANK_VALIDATE_CODE_ERROR1.getError_code(),true, bankJsonBean.getTaskid());
				     		String path = WebDriverUnit.saveScreenshotByPath(driver,this.getClass());
							tracerLog.addTag("打开网页获取网页异常,截图路径：",path);
				     		tracerLog.addTag("登录兴业银行（信用卡），打开网页获取网页异常",e.toString()); 
							System.out.println("图片验证码错误从新登陆");
							agentService.releaseInstance(taskBank.getCrawlerHost(), driver);
				     	}
						try {
							Document doc = Jsoup.parse(htmlsource2);
							String eles = doc.select("#sms-lefttime").text();
							System.out.println(eles);
							String htmlsource5 = driver.getPageSource();
							if (eles.contains("已发送")) {
								taskBank=taskBankStatusService.changeStatus("LOGIN", 
										BankStatusCode.BANK_LOGIN_SUCCESS_NEEDSMS.getPhasestatus(),
										"短信验证码已发送到您的手机："+bankJsonBean.getLoginName()+"，请注意查收", 
										BankStatusCode.BANK_LOGIN_SUCCESS_NEEDSMS.getError_code(),false, bankJsonBean.getTaskid());
								System.out.println("获取短信成功");
								String path = WebDriverUnit.saveScreenshotByPath(driver,this.getClass());
								tracerLog.addTag("短信验证码已发送，请注意查收预留手机号"+bankJsonBean.getLoginName()+",截图路径：",path);
								tracerLog.addTag("获取短信成功",bankJsonBean.getTaskid());
							}else if(htmlsource5.contains("对不起，您发送短信频率过于频繁，请稍候再发！")){
								taskBank=taskBankStatusService.changeStatus(BankStatusCode.BANK_SEND_CODE_ERROR.getPhase(), 
										BankStatusCode.BANK_SEND_CODE_ERROR.getPhasestatus(),
										"对不起，您发送短信频率过于频繁，请稍候再发！", 
										BankStatusCode.BANK_SEND_CODE_ERROR.getError_code(),true, bankJsonBean.getTaskid());
								System.out.println("获取短信失败从新登陆");
								String path = WebDriverUnit.saveScreenshotByPath(driver,this.getClass());
								tracerLog.addTag("获取短信失败从新登陆,截图路径：",path);
								tracerLog.addTag("获取短信失败从新登陆",bankJsonBean.getTaskid());
								agentService.releaseInstance(taskBank.getCrawlerHost(), driver);
								//webDriver.quit();
							}else {
								taskBank=taskBankStatusService.changeStatus(BankStatusCode.BANK_SEND_CODE_ERROR.getPhase(), 
										BankStatusCode.BANK_SEND_CODE_ERROR.getPhasestatus(),
										BankStatusCode.BANK_SEND_CODE_ERROR.getDescription(), 
										BankStatusCode.BANK_SEND_CODE_ERROR.getError_code(),true, bankJsonBean.getTaskid());
								System.out.println("获取短信失败从新登陆");
								String path = WebDriverUnit.saveScreenshotByPath(driver,this.getClass());
								tracerLog.addTag("获取短信失败从新登陆,截图路径：",path);
								tracerLog.addTag("获取短信失败从新登陆",bankJsonBean.getTaskid());
								agentService.releaseInstance(taskBank.getCrawlerHost(), driver);
								//webDriver.quit();
							}
						}catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
//							//截图 
//							try {
//								String path = WebDriverUnit.saveScreenshotByPath(webDriver,this.getClass());
//							} catch (Exception e1) {
//								// TODO Auto-generated catch block
//								e1.printStackTrace();
//								agentService.releaseInstance(taskBank.getCrawlerHost(), driver);
//							}
							taskBankStatusService.changeStatus(BankStatusCode.BANK_VALIDATE_CODE_ERROR1.getPhase(), 
									BankStatusCode.BANK_VALIDATE_CODE_ERROR1.getPhasestatus(),
									"系统繁忙，请稍后再试！", 
									BankStatusCode.BANK_VALIDATE_CODE_ERROR1.getError_code(),true, bankJsonBean.getTaskid());
							String path = WebDriverUnit.saveScreenshotByPath(driver,this.getClass());
							tracerLog.addTag("打开网页获取网页异常,截图路径：",path);
							tracerLog.addTag("登录兴业银行，打开网页获取网页异常",e.toString()); 
							agentService.releaseInstance(taskBank.getCrawlerHost(), driver);
							//webDriver.quit();
						}
						
						
					}
		     }else {
		    	 taskBankStatusService.changeStatus(BankStatusCode.BANK_VALIDATE_CODE_ERROR1.getPhase(), 
							BankStatusCode.BANK_VALIDATE_CODE_ERROR1.getPhasestatus(),
							"系统繁忙，请稍后再试！", 
							BankStatusCode.BANK_VALIDATE_CODE_ERROR1.getError_code(),true, bankJsonBean.getTaskid());
					String path = WebDriverUnit.saveScreenshotByPath(driver,this.getClass());
					tracerLog.addTag("打开网页获取网页异常,截图路径：",path);
//					tracerLog.addTag("登录兴业银行，打开网页获取网页异常",e.toString()); 
					agentService.releaseInstance(taskBank.getCrawlerHost(), driver);
		     }
		     
		} catch (Exception e) {
			tracerLog.addTag("CibChinaService.login.getHtml", "error");
			tracerLog.addTag("login.getHtml.error", e.getMessage());
			taskBankStatusService.changeStatus(BankStatusCode.BANK_LOGIN_CAPTCHA_ERROR.getPhase(), 
					BankStatusCode.BANK_LOGIN_CAPTCHA_ERROR.getPhasestatus(), 
					BankStatusCode.BANK_VALIDATE_CODE_ERROR1.getDescription(), 
					BankStatusCode.BANK_LOGIN_CAPTCHA_ERROR.getError_code(),false,bankJsonBean.getTaskid());
			//释放机器，关闭driver进程
			agentService.releaseInstance(taskBank.getCrawlerHost(), driver);
		}
		return null;
	}
	
	
	//手机号登陆输入验证码验证
	@Override
	public TaskBank verifySms(BankJsonBean bankJsonBean) {
		// TODO Auto-generated method stub
		TaskBank taskBank = taskBankStatusService.changeStatusLoginDoing(bankJsonBean);
		try {
			driver.findElement(By.id("mobLogin_sendsms")).clear();
			driver.findElement(By.id("mobLogin_sendsms")).sendKeys(bankJsonBean.getVerification());
			//TaskBank taskBank = null;
			
				Thread.sleep(500L);
				
				String htmlsource2 = driver.getPageSource();
				if (htmlsource2.contains("证件号码")) {
					taskBank = taskBankStatusService.changeStatus(BankStatusCode.BANK_LOGIN_ERROR2.getPhase(), 
							BankStatusCode.BANK_LOGIN_ERROR2.getPhasestatus(),
							"您的账户没有开通网银，请您到兴业银行官网补全个人信息后重试.", 
							BankStatusCode.BANK_LOGIN_ERROR2.getError_code(),true, bankJsonBean.getTaskid());
					String path = WebDriverUnit.saveScreenshotByPath(driver,this.getClass());
					tracerLog.addTag("短信验证码正确,但为开通网银,截图路径：",path);
					tracerLog.addTag("短信验证码正确,但为开通网银",bankJsonBean.getTaskid());
					System.out.println("短信验证码正确,但为开通网银");
				}else if(htmlsource2.contains("手机号码")) {
					taskBank = taskBankStatusService.changeStatus(BankStatusCode.BANK_VALIDATE_CODE_SUCCESS.getPhase(), 
							BankStatusCode.BANK_VALIDATE_CODE_SUCCESS.getPhasestatus(),
							BankStatusCode.BANK_VALIDATE_CODE_SUCCESS.getDescription(), 
							BankStatusCode.BANK_VALIDATE_CODE_SUCCESS.getError_code(),false, bankJsonBean.getTaskid());
					driver.findElement(By.id("iloginPwd")).sendKeys(bankJsonBean.getPassword());//登陆密码
					driver.findElement(By.id("loginSubmitBtn")).click();
					Thread.sleep(1000);
					if (driver.getPageSource().contains("您登录网上银行")){
						String path = WebDriverUnit.saveScreenshotByPath(driver,this.getClass());
						tracerLog.addTag("登陆成功,截图路径：",path);
						tracerLog.addTag("登陆成功",bankJsonBean.getTaskid());
//						getDateCombo(bankJsonBean);
//						tracerLog.addTag("爬去完成",bankJsonBean.getTaskid());
					}else{
						Document doc = Jsoup.parse(driver.getPageSource());
						String eles = doc.select("div.cib-dialog-msg").text();
						System.out.println(eles);
						if(eles.contains("对不起，登录方式、登录名或登录密码错误")){
							tracerLog.addTag("页面弹框",eles);
							eles = eles.substring(0,eles.indexOf("。"));
							System.out.println(eles);
							taskBank=taskBankStatusService.changeStatus(BankStatusCode.BANK_LOGIN_LOGINNAME_PWD_ERROR.getPhase(), 
									BankStatusCode.BANK_LOGIN_LOGINNAME_PWD_ERROR.getPhasestatus(),
									eles,
									BankStatusCode.BANK_LOGIN_LOGINNAME_PWD_ERROR.getError_code(),true, bankJsonBean.getTaskid());
							String path = WebDriverUnit.saveScreenshotByPath(driver,this.getClass());
							tracerLog.addTag("您输入的登录名或密码错误,截图路径：",path);
							tracerLog.addTag("您输入的登录名或密码错误!",bankJsonBean.getTaskid());
							
						}else if(eles.contains("您的登录密码连续输错6次")){
							tracerLog.addTag("页面弹框",eles);
							eles = eles.substring(0,eles.indexOf("。"));
							System.out.println(eles);
							taskBank=taskBankStatusService.changeStatus(BankStatusCode.BANK_LOGIN_LOGINNAME_PWD_ERROR.getPhase(), 
									BankStatusCode.BANK_LOGIN_LOGINNAME_PWD_ERROR.getPhasestatus(),
									eles, 
									BankStatusCode.BANK_LOGIN_LOGINNAME_PWD_ERROR.getError_code(),true, bankJsonBean.getTaskid());
							String path = WebDriverUnit.saveScreenshotByPath(driver,this.getClass());
							tracerLog.addTag("您输入的登录名或密码错误,截图路径：",path);
							tracerLog.addTag("您输入的登录名或密码错误!",bankJsonBean.getTaskid());
						}else{
							tracerLog.addTag("页面弹框",eles);
							taskBank=taskBankStatusService.changeStatus(BankStatusCode.BANK_LOGIN_PWD_ERROR.getPhase(), 
									BankStatusCode.BANK_LOGIN_PWD_ERROR.getPhasestatus(),
									BankStatusCode.BANK_LOGIN_PWD_ERROR.getDescription(),
									BankStatusCode.BANK_LOGIN_PWD_ERROR.getError_code(),true, bankJsonBean.getTaskid());
							String path = WebDriverUnit.saveScreenshotByPath(driver,this.getClass());
							tracerLog.addTag("您输入的登录名或密码错误,截图路径：",path);
							tracerLog.addTag("您输入的登录名或密码错误!",bankJsonBean.getTaskid());
						}
						
					}
//					System.out.println("短信验证码正确");
				}else if(htmlsource2.contains("短信认证码错误")){
					taskBank = taskBankStatusService.changeStatus(BankStatusCode.BANK_VALIDATE_CODE_ERROR.getPhase(), 
							BankStatusCode.BANK_VALIDATE_CODE_ERROR.getPhasestatus(),
							BankStatusCode.BANK_VALIDATE_CODE_ERROR.getDescription(), 
							BankStatusCode.BANK_VALIDATE_CODE_ERROR.getError_code(),true, bankJsonBean.getTaskid());
					String path = WebDriverUnit.saveScreenshotByPath(driver,this.getClass());
					tracerLog.addTag("短信验证码失败,截图路径：",path);
					tracerLog.addTag("短信验证码失败",bankJsonBean.getTaskid());
					System.out.println("短信验证码失败");
					//driver.quit();
				}else if(htmlsource2.contains("对不起，该手机号未在网银登记或无对应银行卡，建议使用“卡号”方式登录。")){
					taskBank = taskBankStatusService.changeStatus(BankStatusCode.BANK_LOGIN_LOGINNAME_ERROR.getPhase(), 
							BankStatusCode.BANK_LOGIN_LOGINNAME_ERROR.getPhasestatus(),
							"对不起，该手机号未在网银登记或无对应银行卡，建议使用“卡号”方式登录。", 
							BankStatusCode.BANK_LOGIN_LOGINNAME_ERROR.getError_code(),true, bankJsonBean.getTaskid());
					String path = WebDriverUnit.saveScreenshotByPath(driver,this.getClass());
					tracerLog.addTag("对不起，该手机号未在网银登记或无对应银行卡，建议使用“卡号”方式登录。截图路径：",path);
					tracerLog.addTag("对不起，该手机号未在网银登记或无对应银行卡，建议使用“卡号”方式登录。",bankJsonBean.getTaskid());
					System.out.println("对不起，该手机号未在网银登记或无对应银行卡，建议使用“卡号”方式登录。");
					//driver.quit();
				}else {
					taskBank = taskBankStatusService.changeStatus(BankStatusCode.BANK_VALIDATE_CODE_ERROR.getPhase(), 
							BankStatusCode.BANK_VALIDATE_CODE_ERROR.getPhasestatus(),
							BankStatusCode.BANK_VALIDATE_CODE_ERROR.getDescription(), 
							BankStatusCode.BANK_VALIDATE_CODE_ERROR.getError_code(),true, bankJsonBean.getTaskid());
					String path = WebDriverUnit.saveScreenshotByPath(driver,this.getClass());
					tracerLog.addTag("短信验证码失败,截图路径：",path);
					tracerLog.addTag("短信验证码失败",bankJsonBean.getTaskid());
					System.out.println("短信验证码失败");
					//driver.quit();
				}
				
				agentService.releaseInstance(taskBank.getCrawlerHost(), driver);
				//driver.quit();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				//截图 
//				try {
//					String path = WebDriverUnit.saveScreenshotByPath(driver,this.getClass());
//				} catch (Exception e1) {
//					// TODO Auto-generated catch block
//					e1.printStackTrace();
//					agentService.releaseInstance(taskBank.getCrawlerHost(), driver);
//				}	
				String path = null;
				try {
					path = WebDriverUnit.saveScreenshotByPath(driver,this.getClass());
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				tracerLog.addTag("打开网页获取网页异常,截图路径：",path);
				taskBankStatusService.changeStatus(BankStatusCode.BANK_VALIDATE_CODE_ERROR1.getPhase(), 
						BankStatusCode.BANK_VALIDATE_CODE_ERROR1.getPhasestatus(),
						"系统繁忙，请稍后再试！", 
						BankStatusCode.BANK_VALIDATE_CODE_ERROR1.getError_code(),true, bankJsonBean.getTaskid());
				tracerLog.addTag("登录兴业银行（信用卡），打开网页获取网页异常",e.toString()); 
				agentService.releaseInstance(taskBank.getCrawlerHost(), driver);
				//driver.quit();
			}
			
			
			return taskBank;
		
	}
	
	//爬取
	@Override
	public TaskBank getAllData(BankJsonBean bankJsonBean) {
		// TODO Auto-generated method stub
		// 获取cookies
		TaskBank taskBank = taskBankStatusService.changeStatusLoginDoing(bankJsonBean);
		Set<org.openqa.selenium.Cookie> cookies = driver.manage().getCookies();
		Set<CookieJson> cookiesSet = new HashSet<CookieJson>();
		for (org.openqa.selenium.Cookie cookie : cookies) {
			CookieJson cookiejson = new CookieJson();
			cookiejson.setDomain(cookie.getDomain());
			cookiejson.setKey(cookie.getName());
			cookiejson.setValue(cookie.getValue());
			cookiesSet.add(cookiejson);
		}
		String cookieJson = new Gson().toJson(cookiesSet);
		try {
			driver.switchTo().frame("workframe");
			String html1 = driver.getPageSource();
			tracerLog.addTag("正在爬取中....",bankJsonBean.getTaskid());
		
			getRegular(html1,bankJsonBean);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		return taskBank;
	}
	
	
	public WebDriver openloginCmbChina(BankJsonBean bankJsonBean)throws Exception{ 
		
		//driver.manage().window().maximize();
		
			System.out.println("launching chrome browser");
			System.setProperty("webdriver.chrome.driver", driverPathChrome);
			try {
			
			ChromeOptions chromeOptions = new ChromeOptions();
			chromeOptions.addArguments("disable-gpu"); 

			driver = new ChromeDriver(chromeOptions);
			 
			driver.manage().timeouts().pageLoadTimeout(30, TimeUnit.SECONDS);
			driver.manage().timeouts().setScriptTimeout(30, TimeUnit.SECONDS);
			
			
				//driver.manage().window().maximize();
				driver.get(LoginPage);
				return driver;
			} catch (Exception e) {
				System.out.println("网络超时");
				taskBankStatusService.changeStatus(BankStatusCode.BANK_LOGIN_TIMEOUT_ERROR.getPhase(), 
						BankStatusCode.BANK_LOGIN_TIMEOUT_ERROR.getPhasestatus(),
						BankStatusCode.BANK_LOGIN_TIMEOUT_ERROR.getDescription(), 
						BankStatusCode.BANK_LOGIN_TIMEOUT_ERROR.getError_code(),true, bankJsonBean.getTaskid());
				tracerLog.addTag("登录兴业银行，打开网页超时",e.toString()); 
				String path = WebDriverUnit.saveScreenshotByPath(driver,this.getClass());
				tracerLog.addTag("打开网页超时,截图路径：",path);
			}
		
//		tracerLog.addTag("WebDriverChromeService loginCmbChina Msg", "兴业银行登陆页加载已完成,当前页面句柄"+driver.getWindowHandle());
		return null;
		
	} 
	
	//账单分期
	public  String getData(BankJsonBean bankJsonBean,int size,int a) throws Exception, IOException{
		WebClient webClient = WebCrawler.getInstance().getNewWebClient();
		Set<org.openqa.selenium.Cookie> cookiesDriver = driver.manage().getCookies();
		for(org.openqa.selenium.Cookie cookie : cookiesDriver){
			Cookie cookieWebClient = new Cookie("personalbank.cib.com.cn", cookie.getName(), cookie.getValue());
			webClient.getCookieManager().addCookie(cookieWebClient);
		}
		String url = null;
		if (size==1){
			url = "https://personalbank.cib.com.cn/pers/creditCard/stage/queryBillStage.do?FUNID=FIN09|FIN09_08|FIN09_08_06&aliveToken=1512368489643";
		}else {
			url = "https://personalbank.cib.com.cn/pers/creditCard/stage/queryBillStage.do?FUNID=FIN09|FIN09_08|FIN09_08_06&aliveToken=1512368489643";
			WebRequest webRequest=new WebRequest(new URL(url),HttpMethod.GET);
			webClient.getOptions().setJavaScriptEnabled(false);
			HtmlPage pages = webClient.getPage(webRequest);
			url = "https://personalbank.cib.com.cn/pers/creditCard/stage/queryBillStage.do?method%3Anext=%E4%B8%8B%E4%B8%80%E6%AD%A5&selIdx="+a+"";
		}
		 
		WebRequest webRequest=new WebRequest(new URL(url),HttpMethod.GET);
		webClient.getOptions().setJavaScriptEnabled(false);
		HtmlPage pages = webClient.getPage(webRequest);
		String html=pages.getWebResponse().getContentAsString();
		String endDate = Jsoup.parse(html).select("#endDate").val();
		String str = Jsoup.parse(html).select("#form > div:nth-child(3) > span:nth-child(1)").text();
		
		String accNumber = str.substring(str.indexOf("：")+1);
		System.out.println("accNumber"+accNumber);
		System.out.println("endDate"+endDate);
		String begin = endDate.substring(0,endDate.indexOf("-"));
		String begin1 = endDate.substring(endDate.indexOf("-"));
		int year = Integer.parseInt(begin)-1;
		String beginDate = year+begin1;
		
//		if (size==1){
			url = "https://personalbank.cib.com.cn/pers/creditCard/stage/queryBillStage!query.do";
			
			WebRequest request = new WebRequest(new URL(url),HttpMethod.POST);
			request.setAdditionalHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
			request.setAdditionalHeader("Accept-Encoding", "gzip, deflate, br");
			request.setAdditionalHeader("Accept-Language", "zh-CN,zh;q=0.9");
			request.setAdditionalHeader("Cache-Control", "max-age=0");
			request.setAdditionalHeader("Connection", "keep-alive");
			request.setAdditionalHeader("Host", "personalbank.cib.com.cn");
			request.setAdditionalHeader("Origin", "https://personalbank.cib.com.cn");
			request.setAdditionalHeader("Referer", "https://personalbank.cib.com.cn/pers/creditCard/stage/queryBillStage!query.do");
		    request.setAdditionalHeader("Upgrade-Insecure-Requests", "1");
		    request.setRequestBody("selIdx="+a+"&startDate="+beginDate+"&endDate="+endDate+"&method%3Aquery=%E6%9F%A5%E8%AF%A2&withinMonth=");
			HtmlPage page1 = webClient.getPage(request);
			webClient = page1.getWebClient();
			
			url = "https://personalbank.cib.com.cn/pers/creditCard/stage/queryBillStage!list.do?_search=false&dataSet.nd=1512369293315&dataSet.rows=1000&dataSet.page=1&dataSet.sidx=&dataSet.sord=asc";
			WebRequest webRequest1=new WebRequest(new URL(url),HttpMethod.GET);
			Page page = webClient.getPage(webRequest1);
			String html1 = page.getWebResponse().getContentAsString();
			tracerLog.addTag("兴业银行（信用卡），分期账单"+accNumber,html1); 
			List<CibCreditcardInstallment> cibCreditcardInstallment = parserService.parserTranslment(html1,bankJsonBean.getTaskid(),accNumber);
			cibCreditcardInstallmentRepository.saveAll(cibCreditcardInstallment);
			tracerLog.addTag("兴业银行（信用卡），分期账单入库成功cibchina_creditcard_installment_bill"+accNumber+"",bankJsonBean.getTaskid()); 
//		}else {
//			for(int i = 0;i<size;i++){
//				url = "https://personalbank.cib.com.cn/pers/creditCard/stage/queryBillStage!query.do";
//				
//				WebRequest request = new WebRequest(new URL(url),HttpMethod.POST);
//				request.setAdditionalHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
//				request.setAdditionalHeader("Accept-Encoding", "gzip, deflate, br");
//				request.setAdditionalHeader("Accept-Language", "zh-CN,zh;q=0.9");
//				request.setAdditionalHeader("Cache-Control", "max-age=0");
//				request.setAdditionalHeader("Connection", "keep-alive");
//				request.setAdditionalHeader("Host", "personalbank.cib.com.cn");
//				request.setAdditionalHeader("Origin", "https://personalbank.cib.com.cn");
//				request.setAdditionalHeader("Referer", "https://personalbank.cib.com.cn/pers/creditCard/stage/queryBillStage!query.do");
//			    request.setAdditionalHeader("Upgrade-Insecure-Requests", "1");
//			    request.setRequestBody("selIdx="+i+"&startDate="+beginDate+"&endDate="+endDate+"&method%3Aquery=%E6%9F%A5%E8%AF%A2&withinMonth=");
//				HtmlPage page1 = webClient.getPage(request);
//				webClient = page1.getWebClient();
//				
//				url = "https://personalbank.cib.com.cn/pers/creditCard/stage/queryBillStage!list.do?_search=false&dataSet.nd=1512369293315&dataSet.rows=10&dataSet.page=1&dataSet.sidx=&dataSet.sord=asc";
//				WebRequest webRequest1=new WebRequest(new URL(url),HttpMethod.GET);
//				Page page = webClient.getPage(webRequest1);
//				String html1 = page.getWebResponse().getContentAsString();
//				List<CibCreditcardInstallment> cibCreditcardInstallment = parserService.parserTranslment(html1,bankJsonBean.getTaskid(),accNumber);
//				cibCreditcardInstallmentRepository.saveAll(cibCreditcardInstallment);
//				tracerLog.addTag("兴业银行（信用卡），分期账单入库成功cibchina_creditcard_installment_bill"+accNumber+"",bankJsonBean.getTaskid()); 
//			}
//		}
		
		return null;
		
		
	}
	
	//账单信息及账单明细
	public List<String> getBill(BankJsonBean bankJsonBean,CibCreditcardUserInfo cibCreditcardUserInfo,String trimAmount,int size) throws Exception{
		WebClient webClient = WebCrawler.getInstance().getNewWebClient();
		Set<org.openqa.selenium.Cookie> cookiesDriver = driver.manage().getCookies();
		for(org.openqa.selenium.Cookie cookie : cookiesDriver){
			Cookie cookieWebClient = new Cookie("personalbank.cib.com.cn", cookie.getName(), cookie.getValue());
			webClient.getCookieManager().addCookie(cookieWebClient);
		}
		webClient.getOptions().setJavaScriptEnabled(false);
		System.out.println("账单信息及账单明细");
		if (size==1){
			String url = "https://personalbank.cib.com.cn/pers/creditCard/query/queryPastBill.do?FUNID=FIN09|FIN09_08|FIN09_08_02|FIN09_08_02_02&aliveToken=1512372198608&loginTicket=9db3fa45e301260ecd6a1dabc9a0fe11&jumpTicket=aff67b2496e178ed3c97fb8f086090cc";
			WebRequest webRequest=new WebRequest(new URL(url),HttpMethod.GET);
			webClient.getOptions().setJavaScriptEnabled(false);
			HtmlPage pages = webClient.getPage(webRequest);
			String html=pages.getWebResponse().getContentAsString();
			Document doc = Jsoup.parse(html);
			Elements eles = doc.select("#queryYearMonth option");
			Elements eles1 = doc.select("#form > div.info-show > span:nth-child(1)");
			String str = eles1.text().trim();
			String cardNumber1 = str.substring(str.indexOf("：")+1);//卡号
			List <String> list = new ArrayList<String>();
			if (cardNumber1.contains(cibCreditcardUserInfo.getCardNumber())){
				
				String urlbill = "https://personalbank.cib.com.cn/pers/creditCard/query/queryBonus.do?FUNID=FIN09|FIN09_08|FIN09_08_03|FIN09_08_03_01&aliveToken=1512380333428";
				WebRequest webRequestbill=new WebRequest(new URL(urlbill),HttpMethod.GET);
				HtmlPage pagesbill = webClient.getPage(webRequestbill);
				String htmlbill=pagesbill.getWebResponse().getContentAsString();
				Document docbill = Jsoup.parse(htmlbill);
				Elements elesbill = docbill.select("#bonusTbl td");
				
				Elements eles2 = doc.select("#form > table > tbody > tr:nth-child(1) > td:nth-child(4)");
				if(eles2.size()>0){
					String bill = eles2.text().trim();
					System.out.println("bill"+bill);
					String bill_date = bill.substring(bill.lastIndexOf("-")+1)+"号";
					if (bill_date.contains("0")&&!bill_date.contains("10")&&!bill_date.contains("20")&&!bill_date.contains("30")){
						bill_date = bill_date.substring(bill_date.lastIndexOf("0")+1);
					}
					cibCreditcardUserInfo.setBill_date(bill_date);
				}
				String urlE = "https://personalbank.cib.com.cn/pers/creditCard/servSet/modifyCustData.do?FUNID=FIN09|FIN09_05|FIN09_05_05&aliveToken=1512696481904";
				WebRequest webRequestE=new WebRequest(new URL(urlE),HttpMethod.GET);
				HtmlPage pagesE = webClient.getPage(webRequestE);
				String htmlE=pagesE.getWebResponse().getContentAsString();
				String email = Jsoup.parse(htmlE).select("#email").val();
				String strE = Jsoup.parse(htmlE).select("#form > div.info-show > span:nth-child(1)").text().trim();
				String cardNumber3 = strE.substring(strE.indexOf("：")+1);//卡号
				tracerLog.addTag("基本信息邮箱",email);
				System.out.println(htmlE);
				if (cardNumber3.contains(cibCreditcardUserInfo.getCardNumber())){
					cibCreditcardUserInfo.setEmail(email);
					cibCreditcardUserInfoRepository.save(cibCreditcardUserInfo);
					taskBankStatusService.updateTaskBankUserinfo(BankStatusCode.BANK_USERINFO_SUCCESS.getError_code(),
							BankStatusCode.BANK_USERINFO_SUCCESS.getDescription() , bankJsonBean.getTaskid());
					tracerLog.addTag("基本信息入库成功cibchina_creditcard_userinfo"+cardNumber3,bankJsonBean.getTaskid());
				}	
					
				
				
				if (eles.size()>0){
					for (int i = 0;i<eles.size();i++){
						String date = eles.get(i).attr("value").trim();
						System.out.println("date"+date);
//						String billUrl = "https://personalbank.cib.com.cn/pers/creditCard/query/queryPastBill!query.do";
//						WebRequest webRequest3=new WebRequest(new URL(billUrl),HttpMethod.POST);
//						webRequest3.setAdditionalHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
//						webRequest3.setAdditionalHeader("Accept-Encoding", "gzip, deflate, br");
//						webRequest3.setAdditionalHeader("Accept-Language", "zh-CN,zh;q=0.9");
//						webRequest3.setAdditionalHeader("Cache-Control", "max-age=0");
//						webRequest3.setAdditionalHeader("Connection", "keep-alive");
//						webRequest3.setAdditionalHeader("Host", "personalbank.cib.com.cn");
//						webRequest3.setAdditionalHeader("Origin", "https://personalbank.cib.com.cn");
//						webRequest3.setAdditionalHeader("Referer", "https://personalbank.cib.com.cn/pers/creditCard/query/queryPastBill!query.do");
//						webRequest3.setAdditionalHeader("Upgrade-Insecure-Requests", "1");
//						webRequest3.setRequestBody("currCode=01&queryYearMonth="+date+"&credAccountNo="+cibCreditcardUserInfo.getCardNumber()+"");
						
//						if (i==0){
//							url = "https://personalbank.cib.com.cn/pers/creditCard/query/queryOngoingBill!jump.do";
//						}else {
							url = "https://personalbank.cib.com.cn/pers/creditCard/query/queryPastBill!query.do";
//						}
						
						WebRequest webRequest1=new WebRequest(new URL(url),HttpMethod.POST);
						webRequest1.setAdditionalHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
						webRequest1.setAdditionalHeader("Accept-Encoding", "gzip, deflate, br");
						webRequest1.setAdditionalHeader("Accept-Language", "zh-CN,zh;q=0.9");
						webRequest1.setAdditionalHeader("Cache-Control", "max-age=0");
						webRequest1.setAdditionalHeader("Connection", "keep-alive");
						webRequest1.setAdditionalHeader("Host", "personalbank.cib.com.cn");
						webRequest1.setAdditionalHeader("Origin", "https://personalbank.cib.com.cn");
						webRequest1.setAdditionalHeader("Referer", "https://personalbank.cib.com.cn/pers/creditCard/query/queryPastBill!jump.do");
						webRequest1.setAdditionalHeader("Upgrade-Insecure-Requests", "1");
						webRequest1.setRequestBody("currCode=01&queryYearMonth="+date+"&credAccountNo="+cibCreditcardUserInfo.getCardNumber()+"");
						HtmlPage page1 = webClient.getPage(webRequest1);
//						HtmlPage page3 = webClient.getPage(webRequest3);
						String html2=page1.getWebResponse().getContentAsString();
//						System.out.println(html2);
						if (elesbill.size()>0){
							String cardNumber2 = elesbill.get(0).text().trim();
							if (cardNumber1.contains(cardNumber2)){
								String integral = elesbill.get(2).text().trim();//积分
//								String integralAdd = elesbill.get(5).text().trim();//新增积分
//								String integralExchange = elesbill.get(6).text().trim();//本期调整积分
								CibCreditcardBill cibCreditcardBill = parserService.parserCredBill(html2,cardNumber1,trimAmount,integral,bankJsonBean.getTaskid());
								
								if(cibCreditcardBill.getBillDate()!=null){
									cibCreditcardBillRepository.save(cibCreditcardBill);
									tracerLog.addTag("账单信息HTML"+cardNumber2,html2);
									tracerLog.addTag("账单信息入库成功cibchina_creditcard_bill_info"+cardNumber2,bankJsonBean.getTaskid());	
								}
							}
						}
						webClient = page1.getWebClient();
						if(i==0) {
							url = "https://personalbank.cib.com.cn/pers/creditCard/query/queryOngoingBill!list.do?_search=false&dataSet.nd=1512442649490&dataSet.rows=1000&dataSet.page=1&dataSet.sidx=&dataSet.sord=asc";
						}else {
							url = "https://personalbank.cib.com.cn/pers/creditCard/query/queryPastBill!list.do?_search=false&dataSet.nd=1512378516752&dataSet.rows=1000&dataSet.page=1&dataSet.sidx=&dataSet.sord=asc";
						}
						
						WebRequest webRequest2=new WebRequest(new URL(url),HttpMethod.GET);
						Page pages1 = webClient.getPage(webRequest2);
						String html1=pages1.getWebResponse().getContentAsString();
						if(html1!=null&&!html1.equals("")){
							System.out.println(html1);
							list.add(html1);
						}
						
					}
				}
			}
			return list;
		}else if(size>1){
			System.out.println("size>1");
			List <String> list = new ArrayList<String>();
			for (int i = 0;i<size;i++){
				String url = "https://personalbank.cib.com.cn/pers/creditCard/query/queryPastBill.do?FUNID=FIN09|FIN09_08|FIN09_08_02|FIN09_08_02_02&aliveToken=1512372198608&loginTicket=9db3fa45e301260ecd6a1dabc9a0fe11&jumpTicket=aff67b2496e178ed3c97fb8f086090cc";
				WebRequest webRequest1=new WebRequest(new URL(url),HttpMethod.GET);
				HtmlPage pages1 = webClient.getPage(webRequest1);
				url = "https://personalbank.cib.com.cn/pers/creditCard/query/queryPastBill.do?method%3Anext=%E4%B8%8B%E4%B8%80%E6%AD%A5&selIndex="+i+"";
				WebRequest webRequest=new WebRequest(new URL(url),HttpMethod.POST);
				webClient.getOptions().setJavaScriptEnabled(false);
				HtmlPage pages = webClient.getPage(webRequest);
				String html=pages.getWebResponse().getContentAsString();
				Document doc = Jsoup.parse(html);
				Elements eles = doc.select("#queryYearMonth option");
				Elements eles1 = doc.select("#form > div.info-show > span:nth-child(1)");
				String str = eles1.text().trim();
				String cardNumber1 = str.substring(str.indexOf("：")+1);//卡号
				
				if (cardNumber1.contains(cibCreditcardUserInfo.getCardNumber())){
//					CibCreditcardBill cibCreditcardBill = new CibCreditcardBill();
					String urlbill = "https://personalbank.cib.com.cn/pers/creditCard/query/queryBonus.do?FUNID=FIN09|FIN09_08|FIN09_08_03|FIN09_08_03_01&aliveToken=1512380333428";
					WebRequest webRequestbill1=new WebRequest(new URL(urlbill),HttpMethod.GET);
					HtmlPage pagesbill1 = webClient.getPage(webRequestbill1);                 
					urlbill = "https://personalbank.cib.com.cn/pers/creditCard/query/queryBonus.do?method%3Anext=%E4%B8%8B%E4%B8%80%E6%AD%A5&selIndex="+i+"";
					WebRequest webRequestbill=new WebRequest(new URL(urlbill),HttpMethod.POST);
					HtmlPage pagesbill = webClient.getPage(webRequestbill);
					String htmlbill=pagesbill.getWebResponse().getContentAsString();
					//System.out.println(htmlbill);
					Document docbill = Jsoup.parse(htmlbill);
					Elements elesbill = docbill.select("#bonusTbl td");
//					if (elesbill.size()>0){
//						String cardNumber2 = elesbill.get(0).text().trim();
//						if (cardNumber1.contains(cardNumber2)){
//							String integral = elesbill.get(2).text().trim();//积分
//							String integralAdd = elesbill.get(5).text().trim();//新增积分
//							String integralExchange = elesbill.get(6).text().trim();//本期调整积分
//							
//							cibCreditcardBill = parserService.parserCredBill(html,cardNumber1,trimAmount,integral,bankJsonBean.getTaskid());
//							System.out.println(cibCreditcardBill);
//							
//							
//						}
//					}
//					cibCreditcardBillRepository.save(cibCreditcardBill);
//					tracerLog.addTag("账单信息入库成功cibchina_creditcard_bill_info:"+cardNumber1+"",bankJsonBean.getTaskid());
					Elements eles2 = doc.select("#form > table > tbody > tr:nth-child(1) > td:nth-child(4)");
					if(eles2.size()>0){
						String bill = eles2.text().trim();
						System.out.println("bill"+bill);
						String bill_date = bill.substring(bill.lastIndexOf("-")+1)+"号";
						if (bill_date.contains("0")&&!bill_date.contains("10")&&!bill_date.contains("20")&&!bill_date.contains("30")){
							bill_date = bill_date.substring(bill_date.lastIndexOf("0")+1);
						}
						cibCreditcardUserInfo.setBill_date(bill_date);
						
						
						
					}
					String urlE = "https://personalbank.cib.com.cn/pers/creditCard/servSet/modifyCustData.do?FUNID=FIN09|FIN09_05|FIN09_05_05&aliveToken=1513059609086";
					WebRequest webRequestE1=new WebRequest(new URL(urlE),HttpMethod.GET);
					HtmlPage pagesE1 = webClient.getPage(webRequestE1);
					
					urlE = "https://personalbank.cib.com.cn/pers/creditCard/servSet/modifyCustData.do?method%3Anext=%E4%B8%8B%E4%B8%80%E6%AD%A5&selectedIndex="+i+"";
					WebRequest webRequestE=new WebRequest(new URL(urlE),HttpMethod.POST);
					HtmlPage pagesE = webClient.getPage(webRequestE);
					String htmlE=pagesE.getWebResponse().getContentAsString();
					String email = Jsoup.parse(htmlE).select("#email").val();
					String strE = Jsoup.parse(htmlE).select("#form > div.info-show > span:nth-child(1)").text().trim();
//					System.out.println(htmlE);
//					System.out.println("strE"+strE);
					String cardNumber3 = strE.substring(strE.indexOf("：")+1);//卡号
					if (cardNumber3.contains(cibCreditcardUserInfo.getCardNumber())){
						cibCreditcardUserInfo.setEmail(email);
						
					}
					cibCreditcardUserInfoRepository.save(cibCreditcardUserInfo);
					taskBankStatusService.updateTaskBankUserinfo(BankStatusCode.BANK_USERINFO_SUCCESS.getError_code(),
							BankStatusCode.BANK_USERINFO_SUCCESS.getDescription() , bankJsonBean.getTaskid());
					tracerLog.addTag("基本信息入库成功cibchina_creditcard_userinfo:"+cardNumber1+"",bankJsonBean.getTaskid());
					if (eles.size()>0){
						for (int j = 0;j<eles.size();j++){
							String date = eles.get(j).attr("value").trim();
							//System.out.println("date"+date);
//							if (j==0){
//								url = "https://personalbank.cib.com.cn/pers/creditCard/query/queryOngoingBill!jump.do";
//							}else {
								url = "https://personalbank.cib.com.cn/pers/creditCard/query/queryPastBill!query.do";
//							}
							
							WebRequest webRequest2=new WebRequest(new URL(url),HttpMethod.POST);
							webRequest2.setAdditionalHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
							webRequest2.setAdditionalHeader("Accept-Encoding", "gzip, deflate, br");
							webRequest2.setAdditionalHeader("Accept-Language", "zh-CN,zh;q=0.9");
							webRequest2.setAdditionalHeader("Cache-Control", "max-age=0");
							webRequest2.setAdditionalHeader("Connection", "keep-alive");
							webRequest2.setAdditionalHeader("Host", "personalbank.cib.com.cn");
							webRequest2.setAdditionalHeader("Origin", "https://personalbank.cib.com.cn");
							webRequest2.setAdditionalHeader("Referer", "https://personalbank.cib.com.cn/pers/creditCard/query/queryPastBill!jump.do");
							webRequest2.setAdditionalHeader("Upgrade-Insecure-Requests", "1");
							webRequest2.setRequestBody("currCode=01&queryYearMonth="+date+"&credAccountNo="+cibCreditcardUserInfo.getCardNumber()+"");
							HtmlPage page1 = webClient.getPage(webRequest2);
							String html2=page1.getWebResponse().getContentAsString();
//							System.out.println(html2);
							if (elesbill.size()>0){
								String cardNumber2 = elesbill.get(0).text().trim();
								if (cardNumber1.contains(cardNumber2)){
									String integral = elesbill.get(2).text().trim();//积分
//									String integralAdd = elesbill.get(5).text().trim();//新增积分
//									String integralExchange = elesbill.get(6).text().trim();//本期调整积分
									CibCreditcardBill cibCreditcardBill = parserService.parserCredBill(html2,cardNumber1,trimAmount,integral,bankJsonBean.getTaskid());
									if(cibCreditcardBill.getBillDate()!=null){
										cibCreditcardBillRepository.save(cibCreditcardBill);
										tracerLog.addTag("账单信息HTML"+cardNumber3,html2);
										tracerLog.addTag("账单信息入库成功cibchina_creditcard_bill_info"+cardNumber3,bankJsonBean.getTaskid());	
									}
									
								}
							}
							webClient = page1.getWebClient();
							if(j==0) {
								url = "https://personalbank.cib.com.cn/pers/creditCard/query/queryOngoingBill!list.do?_search=false&dataSet.nd=1512442649490&dataSet.rows=1000&dataSet.page=1&dataSet.sidx=&dataSet.sord=asc";
							}else {
								url = "https://personalbank.cib.com.cn/pers/creditCard/query/queryPastBill!list.do?_search=false&dataSet.nd=1512378516752&dataSet.rows=1000&dataSet.page=1&dataSet.sidx=&dataSet.sord=asc";
							}
							
							WebRequest webRequest3=new WebRequest(new URL(url),HttpMethod.GET);
							Page pages2 = webClient.getPage(webRequest3);
							String html1=pages2.getWebResponse().getContentAsString();
							if(html1!=null&&!html1.equals("")){
								System.out.println(html1);
								list.add(html1);
							}
							
						}
					}
				}
				
			}
			return list;
		}
		
		return null;
		
		
		
	}
	//
	public  String getRegular(String html,BankJsonBean bankJsonBean) throws Exception{
		
		Document doc = Jsoup.parse(html);
		Elements eles = doc.select("tr.ui-widget-content td:nth-child(4)");
		int size = 0;
		if(eles!=null&&eles.size()>0){
			for(int i = 0;i<eles.size();i++){
				String type = eles.get(i).text().trim();
				//System.out.println("type:"+type);
				if (type.contains("信用卡")){
					size++;
				}
			}
		}
		if (size==1){
			//基本信息
			WebClient webClient = WebCrawler.getInstance().getNewWebClient();
			Set<org.openqa.selenium.Cookie> cookiesDriver = driver.manage().getCookies();
			for(org.openqa.selenium.Cookie cookie : cookiesDriver){
				Cookie cookieWebClient = new Cookie("personalbank.cib.com.cn", cookie.getName(), cookie.getValue());
				webClient.getCookieManager().addCookie(cookieWebClient);
			}
			String url = "https://personalbank.cib.com.cn/pers/creditCard/query/queryAcctInfo.do?FUNID=FIN09|FIN09_08|FIN09_08_01&aliveToken=1512112371477";
			WebRequest webRequest=new WebRequest(new URL(url),HttpMethod.GET);
			webClient.getOptions().setJavaScriptEnabled(false);
			HtmlPage pages = webClient.getPage(webRequest);
			webClient = pages.getWebClient();
			String html1=pages.getWebResponse().getContentAsString();
//			tracerLog.addTag("兴业银行（信用卡），基本信息html",html1); 
			CibCreditcardUserInfo cibCreditcardUserInfo = parserService.parserCredUser(html1,bankJsonBean.getTaskid());
			Document doc1 = Jsoup.parse(html1);
			Elements eles1 = doc1.select("table:nth-child(4) > tbody > tr > td > div");
			String trimAmount = eles1.get(14).text().trim();//调整金额
			//账单明细
			List<String> list = getBill(bankJsonBean,cibCreditcardUserInfo,trimAmount,size);
			tracerLog.addTag("兴业银行（信用卡），账单明细"+cibCreditcardUserInfo.getCardNumber()+"",list.toString()); 
			for (String html3:list){
				List<CibCreditcardTransFlow> cibCreditcardTransFlow = parserService.parserCredFlow(html3,bankJsonBean.getTaskid(),cibCreditcardUserInfo.getCardNumber());
				cibCreditcardTransFlowRepository.saveAll(cibCreditcardTransFlow);
			}
			taskBankStatusService.updateTaskBankTransflow(BankStatusCode.BANK_TRANSFLOW_SUCCESS.getError_code(),
					BankStatusCode.BANK_TRANSFLOW_SUCCESS.getDescription() , bankJsonBean.getTaskid());
			tracerLog.addTag("兴业银行（信用卡），账单明细入库成功cibchina_creditcard_trans_detail"+cibCreditcardUserInfo.getCardNumber()+"",bankJsonBean.getTaskid()); 
			getData(bankJsonBean,size,0);
			taskBankStatusService.changeTaskBankFinish(bankJsonBean.getTaskid());
		}else if(size>1){
			//基本信息
			for (int i = 0;i <size;i++){
				WebClient webClient = WebCrawler.getInstance().getNewWebClient();
				Set<org.openqa.selenium.Cookie> cookiesDriver = driver.manage().getCookies();
				for(org.openqa.selenium.Cookie cookie : cookiesDriver){
					Cookie cookieWebClient = new Cookie("personalbank.cib.com.cn", cookie.getName(), cookie.getValue());
					webClient.getCookieManager().addCookie(cookieWebClient);
				}
				String url = "https://personalbank.cib.com.cn/pers/creditCard/query/queryAcctInfo.do?FUNID=FIN09|FIN09_08|FIN09_08_01&aliveToken=1512112371477";
				WebRequest webRequest1=new WebRequest(new URL(url),HttpMethod.GET);
				webClient.getOptions().setJavaScriptEnabled(false);
				HtmlPage pages1 = webClient.getPage(webRequest1);
				url = "https://personalbank.cib.com.cn/pers/creditCard/query/queryAcctInfo.do?method%3Anext=%E4%B8%8B%E4%B8%80%E6%AD%A5&selIndex="+i+"";          
				WebRequest webRequest=new WebRequest(new URL(url),HttpMethod.POST);
//				webRequest.setAdditionalHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
//				webRequest.setAdditionalHeader("Accept-Encoding", "gzip, deflate, br");
//				webRequest.setAdditionalHeader("Accept-Language", "zh-CN,zh;q=0.9");
//				webRequest.setAdditionalHeader("Cache-Control", "max-age=0");
//				webRequest.setAdditionalHeader("Connection", "keep-alive");
//				webRequest.setAdditionalHeader("Content-Type", "application/x-www-form-urlencoded");
//				webRequest.setAdditionalHeader("Host", "personalbank.cib.com.cn");
//				webRequest.setAdditionalHeader("Origin", "https://personalbank.cib.com.cn");
//				webRequest.setAdditionalHeader("Referer", "https://personalbank.cib.com.cn/pers/creditCard/query/queryAcctInfo.do?FUNID=FIN09|FIN09_08|FIN09_08_01&aliveToken=1513038956572");
//				webRequest.setAdditionalHeader("Upgrade-Insecure-Requests", "1");
//				webRequest.setRequestBody("");
				//webClient.getOptions().setJavaScriptEnabled(false);
				HtmlPage pages = webClient.getPage(webRequest);
				String html1=pages.getWebResponse().getContentAsString();
				//System.out.println(html1);
				CibCreditcardUserInfo cibCreditcardUserInfo = parserService.parserCredUser(html1,bankJsonBean.getTaskid());
				Document doc1 = Jsoup.parse(html1);
				Elements eles1 = doc1.select("table:nth-child(4) > tbody > tr > td > div");
				System.out.println(eles1.size());
				String trimAmount = eles1.get(14).text().trim();//调整金额
				//账单明细
				List<String> list = getBill(bankJsonBean,cibCreditcardUserInfo,trimAmount,size);
				tracerLog.addTag("兴业银行（信用卡），账单明细"+cibCreditcardUserInfo.getCardNumber()+"",list.toString()); 
				for (String html3:list){
					List<CibCreditcardTransFlow> cibCreditcardTransFlow = parserService.parserCredFlow(html3,bankJsonBean.getTaskid(),cibCreditcardUserInfo.getCardNumber());
					cibCreditcardTransFlowRepository.saveAll(cibCreditcardTransFlow);
				}
				taskBankStatusService.updateTaskBankTransflow(BankStatusCode.BANK_TRANSFLOW_SUCCESS.getError_code(),
						BankStatusCode.BANK_TRANSFLOW_SUCCESS.getDescription() , bankJsonBean.getTaskid());
				tracerLog.addTag("兴业银行（信用卡），账单明细入库成功cibchina_creditcard_trans_detail"+cibCreditcardUserInfo.getCardNumber()+"",bankJsonBean.getTaskid()); 
				getData(bankJsonBean,size,i);
				taskBankStatusService.changeTaskBankFinish(bankJsonBean.getTaskid());
			}
//			Thread.sleep(15000);
		}else {
			taskBankStatusService.changeStatus(BankStatusCode.BANK_LOGIN_LOGINNAME_ERROR.getPhase(), 
					BankStatusCode.BANK_LOGIN_LOGINNAME_ERROR.getPhasestatus(),
					"没有找到信用卡信息，请检查输入的账号信息是否正确", 
					BankStatusCode.BANK_LOGIN_LOGINNAME_ERROR.getError_code(),true, bankJsonBean.getTaskid());
			tracerLog.addTag("没有找到信用卡信息，请检查输入的账号信息是否正确",bankJsonBean.getTaskid());
		}
		
		return null;
	}	
	
	/**
	 * @Des 系统退出，释放资源
	 * @param BankJsonBean 
	 */ 
	public TaskBank quit(BankJsonBean bankJsonBean){ 
		//关闭task (只是 finish = true 、 error_code=-1 、error_message = 系统超时请重试  , description、Phases、PhasesStatus 都不改变，以便查看当时的状态 )
		TaskBank taskBank = taskBankStatusService.systemClose(true,bankJsonBean.getTaskid());  
		//调用公用释放资源方法
		agentService.releaseInstance(taskBank.getCrawlerHost(), driver);
		return taskBank;
	}

	

	@Override
	public TaskBank getAllDataDone(String taskId) {
		// TODO Auto-generated method stub
		return null;
	}

	

	

	
}
