import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageReader;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.ThreadedRefreshHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.html.HtmlImage;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.util.Cookie;
import com.gargoylesoftware.htmlunit.util.NameValuePair;

import app.crawler.htmlparse.HtmlParse;

/**
 * 
 * 项目名称：common-microservice-executor 类名称：test 类描述： 创建人：hyx 创建时间：2018年7月12日
 * 上午10:32:07
 * 
 * @version
 */
public class testzhixing {
	public static final int DEFAULT_PAGE_TIME_OUT = 10000; // ms
	public static final int DEFAULT_JS_TIME_OUT = 10000;

	// String url = "http://zxgk.court.gov.cn/shixin/disDetail?"
	// + "id=703892740"
	// + "&pCode=dqmx"
	// + "&captchaId=52b5c55d37ba4ca7aeec6abb804f432c";
	// http://zxgk.court.gov.cn/shixin/captcha.do?captchaId=52b5c55d37ba4ca7aeec6abb804f432c&random=0.11988255431696948
	public static void main(String[] args) {


		HtmlPage htmlPage = null;
		
		try {
			// HtmlPage htmlPage = (HtmlPage) getHtmlByUrlForGET(url);

			String url = "http://zhixing.court.gov.cn/search/index_form.do";
			htmlPage = (HtmlPage) getHtmlByUrlForGET(url);

			HtmlImage image = (HtmlImage) htmlPage.getElementById("captchaImg");
			// System.out.println(htmlPage);

			String captchaId = HtmlParse.getcaptchaId(htmlPage.getWebResponse().getContentAsString());

			ImageReader imageReader = image.getImageReader();
			BufferedImage bufferedImage = imageReader.read(0);

			JFrame f2 = new JFrame();
			JLabel l = new JLabel();
			l.setIcon(new ImageIcon(bufferedImage));
			f2.getContentPane().add(l);
			f2.setSize(500, 100);
			f2.setTitle("验证码");
			f2.setVisible(true);

			String valicodeStr = JOptionPane.showInputDialog("请输入验证码：");

			url = "http://zhixing.court.gov.cn/search/newsearch?"
					+ "searchCourtName=%E5%85%A8%E5%9B%BD%E6%B3%95%E9%99%A2%EF%BC%88%E5%8C%85%E5%90%AB%E5%9C%B0%E6%96%B9%E5%90%84%E7%BA%A7%E6%B3%95%E9%99%A2%EF%BC%89"
					+ "&selectCourtId=1"
					+ "&selectCourtArrange=1"
					+ "&pname=%E7%8E%8B%E5%87%AF"
					+ "&cardNum="
					+ "&j_captcha="+valicodeStr.trim()
					+ "&captchaId="+captchaId.trim();

		/*	paramsList.add(new NameValuePair("pName", "王凯"));
			paramsList.add(new NameValuePair("pCardNum", ""));
			paramsList.add(new NameValuePair("pProvince", "0"));
			paramsList.add(new NameValuePair("pCode", valicodeStr));
			paramsList.add(new NameValuePair("captchaId", captchaId.trim()));*/

	
//			Page page = getByHtmlUnitPost(webClient, url, paramsList);
			Page page = (HtmlPage) getHtmlByUrlForGET(url);
			System.out.println("==" + page.getWebResponse().getContentAsString());
			
			System.out.println(url);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static Page getByHtmlUnitPost(WebClient webClient, String url, List<NameValuePair> paramsList)
			throws Exception {

		Set<Cookie> cookies = webClient.getCookieManager().getCookies();

		for (Cookie cookie : cookies) {
			System.out.println("bbbbb"+cookie.getName() + ":" + cookie.getValue());
		}

		webClient.setRefreshHandler(new ThreadedRefreshHandler());
		webClient.getOptions().setCssEnabled(false);
		webClient.getOptions().setJavaScriptEnabled(true);
		webClient.getOptions().setThrowExceptionOnScriptError(false);
		webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
		webClient.getOptions().setPrintContentOnFailingStatusCode(false);
		webClient.getOptions().setRedirectEnabled(false);
		webClient.getOptions().setTimeout(DEFAULT_PAGE_TIME_OUT); // 15->60
		webClient.waitForBackgroundJavaScript(DEFAULT_JS_TIME_OUT); // 5s
		webClient.setAjaxController(new NicelyResynchronizingAjaxController());

		webClient.addRequestHeader("Host", "zxgk.court.gov.cn");
		webClient.addRequestHeader("Origin", "http://zxgk.court.gov.cn");
		webClient.addRequestHeader("Referer", "http://zxgk.court.gov.cn/shixin/index_form.do");

		webClient.addRequestHeader("Upgrade-Insecure-Requests", "1");

		webClient.addRequestHeader("Accept",
				"text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
		webClient.addRequestHeader("Accept-Encoding", "gzip, deflate");
		webClient.addRequestHeader("Accept-Language", "zh-CN,zh;q=0.9");
		webClient.addRequestHeader("Cache-Control", "max-age=0");
		webClient.addRequestHeader("Connection", "keep-alive");
		webClient.addRequestHeader("Accept-Language", "zh-CN,zh;q=0.9");
		webClient.addRequestHeader("Content-Type", "application/x-www-form-urlencoded");

		WebRequest webRequest = new WebRequest(new URL(url), HttpMethod.GET);
		if (paramsList != null) {
			webRequest.setRequestParameters(paramsList);
		}
		webClient.setJavaScriptTimeout(50000);
		webClient.getOptions().setTimeout(50000); // 15->60
		Page page = webClient.getPage(webRequest);
		return page;
	}

	public static Page getHtmlByUrlForGET(String url) throws Exception {

		WebClient webClient = new WebClient(BrowserVersion.CHROME);
		webClient.setRefreshHandler(new ThreadedRefreshHandler());
		webClient.getOptions().setCssEnabled(false);
		webClient.getOptions().setJavaScriptEnabled(true);
		webClient.getOptions().setThrowExceptionOnScriptError(false);
		webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
		webClient.getOptions().setPrintContentOnFailingStatusCode(false);
		webClient.getOptions().setRedirectEnabled(false);
		webClient.getOptions().setTimeout(DEFAULT_PAGE_TIME_OUT); // 15->60
		webClient.waitForBackgroundJavaScript(DEFAULT_JS_TIME_OUT); // 5s
		webClient.setAjaxController(new NicelyResynchronizingAjaxController());

		WebRequest webRequest = new WebRequest(new URL(url), HttpMethod.GET);
		// if (searchTask.getIpaddress() != null && searchTask.getIpport() !=
		// null) {
		// webRequest.setProxyHost(searchTask.getIpaddress());
		// webRequest.setProxyPort(Integer.parseInt(searchTask.getIpport()));
		// }

		Page page = webClient.getPage(webRequest);

		return page;
	}

	public static Page getHtmlByUrlForGET2(String url, WebClient webClient) throws Exception {

		WebRequest webRequest = new WebRequest(new URL(url), HttpMethod.GET);
		// if (searchTask.getIpaddress() != null && searchTask.getIpport() !=
		// null) {
		// webRequest.setProxyHost(searchTask.getIpaddress());
		// webRequest.setProxyPort(Integer.parseInt(searchTask.getIpport()));
		// }

		Page page = webClient.getPage(webRequest);

		return page;
	}
}
