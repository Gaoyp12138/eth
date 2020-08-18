package com.example.demo;

import org.web3j.utils.Convert;

import java.math.BigDecimal;

/**
 * @Author: Gaoyp
 * @Description:
 * @Date: Create in  11:05 下午 2020/8/17
 * @Modified By:
 */
public class Test {

    public static void main(String[] args) {
        BigDecimal bigDecimal = Convert.fromWei("570129190810514570011026", Convert.Unit.ETHER);
        System.out.println(bigDecimal);
    }
}
