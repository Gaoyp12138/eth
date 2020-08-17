package com.example.demo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

/**
 * @Author: Gaoyp
 * @Description: 监听区块存进数据库，每5s扫一次，查询的时候先从数据库查询，如果查询不到可能是未同步，，再去链上查询一遍
 * @Date: Create in  14:03 2019-11-25
 * @Modified By:
 */
@Component
@Slf4j
public class EthScheduled {


    @Scheduled(cron = "0/5 * * * * ?")
    public void getLatestBlockInfo() {
        try {
            log.info("开始了");
            BigInteger latestBlockNumber = Web3jHandler.getInstance().getLatestBlockNumber();
            BigInteger startBlockNum = new BigInteger("9391396");

            for (BigInteger i = startBlockNum; -1 == i.compareTo(latestBlockNumber); i = i.add(new BigInteger("1"))) {
                EthBlock.Block block = Web3jHandler.getInstance().getBlock(i);
                saveTransaction(block);
            }
        } catch (Exception e) {
            log.error("扫描区块异常:{}", e);
        }

    }


    BigInteger totalV = new BigInteger("0");

    private void saveTransaction(EthBlock.Block block) {
        List<EthBlock.TransactionResult> list = block.getTransactions();
        if (null != list && list.size() > 0) {
            for (EthBlock.TransactionResult result : list) {
                String txHash = null;
                try {
                    txHash = (String) result.get();
                } catch (Exception e) {
                    log.error("Get Transaction by Hash Error: ", e);
                }
                if (null != txHash) {
                    try {
                        Web3j web3j = Web3j.build(new HttpService(ResourceParam.getRpcNodeUrl()));
                        Transaction transaction = web3j.ethGetTransactionByHash(txHash).send().getResult();
                        TransactionReceipt tr = web3j.ethGetTransactionReceipt(txHash).send().getResult();
                        if (null != tr.getTo() && tr.getTo().equals("0x5acc84a3e955bdd76467d3348077d003f00ffb97")) {
                            BigInteger value = transaction.getValue();
                            totalV = value.add(totalV);
                            log.info("算了一次： " + totalV);
                        }
                    } catch (IOException e) {
                        log.error("Get Gas Used Error", e);
                        return;
                    }
                }
            }
        }
        log.info("----------" + totalV);
    }
}
