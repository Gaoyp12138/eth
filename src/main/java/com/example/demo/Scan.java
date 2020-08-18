package com.example.demo;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.web3j.utils.Convert;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;

/**
 * @Author: Gaoyp
 * @Description:
 * @Date: Create in  10:29 下午 2020/8/17
 * @Modified By:
 */
@RestController
@Slf4j
public class Scan {

    BigInteger sum = new BigInteger("0");


    @GetMapping("/test/{start}")
    public BigInteger test(@PathVariable String start) {
        List<Data> list = getList(start);
        String startNumber = getStart(list);
        getSum(list);
        if (!startNumber.equals("10681651")) {
            test(startNumber);
        }
        return sum;
    }

    public List<Data> getList(String start) {
        String url = "https://api.etherscan.io/api?module=account&action=txlist&address=0x5acc84a3e955bdd76467d3348077d003f00ffb97&startblock=%s&endblock=10681653&sort=asc&apikey=B6HZQ7IUEG4QUAS6UX5AD99V8BISJRDJYN";
        String format = String.format(url, start);
        log.info("请求地址：" + format);
        String result = OkHttpClientHelper.get(format, new HashMap<>(), new HashMap<>());
        JSONObject jsonObject = JSONObject.parseObject(result);
        String status = (String) jsonObject.get("status");
        if (null != status && status.equals("1")) {
            JSONArray result1 = jsonObject.getJSONArray("result");
            List<Data> dataList = JSON.parseArray(JSON.toJSONString(result1), Data.class);
            return dataList;
        }
        return null;
    }

    public String getStart(List<Data> dataList) {
        if (null != dataList) {
            int size = dataList.size();
            Data lastData = dataList.get(size - 1);
            String blockNumber = lastData.getBlockNumber();
            log.info("起始块：" + blockNumber);
            return blockNumber;
        }
        return null;
    }

    public void getSum(List<Data> dataList) {
        for (Data data : dataList) {
            if (data.getTo().toLowerCase().equals("0x5acc84a3e955bdd76467d3348077d003f00ffb97") && data.getTxreceipt_status().equals("1")) {
                log.info("本次投入： " + Convert.fromWei(data.getValue(), Convert.Unit.ETHER));
                sum = sum.add(new BigInteger(data.getValue()));
            }
        }
        log.info("算了一次------------------------： " + Convert.fromWei(sum.toString(), Convert.Unit.ETHER));
    }

}
