package de.baumann.browser.Bamboo;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class BambooEngine {
    public static final String TAG = "BambooEngine";

    public static final String BAMBU_PREFIX = "bambu://"; //이미지 태그 비활성화 prefix

    private OkHttpClient client;

    private String baseURL;
    private @Nullable
    OnLoadFinishedListener onLoadFinishedListener;

    private Response htmlResponse = null;

    private String orgHTML = null; // 원본 HTML
    private String relToAbsHTML = null; // orgHTML 에서 상대경로를 절대경로로 처리한 HTML
    private String inactiveImgHTML = null; // relToAbsHTML 에서 이미지 태그를 비활성화 처리한 HTML
    private String activeHTML = null; // 최종 HTML

    private HashMap<String, String> imgSrcMap; //이미지 경로 맵

    private ExecutorService executorService = Executors.newFixedThreadPool(1); //쓰레드풀
    private boolean isRunningHTMLDownloadThread = false;

    public BambooEngine() {
        imgSrcMap = new HashMap<>();
        init();
    }

    public void setOnLoadFinishedListener(OnLoadFinishedListener listener) {
        this.onLoadFinishedListener = listener;

    }

    public void destroy(){
        if(executorService != null){
            executorService.shutdownNow();
        }
    }

    private void init() {
        this.client = new OkHttpClient.Builder()
                .followRedirects(true)  //리다이렉팅 처리
                .followSslRedirects(true)
                .addNetworkInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        baseURL = chain.request().url().toString();
                        return chain.proceed(chain.request());
                    }
                }).build();
    }

    public void run(String baseURL) {
        if (!baseURL.endsWith("/")) baseURL = baseURL + "/";
        this.baseURL = baseURL;

        Request request = new Request.Builder()
                .url(baseURL)
                .build();

        if (isRunningHTMLDownloadThread) {
            Log.d(TAG, "로딩중 취소");
            executorService.shutdownNow();
            executorService = Executors.newFixedThreadPool(1);
        } else {
            Log.d(TAG, "대기열이 없음");
            isRunningHTMLDownloadThread = true;
        }
        executorService.submit(new HTMLDownloadRunnable(request));
    }

    //비활성화 이미지를 활성화
    public void loadImg(String bambuUrl) {
        if (!bambuUrl.contains(BAMBU_PREFIX)) return;
        String id = bambuUrl.replace(BAMBU_PREFIX, "");
        Document doc = Jsoup.parse(activeHTML);

        Elements elements = doc.select("img[src=" + bambuUrl + "]");
        if (elements.size() == 0) return;
        Element element = elements.first();

        element.attr("src", imgSrcMap.get(id));

        activeHTML = doc.outerHtml();
        if (onLoadFinishedListener != null) {
            onLoadFinishedListener.onReloadFinish(activeHTML);
        }
    }

    private String inactiveAllImgTag(String html) {
        Document doc = Jsoup.parse(html);
        Elements imgElements = doc.select("img[src]");

        int id = 0;
        for (Element ele : imgElements) {
            String orgLink = ele.attr("src");
            String activeLink = null;
            if (orgLink.startsWith("http")) {
                //do nothing
                activeLink = orgLink;
            } else if (orgLink.startsWith("//")) {
                //상대경로 -> BaseURL + url
                activeLink = "http://" + orgLink.substring(2);// "//"를 제거함
            } else if (orgLink.startsWith("/")) {
                activeLink = baseURL + orgLink.substring(1);// "/"를 제거함
            } else { //슬래쉬로 시작하지 않는 경우
                activeLink = baseURL + orgLink;
            }


            ele.attr("src", BAMBU_PREFIX + id); //비활성화

            imgSrcMap.put(id + "", activeLink);//[식별자,절대경로]
            id++;
        }

        return doc.outerHtml();
    }

    private String convertRelToAbsPath(String html) {
        Document doc = Jsoup.parse(html);
        Elements hrefElements = doc.select("[href]");
        Elements srcElements = doc.select("[src]");

        iterateConvertPath(hrefElements, "href");
        iterateConvertPath(srcElements, "src");

        return doc.outerHtml();
    }

    private void iterateConvertPath(Elements elements, String attrName) {
        for (Element ele : elements) {
            if (ele.tagName().equals("img")) continue; //img태그 패스
            String attr = ele.attr(attrName);
            if (attr.startsWith("http")) {
                //do nothing
            } else if (attr.startsWith("//")) {
                //상대경로 -> BaseURL + url
                ele.attr(attrName, "http://" + attr.substring(2));// "//"를 제거함
            } else if (attr.startsWith("/")) {
                ele.attr(attrName, baseURL + attr.substring(1));// "/"를 제거함
            }
        }
    }

    private class HTMLDownloadRunnable implements Runnable {
        private Request request;

        public HTMLDownloadRunnable(Request request) {
            this.request = request;
        }

        @Override
        public void run() {
            try {
                htmlResponse = client.newCall(request).execute();
                orgHTML = htmlResponse.body().string();
                relToAbsHTML = convertRelToAbsPath(orgHTML);
                inactiveImgHTML = inactiveAllImgTag(relToAbsHTML);
                activeHTML = inactiveImgHTML;

                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        onLoadFinishedListener.onLoadFinish(inactiveImgHTML);
                    }
                });
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            isRunningHTMLDownloadThread = false;
        }
    }
}
