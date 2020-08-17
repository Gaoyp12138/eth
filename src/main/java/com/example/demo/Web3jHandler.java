package com.example.demo;

import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Uint;
import org.web3j.crypto.*;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.protocol.http.HttpService;
import org.web3j.protocol.websocket.WebSocketService;
import org.web3j.rlp.RlpEncoder;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpString;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.ConnectException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author ccl
 * @time 2018-05-24 15:19
 * @name Web3jHandler
 * @desc:
 */
public class Web3jHandler {
    private static final Logger LOG = LoggerFactory.getLogger(Web3jHandler.class);
    private static volatile Web3jHandler instance = null;
    private static Web3j web3j = null;
    private static final BigInteger GAS_PRICE = new BigInteger("25000000000");
    private static final BigInteger GAS_LIMIT = new BigInteger("2100000");
    private static final String UNIT_WEI = "1000000000000000000";

    private static final String HEX_PRE = "0x";
    private static final String HEX_0X0 = "0x0";
    private static final String HEX_0X1 = "0x1";
    private static final String HEX_0X2 = "0x2";

    private static final String HTTP_PREFIX = "http://";
    private static final String HTTPS_PREFIX = "https://";
    private static final String WS_PREFIX = "ws://";
    private static final String WSS_PREFIX = "wss://";

    private Web3jHandler(String serverUrl) {
        web3j = Web3j.build(new HttpService(serverUrl));
    }

    public static Web3jHandler getInstance() {
        return getInstance(ResourceParam.getRpcNodeUrl());
    }

    public static Web3jHandler getInstance(String serverUrl) {
        if (null == instance) {
            synchronized (Web3jHandler.class) {
                if (null == instance) {
                    instance = new Web3jHandler(serverUrl);
                }
            }
        }
        return instance;
    }

    public Web3j getWeb3j() {
        return web3j;
    }

    public static Web3j getWeb3j(String url) {
        if (StringUtils.isBlank(url)) {
            url = ResourceParam.getRpcNodeUrl();
        }
        LOG.info(url);
        if (url.startsWith(HTTP_PREFIX) || url.startsWith(HTTPS_PREFIX)) {
            return Web3j.build(new HttpService(url));
        }
        if (url.startsWith(WS_PREFIX) || url.startsWith(WSS_PREFIX)) {
            WebSocketService webSocketService = new WebSocketService(url, true);
            try {
                webSocketService.connect();
                return Web3j.build(webSocketService);
            } catch (ConnectException e) {
                LOG.error("[{}] Websocket connect error: {}", url, e);
            }
        }
        return null;
    }

    public static String createWallet(String path, String password) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException, CipherException, IOException {
        if (null == path || "".equals(path.trim())) {
            path = System.getProperties().getProperty("user.home");
        }
        File filePath = new File(path);
        return WalletUtils.generateNewWalletFile(password, filePath, true);

    }

    public static String createWallet(String path, String name, String password) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException, CipherException, IOException {
        if (null == path || "".equals(path.trim())) {
            path = System.getProperties().getProperty("user.home");
        }
        String fileName = createWallet(path, password);
        File file = new File(path + fileName);
        if (file.renameTo(new File(path + name))) {
            return path + name;
        }
        return fileName;
    }

    public BigInteger getConfirmedBlockNumber(String txHash) throws IOException {
        TransactionReceipt transactionReceipt = web3j.ethGetTransactionReceipt(txHash).send().getResult();
        BigInteger currentBlockNum = transactionReceipt.getBlockNumber();
        return getLatestBlockNumber().subtract(currentBlockNum);
    }

    public BigInteger getBlockNumber() throws IOException {
        return web3j.ethBlockNumber().send().getBlockNumber();
    }

    public BigDecimal getBalance(String address) throws IOException {
        EthGetBalance ethGetBalance = web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST).send();
        BigInteger b = ethGetBalance.getBalance();
        return Convert.fromWei(new BigDecimal(b), Convert.Unit.ETHER).setScale(8, BigDecimal.ROUND_FLOOR);
    }

    public String getERC20Balance(String token, String address) throws IOException {
        Function function = new Function("balanceOf",
                Arrays.asList(new Address(address)),
                Arrays.asList(new TypeReference<Address>() {
                }));

        String encode = FunctionEncoder.encode(function);
        org.web3j.protocol.core.methods.request.Transaction ethCallTransaction =
                org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(address, token, encode);
        return web3j.ethCall(ethCallTransaction, DefaultBlockParameterName.LATEST).send().getResult();
    }

    public String allowance(String token, String owner, String spender) throws IOException {
        Function function = new Function("allowance",
                Arrays.asList(
                        new Address(owner),
                        new Address(spender)),
                Arrays.asList(new TypeReference<Uint>() {
                }));
        String encode = FunctionEncoder.encode(function);
        org.web3j.protocol.core.methods.request.Transaction ethCallTransaction =
                org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(null, token, encode);
        String hex = web3j.ethCall(ethCallTransaction, DefaultBlockParameterName.LATEST).send().getResult();
        return Numeric.toBigInt(hex).toString();
    }

    public EthBlock.Block getLatestBlock() throws IOException {
        return web3j.ethGetBlockByNumber(DefaultBlockParameterName.LATEST, false).send().getBlock();
    }

    public EthBlock.Block getBlock(String hash) throws IOException {
        return web3j.ethGetBlockByHash(hash, false).send().getBlock();
    }

    public EthBlock.Block getBlock(BigInteger number) throws IOException {
        return web3j.ethGetBlockByNumber(DefaultBlockParameter.valueOf(number), false).send().getBlock();
    }

    public TransactionReceipt getTransactionReceipt(String hash) throws IOException {
        return web3j.ethGetTransactionReceipt(hash).send().getResult();
    }

    public TransactionReceipt receipt(String hash) {
        try {
            return web3j.ethGetTransactionReceipt(hash).send().getResult();
        } catch (IOException e) {
            LOG.error("EthGetTransactionReceipt error: {}", e);
        }
        return null;
    }

    public BigInteger getGasUsed(String hash) throws IOException {
        return web3j.ethGetTransactionReceipt(hash).send().getResult().getGasUsed();
    }

    public Transaction getTransaction(String hash) throws IOException {
        return web3j.ethGetTransactionByHash(hash).send().getTransaction().map(transaction -> {
            return transaction;
        }).orElse(null);
    }

    public Transaction getTransactionByHash(String hash) throws IOException {
        return web3j.ethGetTransactionByHash(hash).send().getResult();
    }

    public BigInteger getGasPrice() throws IOException {
        return web3j.ethGasPrice().send().getGasPrice();
    }

    public BigInteger estimateGas(String from, String to, BigInteger nonce, BigInteger gasPrice, BigInteger gasLimit, BigDecimal value) throws Exception {
        return estimateGas(from, to, nonce, gasPrice, gasLimit, value, null);
    }

    public BigInteger estimateGas(String from, String to, BigInteger nonce, BigInteger gasPrice, BigInteger gasLimit, BigDecimal value, String data) throws Exception {
        if (null != data && !"".equals(data.trim())) {
            data = Numeric.toHexString(data.getBytes("UTF-8"));
        }
        org.web3j.protocol.core.methods.request.Transaction transaction = new org.web3j.protocol.core.methods.request.Transaction(from, nonce, gasPrice, gasLimit, to, null != value ? Convert.toWei(value, Convert.Unit.ETHER).toBigInteger() : null, data);
        EthEstimateGas ethEstimateGas = web3j.ethEstimateGas(transaction).send();
        if (ethEstimateGas.hasError()) {
            throw new Exception(ethEstimateGas.getError().getMessage());
        }
        return ethEstimateGas.getAmountUsed();
    }

    public List<EthLog.LogResult> ethEventLogs(String address, Long fromBlock, Long toBlock) throws IOException {
        org.web3j.protocol.core.methods.request.EthFilter ethFilter = new org.web3j.protocol.core.methods.request.EthFilter(
                DefaultBlockParameter.valueOf(BigInteger.valueOf(fromBlock)),
                DefaultBlockParameter.valueOf(BigInteger.valueOf(toBlock)),
                address);
        return web3j.ethGetLogs(ethFilter).send().getResult();
    }

    public static boolean isValidAddress(String address) {
        if (null == address || "".equals(address.trim()) || !address.startsWith(HEX_PRE)) {
            return false;
        }
        return WalletUtils.isValidAddress(address);
    }

    public static BigDecimal toWei(String number) {
        return Convert.toWei(number, Convert.Unit.ETHER);
    }

    public String sendRawTransactions(String hash) throws Exception {
        EthSendTransaction ethSendTransaction = null;
        String transactionHash = null;

        ethSendTransaction = web3j.ethSendRawTransaction(hash).send();
        if (ethSendTransaction.hasError()) {
            LOG.error("Transaction Error: {}", ethSendTransaction.getError().getMessage());
            throw new Exception(ethSendTransaction.getError().getMessage());
        } else {
            transactionHash = ethSendTransaction.getTransactionHash();
            LOG.info("Transactoin Hash: {}", transactionHash);
        }
        return transactionHash;
    }

    public String sendRawTransaction(String hash) {
        EthSendTransaction ethSendTransaction = null;
        String transactionHash = null;
        try {
            ethSendTransaction = web3j.ethSendRawTransaction(hash).send();
            if (ethSendTransaction.hasError()) {
                LOG.error("Transaction Error: {}", ethSendTransaction.getError().getMessage());
            } else {
                transactionHash = ethSendTransaction.getTransactionHash();
                LOG.info("Transactoin Hash: {}", transactionHash);
            }
        } catch (IOException e) {
            LOG.error("Send Raw Transaction Error: {}", e);
        }
        return transactionHash;
    }

    public String sendRawTransactionAsyc(String hash) {
        EthSendTransaction ethSendTransaction = null;
        String transactionHash = null;
        try {
            ethSendTransaction = web3j.ethSendRawTransaction(hash).sendAsync().get(30, TimeUnit.SECONDS);
            if (ethSendTransaction.hasError()) {
                LOG.error("Transaction Error: {}", ethSendTransaction.getError().getMessage());
            } else {
                transactionHash = ethSendTransaction.getTransactionHash();
                LOG.info("Transactoin Hash: {}", transactionHash);
            }
        } catch (InterruptedException e) {
            LOG.error("Send Raw Transaction Error: {}", e);
        } catch (ExecutionException e) {
            LOG.error("Send Raw Transaction Error: {}", e);
        } catch (TimeoutException e) {
            LOG.error("Send Raw Transaction Error: {}", e);
        }
        return transactionHash;
    }

    public BigInteger getNonce(String address) throws IOException {
        return getNoncePending(address);
    }

    public BigInteger getNetPeerCount() throws IOException {
        return web3j.netPeerCount().send().getQuantity();
    }

    public BigInteger getNonceLatest(String address) throws IOException {
        EthGetTransactionCount count = web3j.ethGetTransactionCount(address, DefaultBlockParameterName.LATEST).send();
        return count.getTransactionCount();
    }

    public BigInteger getNoncePending(String address) throws IOException {
        EthGetTransactionCount count = web3j.ethGetTransactionCount(address, DefaultBlockParameterName.PENDING).send();
        return count.getTransactionCount();
    }

    public BigInteger getLatestBlockNumber() throws IOException {
        return getLatestBlock().getNumber();
    }

    public EthBlock.Block getBlockByNumber(BigInteger bigInteger) throws IOException {
        return web3j.ethGetBlockByNumber(DefaultBlockParameter.valueOf(bigInteger), false).send().getBlock();
    }

    public List<EthBlock.Block> getLatestBlock(int total) throws IOException {
        List<EthBlock.Block> result = new ArrayList<>(total);
        long latestNum = getLatestBlockNumber().longValue();
        long start = latestNum - total + 1L;
        for (; start <= latestNum; start++) {
            result.add(getBlock(new BigInteger("" + start)));
        }
        return result;
    }

    public String transfer(String source, String password, String to, BigDecimal value) throws Exception {
        Credentials credentials = WalletUtils.loadCredentials(password, source);
        return transfer(credentials, to, value);
    }

    public String transfer(Credentials credentials, String to, BigDecimal value) throws Exception {
        value = Convert.toWei(value, Convert.Unit.ETHER);
        BigInteger nonce = getNonce(credentials.getAddress());
        RawTransaction rawTransaction = RawTransaction.createEtherTransaction(nonce, Convert.toWei("20", Convert.Unit.GWEI).toBigInteger(), GAS_LIMIT, to, value.toBigInteger());
        byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
        String hexValue = Numeric.toHexString(signedMessage);
        return sendRawTransactions(hexValue);
    }

    public boolean getTransactionStatus(String hash) {
        boolean result = false;
        try {
            EthGetTransactionReceipt receipt = web3j.ethGetTransactionReceipt(hash).send();
            String status = receipt.getTransactionReceipt().get().getStatus();
            LOG.info("get transaction: {}---> status: {}", hash, status);
            result = HEX_0X1.equals(status) ? true : false;
        } catch (IOException e) {
            LOG.error("get transaction status error: {}", e);
        }
        return result;
    }

    public BigInteger validateTransaction(String txhash) throws IOException {
        return getTransactionByHash(txhash).getValue();
    }

    public static String parseAddressFromExtra(EthBlock.Block block) throws SignatureException {
        if (null == block) {
            return null;
        }
        RlpString parentHash = RlpString.create(Hex.decode(block.getParentHash().substring(2)));
        RlpString uncleHash = RlpString.create(Hex.decode(block.getSha3Uncles().substring(2)));
        RlpString miner = RlpString.create(Hex.decode(block.getMiner().substring(2)));
        RlpString stateRoot = RlpString.create(Hex.decode(block.getStateRoot().substring(2)));
        RlpString txRoot = RlpString.create(Hex.decode(block.getTransactionsRoot().substring(2)));
        RlpString receiptRoot = RlpString.create(Hex.decode(block.getReceiptsRoot().substring(2)));
        RlpString logsBoom = RlpString.create(Hex.decode(block.getLogsBloom().substring(2)));
        RlpString diff = RlpString.create(block.getDifficulty());
        if (HEX_0X1.equals(block.getDifficultyRaw())) {
            diff = RlpString.create(1);
        }
        if (HEX_0X2.equals(block.getDifficultyRaw())) {
            diff = RlpString.create(2);
        }
        RlpString number = RlpString.create(block.getNumber());
        RlpString gasLimit = RlpString.create(block.getGasLimit());
        RlpString gasUsed = RlpString.create(block.getGasUsed());
        if (HEX_0X0.equals(block.getGasUsedRaw())) {
            gasUsed = RlpString.create(0);
        }
        int exLen = 65;
        RlpString timestamp = RlpString.create(block.getTimestamp());
        String extraData = block.getExtraData().substring(2);
        byte[] extraDataByteArray = Hex.decode(extraData);
        if (extraDataByteArray.length < exLen) {
            return miner.asString();
        }
        byte[] data = new byte[extraDataByteArray.length - exLen];
        System.arraycopy(extraDataByteArray, 0, data, 0, extraDataByteArray.length - exLen);
        RlpString extraDataString = RlpString.create(data);

        RlpString mixHash = RlpString.create(Hex.decode(block.getMixHash().substring(2)));
        RlpString nonce = RlpString.create(Hex.decode(block.getNonceRaw().substring(2)));
        RlpList list = new RlpList(parentHash, uncleHash, miner, stateRoot, txRoot, receiptRoot, logsBoom, diff, number, gasLimit, gasUsed, timestamp, extraDataString, mixHash, nonce);
        byte[] out = RlpEncoder.encode(list);

        byte[] sig = new byte[exLen];
        byte[] r = new byte[32];
        byte[] s = new byte[32];
        byte v = 0x0;
        if (extraDataByteArray.length <= exLen) {
            return null;
        }

        System.arraycopy(extraDataByteArray, extraDataByteArray.length - 65, sig, 0, exLen);
        for (int i = 0; i < sig.length; i++) {
            if (i <= 31) {
                r[i] = sig[i];
            }

            if (i >= 32 && i < 64) {
                s[i - 32] = sig[i];
            }

            if (i == 64) {
                v = (byte) ((sig[i] & 0xFF) + 27);
            }
        }
        Sign.SignatureData sigMsg = new Sign.SignatureData(v, r, s);
        BigInteger pubKeyRecovered = Sign.signedMessageToKey(out, sigMsg);
        String address = Keys.getAddress(pubKeyRecovered.toString(16));
        if (null != address && !address.startsWith(HEX_PRE)) {
            address = HEX_PRE + address;
        }
        return address;
    }

    public static String signedEthTransactionData(String privateKey, String to, BigInteger nonce, BigDecimal value, String data) throws Exception {
        //把十进制的转换成ETH的Wei, 1ETH = 10^18 Wei
        BigDecimal realValue = Convert.toWei(value, Convert.Unit.ETHER);
        RawTransaction rawTransaction = RawTransaction.createTransaction(nonce, Convert.toWei("22", Convert.Unit.GWEI).toBigInteger(), GAS_LIMIT, to, realValue.toBigIntegerExact(), data);
        //手续费= (gasPrice * gasLimit ) / 10^18 ether
        Credentials credentials = Credentials.create(privateKey);
        //使用TransactionEncoder对RawTransaction进行签名操作
        byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
        //        //转换成0x开头的字符串
        return Numeric.toHexString(signedMessage);
    }

    /**
     * ETH 离线签名 1
     */
    public static String signedOffline(String privateKey, String to, BigInteger nonce, BigDecimal value) throws Exception {
        //把十进制的转换成ETH的Wei, 1ETH = 10^18 Wei
        BigDecimal realValue = Convert.toWei(value, Convert.Unit.ETHER);
        RawTransaction rawTransaction = RawTransaction.createEtherTransaction(nonce, Convert.toWei(GAS_PRICE.toString(), Convert.Unit.GWEI).toBigInteger(), GAS_LIMIT, to, realValue.toBigIntegerExact());
        //手续费= (gasPrice * gasLimit ) / 10^18 ether
        Credentials credentials = Credentials.create(privateKey);
        //使用TransactionEncoder对RawTransaction进行签名操作
        byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
        //转换成0x开头的字符串
        return Numeric.toHexString(signedMessage);
    }

    /**
     * ETH 离线签名 2
     */
    public static String signedEthContractTransactionData(String privateKey, String contractAddress, String to, BigInteger nonce, BigInteger gasPrice, BigInteger gasLimit, Double value, Double decimal) throws Exception {
        //因为每个代币可以规定自己的小数位, 所以实际的转账值=数值 * 10^小数位
        BigDecimal realValue = BigDecimal.valueOf(value * Math.pow(10.0, decimal));
        //0xa9059cbb代表某个代币的转账方法hex(transfer) + 对方的转账地址hex + 转账的值的hex
        String data = "0xa9059cbb" + Numeric.toHexStringNoPrefixZeroPadded(Numeric.toBigInt(to), 64) + Numeric.toHexStringNoPrefixZeroPadded(realValue.toBigInteger(), 64);
        RawTransaction rawTransaction = RawTransaction.createTransaction(nonce, gasPrice, gasLimit, contractAddress, data);
        Credentials credentials = Credentials.create(privateKey);
        //使用TransactionEncoder对RawTransaction进行签名操作
        byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
        //转换成0x开头的字符串
        return Numeric.toHexString(signedMessage);
    }


    public String transaction(String to, Credentials credentials, BigInteger value, String data) throws IOException {
        BigInteger nonce = getNonce(credentials.getAddress());
        RawTransaction rawTransaction = RawTransaction.createTransaction(nonce, GAS_PRICE, GAS_LIMIT, to, value, data);
        byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
        String hexValue = Numeric.toHexString(signedMessage);
        return hexValue;
    }


    public TransactionReceipt getTransactionRecipt(String txHash) throws IOException {
        return web3j.ethGetTransactionReceipt(txHash).send().getTransactionReceipt().orElse(null);
    }

    public Integer getStatus(String txhash) throws IOException {
        TransactionReceipt transactionRecipt = getTransactionRecipt(txhash);
        if (transactionRecipt == null) {
            return null;
        }
        return Integer.parseInt(transactionRecipt.getStatus().substring(2));
    }

}
