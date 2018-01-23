package com.less.aspider.samples;

import com.google.gson.Gson;
import com.less.aspider.ASpider;
import com.less.aspider.bean.Page;
import com.less.aspider.downloader.Downloader;
import com.less.aspider.downloader.HttpConnDownloader;
import com.less.aspider.eventbus.SimpleEventBus;
import com.less.aspider.http.HttpConnUtils;
import com.less.aspider.pipeline.Pipeline;
import com.less.aspider.processor.PageProcessor;
import com.less.aspider.proxy.ProxyProvider;
import com.less.aspider.proxy.SimpleProxyProvider;
import com.less.aspider.samples.bean.JianShuUser;
import com.less.aspider.samples.db.JianshuDao;
import com.less.aspider.scheduler.BDBScheduler;
import com.less.aspider.scheduler.Scheduler;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Administrator
 */
public class JianShuSpider4 {

    private static final String BASE_URL = "https://s0.jianshuapi.com/v2/users/%d";

    private static int startIndex = 1055547;

    private static int endIndex = 2000000;

    private static JianshuDao jianshuDao = new JianshuDao();

    private static Scheduler bdbScheduler = new BDBScheduler();

    public static void main(String args[]) {
        Downloader downloader = new HttpConnDownloader();

        Map<String, String> headers = new HashMap<>();
        headers.put("Host", "s0.jianshuapi.com");
        headers.put("X-App-Name", "haruki");
        headers.put("X-App-Version", "3.2.0");
        headers.put("X-Device-Guid", "127051030369235");
        headers.put("X-Timestamp", "xx");
        headers.put("X-Auth-1", "xx");
        downloader.setHeaders(headers);

        ProxyProvider proxyProvider = new SimpleProxyProvider().longLive();
        downloader.setProxyProvider(proxyProvider);

        SimpleEventBus.getInstance().registerDataSetObserver(proxyProvider);
        SimpleEventBus.getInstance().startWork("F:\\temp.txt", 3600, true);

        timerWork("F:\\jconfig.json", 5, true);

        List<String> urls = new ArrayList<>();
        for (int i = startIndex; i < endIndex; i++) {
            urls.add(String.format(BASE_URL, i));
        }

        ASpider.create()
                .pageProcessor(new PageProcessor() {
                    @Override
                    public void process(Page page) {
                        System.out.println(page.getRawText());
                        Gson gson = new Gson();
                        JianShuUser user = gson.fromJson(page.getRawText(), JianShuUser.class);
                        page.putField("user", user);
                    }
                })
                .thread(100)
                .downloader(downloader)
                .scheduler(bdbScheduler)
                .sleepTime(0)
                .retrySleepTime(0)
                .addPipeline(new Pipeline() {
                    @Override
                    public void process(Map<String, Object> fields) {
                        for (Map.Entry<String, Object> entry : fields.entrySet()) {
                            JianShuUser jianShuUser = (JianShuUser) entry.getValue();
                            jianshuDao.save(jianShuUser);
                        }
                    }
                })
                .urls(urls)
                .run();
    }

    public static class JHeader {
        String timestamp;
        String auth;
    }

    public static void timerWork(final String path, int period, boolean daemon) {
        ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(1,
                new BasicThreadFactory.Builder().namingPattern("example-schedule-pool-%d").daemon(daemon).build());
        executorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                File file = new File(path);
                if (file.exists()) {
                    try {
                        Gson gson = new Gson();
                        JHeader jHeader = gson.fromJson(new FileReader(file), JHeader.class);
                        HttpConnUtils.getDefault().addHeader("X-Auth-1", jHeader.auth);
                        HttpConnUtils.getDefault().addHeader("X-Timestamp", jHeader.timestamp);

                        System.out.println(HttpConnUtils.getDefault().getHeaders());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    System.err.println("headers file not exsist!");
                }
            }
        }, 0, period, TimeUnit.MINUTES);
    }
}