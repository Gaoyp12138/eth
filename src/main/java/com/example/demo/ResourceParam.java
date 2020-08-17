package com.example.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author anonymity
 * @create 2019-05-29 14:38
 **/
@Component
public class ResourceParam {

    private static String rpcNodeUrl;


    public static String getRpcNodeUrl() {
        String[] urls = rpcNodeUrl.split(",");
        if (urls.length > 1) {
            int index = (int) (System.currentTimeMillis() % urls.length);
            return urls[index];
        }
        return rpcNodeUrl;
    }

    @Value("${rpc.node.address}")
    public void setRpcNodeUrl(String rpcNodeUrl) {
        ResourceParam.rpcNodeUrl = rpcNodeUrl;
    }
}
