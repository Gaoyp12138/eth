package com.example.demo;


import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author ccl
 * @time 2018-04-10 17:03
 * @name OkHttpClientHelper
 * @desc:
 */
@Slf4j
public class OkHttpClientHelper {

    private static final OkHttpClient okHttpClient = new OkHttpClient.Builder().connectTimeout(200L, TimeUnit.SECONDS).readTimeout(200L, TimeUnit.SECONDS).writeTimeout(200L, TimeUnit.SECONDS).build();
    private static final OkHttpClient proxyOkHttpClient = new OkHttpClient.Builder().proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 10809))).build();

    public static String get(String url, Map<String, String> header, Map<String, Object> params){
        Request.Builder builder = new Request.Builder();
        if(null != header){
            builder.headers(Headers.of(header));
        }
        if(null != params && params.size() > 0){
            StringBuffer urlParams = new StringBuffer();
            params.entrySet().stream().forEach(entry -> urlParams.append("&" + entry.getKey() + "=" + entry.getValue()));
            if(!url.contains("?")){
                url += "?" + urlParams.substring(1);
            }else{
                url += urlParams.toString();
            }
        }
        return execute(builder.url(url).get().build());
    }


    public static String post(String url, Map<String, String> header, Map<String, Object> params){
        Request.Builder builder = new Request.Builder();
        if(null != header){
            builder.headers(Headers.of(header));
        }
        RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), JSON.toJSONString(params));
        return execute(builder.url(url).post(body).build());
    }

    public static String put(String url, Map<String, String> header, Map<String, Object> params){
        Request.Builder builder = new Request.Builder();
        if(null != header){
            builder.headers(Headers.of(header));
        }
        RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), JSON.toJSONString(params));
        return execute(builder.url(url).put(body).build());
    }

    public static String put(String url, Map<String, String> header, List list){
        Request.Builder builder = new Request.Builder();
        if(null != header){
            builder.headers(Headers.of(header));
        }
        RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), JSON.toJSONString(list));
        return execute(builder.url(url).put(body).build());
    }

    public static String delete(String url, Map<String, String> header, Map<String, Object> params){
        Request.Builder builder = new Request.Builder();
        if(null != header){
            builder.headers(Headers.of(header));
        }
        RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), JSON.toJSONString(params));
        return execute(builder.url(url).delete(body).build());
    }
    private static String execute(Request request) {
        String result = null;
        Response response = null;
        try {
            response = okHttpClient.newCall(request).execute();
            if(response.isSuccessful()){
                result = response.body().string();
                response.body().close();
            }
        } catch (IOException e) {
            log.error("OkHttpClientHelper execute error:{} url:{} method:{}",e.getMessage(),request.url(),request.method(),e);
        } finally {
            if(null != response && null != response.body()){
                response.body().close();
            }
        }
        return result;
    }

}
