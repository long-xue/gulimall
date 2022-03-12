package com.atguigu.gulimall.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.atguigu.gulimall.order.vo.PayVo;

import lombok.Data;

@ConfigurationProperties(prefix = "alipay")
@Component
@Data
public class AlipayTemplate {

    //在支付宝创建的应用的id
    private   String app_id = "2016102500758441";

    // 商户私钥，您的PKCS8格式RSA2私钥
    private String merchant_private_key = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCecRKvUfooxCQJfDvP1br24IpN0TtLEyOpJaNdPD2pMJh6YQenSF+Q+B0mPTYIVNJK0KS7EvA9p7QbTpbWPxAXSCjHtyyBt0FKhCh8PSQq85zuio/WhlvZal3vsjRYDKLjeUYZzCIJI1HRA0atECNWSVLCw0WchY+UdFeLPNJeBzT4vN4MT5E3EosVfVmt/X/H+ZobSelx+9aFYDNAS0N1sjS3Xd1TDCf6RFtU7V9rCQIh5rEwNqy3ix6+zoTRppXm9CnVL1X5fK6vvKlwnQ2LlER/Xq2ikTUW1iUDUCI+vAmgcwthurFKkVmSLUuEqmTZEwKGdt6/nmZRBGkRUC47AgMBAAECggEAPj5Jitp3niA/0AIgNSvPwUDA1IVH99xS/BYvMWeDCb5viFQp/4utD3SJbwZI/cjkvENvE5cDDhWd6cRb+VrY5WyRqLPLJkJpwe5dQUF7vevqUMPacfpihu0Ebi/h5F1CeH6iDWxmSpS3hZb14vMeDr3LZ3vzEuRdXYK/CZIhSpKH71b1vwlfjmYTy3zxyca55yNJutinef2VGu54yRsVFO4xmaW28/7AnaYA6sKiAbXsDilpDBwScgVCjNYJS7OZbPKV+WwbOJj2pzyEa2d1fi3SC4s4hnlD+28OyS2jXtz9adhk6xDJ9WIH8Kse8ncBEkyrOoBGS9uLvlwoWgG38QKBgQDOzlk7n4WRe/wrm0dxz63OIrxXO8dHyG6NVCTdUvIoFb5yeu7FlgsWTSSAuoFi10NXVue0F5Kln6kzaGlgqtHifh9jyRSIWG+rz+c7v5AUP0HAF/Zs9LFLxGGAMraczxuoMvc5vJa4z1tps2gTuL5e3jKXmWasJJjbxyoUZ1DrXwKBgQDEIYfgfbOPnM3D3oLaqBNSSmfKPV8C69LHU+sOa0+egb8nSAjepZSpRIjjpN3jrQJIpt4QeHaKpGJXVy9Jez2SSvs0iX0i0PStcIlnzlMvNtoFrE054YrFvPAeD522O5Rn9kkPjrEEF+WudM3aQRMGizI3RNj8DiqiNFduHgPGpQKBgQCGSb/v03osKlJpDn+qC61UuYGLM6oHlsMSypzVitLr7JKqS+FYNEImzRjy7iiuQhHcWPZEHzp+idqZIyeqOcHoTPWdGilXD/YTnwEL7yxjuV+8mNLPlWS7Sl31nPivglz3L4ER4MAOabd2P6VJiSE1ARvhDR96kJkhvYY5kY0eCQKBgEmLhUch+7Yrh2EeTBUwF2axLfBBSSJwSa6jAi57APUgFAznzyOmFXPPlkE+rlHUq1mQ8Q5eDZ15fMLhl4LHatQzt7UApWLr1gQ/gdbiIIFA4UplTOQadg4mBihPlMUj3EeLq+PgjvJI/8CGXjasVjUK9z6QW4lQyp3eYLaTHLldAoGAThn3C111HSH74wGMBCaDpLjwG6/oRAgoy2Ze+FTU2E2gX5FVEelxgSqQEf28u6lIn/cjEMUOZeFoCkrkIJe5bjIN3s0qawNiuMnFLQfBL555KIxIxE/xO/6sq1b8mPBngEVcNb1ZvKaCm6/9pMmCCI/1/MIgd+tAMnLULdqmghk=";
	// 支付宝公钥,查看地址：https://openhome.alipay.com/platform/keyManage.htm 对应APPID下的支付宝公钥。
    private  String alipay_public_key = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAkw7OmxubpJqkqhsdu3yPRVRnDJTjs1Cfl2lox10kv8sKkQwYwtojJQ45RgoSnn1vo/rda/F73pLoANYgE+RpNx8kghatwYDsqQm3S4B84aMR+7GMN9qMm6dXXxoKGn5aY+9NPjkCDO9YeZ4HPn1Yqndj7Y//YfHVSmQQPQKvuWRGGr1bVYUW/IpKBOGr77oSgea0sm42GxPoLypuTiVfq/7AcjmZ3wbTYF2NMwebFXUJmwR5EjdBQa/CnVDKDwpB4doqlP/HpXaepadHYWJZEPokt+nZyiW5l9HrsHU/67A/BSpC/MX/nrtonjnK+j0w/ywvUIIPbzDUpwuTClnNGQIDAQAB";
	// 服务器[异步通知]页面路径  需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    // 支付宝会悄悄的给我们发送一个请求，告诉我们支付成功的信息
    private  String notify_url = "http://内网地址/payed/notify";

    // 页面跳转同步通知页面路径 需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    //同步通知，支付成功，一般跳转到成功页
    private  String return_url = "http://member.gulimall.com/memberOrder.html";

    // 签名方式
    private  String sign_type = "RSA2";

    // 字符编码格式
    private  String charset = "utf-8";

    // 自动关单时间
    private String timeout = "15m";

    // 支付宝网关； https://openapi.alipaydev.com/gateway.do
    private  String gatewayUrl = "https://openapi.alipaydev.com/gateway.do";

    public  String pay(PayVo vo) throws AlipayApiException {

        //AlipayClient alipayClient = new DefaultAlipayClient(AlipayTemplate.gatewayUrl, AlipayTemplate.app_id, AlipayTemplate.merchant_private_key, "json", AlipayTemplate.charset, AlipayTemplate.alipay_public_key, AlipayTemplate.sign_type);
        //1、根据支付宝的配置生成一个支付客户端
        AlipayClient alipayClient = new DefaultAlipayClient(gatewayUrl,
                app_id, merchant_private_key, "json",
                charset, alipay_public_key, sign_type);

        //2、创建一个支付请求 //设置请求参数
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        alipayRequest.setReturnUrl(return_url);
        alipayRequest.setNotifyUrl(notify_url);

        //商户订单号，商户网站订单系统中唯一订单号，必填
        String out_trade_no = vo.getOut_trade_no();
        //付款金额，必填
        String total_amount = vo.getTotal_amount();
        //订单名称，必填
        String subject = vo.getSubject();
        //商品描述，可空
        String body = vo.getBody();

        // 30分钟内不付款就会自动关单
        alipayRequest.setBizContent("{\"out_trade_no\":\""+ out_trade_no +"\","
                + "\"total_amount\":\""+ total_amount +"\","
                + "\"subject\":\""+ subject +"\","
                + "\"body\":\""+ body +"\","
                + "\"timeout_express\":\"" + timeout + "\","
                + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}");

        String result = alipayClient.pageExecute(alipayRequest).getBody();

        //会收到支付宝的响应，响应的是一个页面，只要浏览器显示这个页面，就会自动来到支付宝的收银台页面
        return result;
    }
}
