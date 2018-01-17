package com.less.aspider.samples;

import com.less.aspider.ASpider;
import com.less.aspider.bean.Page;
import com.less.aspider.downloader.Downloader;
import com.less.aspider.downloader.HttpConnDownloader;
import com.less.aspider.pipeline.OneFilePipeline;
import com.less.aspider.processor.PageProcessor;
import com.less.aspider.proxy.ProxyProvider;
import com.less.aspider.proxy.SimpleProxyProvider;
import com.less.aspider.scheduler.BDBScheduler;
import com.less.aspider.util.RegexUtils;

import java.io.File;
import java.util.List;

/**
 * @author Administrator
 */
public class JianShuSpider {
    public static final String BASE_URL = "https://www.jianshu.com";
    public static void main(String args[]) {
        final String userRegex = "https://www\\.jianshu\\.com/u/[a-zA-Z0-9]*";
        final String followRegex = "/users/.*/following";
        final String chooseRegex = "<a class=\"avatar\" href=\"/u/(.*)\">";
        final String fansRegex = "<p>(\\d+)</p>[\\s|\n]*粉丝";

         // Proxy 代理设置
        String path = System.getProperty("user.dir") + File.separator + "ASpidder" + File.separator + "src/main/java/proxy.txt";
        ProxyProvider proxyProvider = SimpleProxyProvider.from(path);
        Downloader downloader = new HttpConnDownloader();
        downloader.setProxyProvider(proxyProvider);
        ASpider.create()
                .pageProcessor(new PageProcessor() {
                    @Override
                    public void process(Page page) {
                        String url = page.getUrl();
                        boolean flag = RegexUtils.get(userRegex).matchers(url);
                        if (flag) {
                            // 个人主页
                            // 选择该【个人主页】的关注列表页面
                            String followUrl = RegexUtils.get(followRegex).selectSingle(page.getRawText(),0);
                            followUrl = BASE_URL + followUrl;
                            page.addTargetRequestsNoReferer(followUrl);
                            String fansCount = RegexUtils.get(fansRegex).selectSingle(page.getRawText(),1);
                            int fans = Integer.parseInt(fansCount);
                            if (fans > -1) {
                                page.putField("user",url);
                                page.putField("fansCount",fansCount);
                            }
                        } else {
                            // 关注列表页面
                            // 选择该【列表页面】的用户个人主页
                            List<String> list = RegexUtils.get(chooseRegex).selectList(page.getRawText(),1);
                            for (String userUrl : list) {
                                userUrl = BASE_URL + "/u/" + userUrl;
                                page.addTargetRequestsNoReferer(userUrl);
                            }
                        }
                    }
                })
                .thread(10)
                .downloader(downloader)
                // 注意BDB由于是持久化的,所以如果第二次执行起始url已存在会直接跳过程序结束.
                .scheduler(new BDBScheduler())
                .addPipeline(new OneFilePipeline("F:/JianShuURL.txt"))
                .urls("https://www.jianshu.com/u/2900feeb8251")
                .run();
    }
}