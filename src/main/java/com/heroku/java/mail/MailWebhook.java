package com.heroku.java.mail;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@Log
public class MailWebhook {

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Webhookテスト
     * @param data 受け取ったJSONから WebhookReceive に変換した値
     * @param map ヘッダー情報
     * @param response 応答オブジェクト
     * @return 使用しない
     */
    @PostMapping(value="/mail/webhook", consumes= MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String mailWebhook(
            @RequestBody String data,
            @RequestHeader Map<String, String> map,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        // 環境変数読み込み用クラス
        MailSetting mailSetting = new MailSetting();

        for (String key : map.keySet()) {
            log.info(key + " : " + map.get(key));
        }
        log.info("data(String) = " + data);

        try {
            WebhookReceive dataClass
                    = objectMapper.readValue(data, WebhookReceive.class);
            log.info("data(class) = " + dataClass);

            String headerSignature = map.get("x-autobahn-webhook-signature");
            String bodySignature = sha256hash(mailSetting.getWebhookKey())
                    + "." + hMacSha1(data, mailSetting.getWebhookKey());
            log.info("bodySignature : " + bodySignature);
            if (headerSignature != null && headerSignature.equals(bodySignature)) {
                log.info("signature check OK !!!!");
            } else {
                log.info("signature check NG !!!!");
            }

        } catch (JsonProcessingException e) {
            log.info("mapping miss: " + e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            log.info("no such algorithm miss: " + e.getMessage());
        } catch (InvalidKeyException e) {
            log.info("invalid key miss: " + e.getMessage());
        }

        log.info("remote ip or proxy ? " + request.getRemoteAddr());

        // 204をセットする
        response.setStatus(HttpStatus.NO_CONTENT.value());

        return null;
    }

    /**
     * Webhook連携された内容
     */
    @Data
    private static class WebhookReceive {
        String  mta_mail_id;
        String  email;
        String  from;
        Long  timestamp;
        LinkedHashMap<String, String>  custom_args;
        String  event;
        String  header_from;
        String  batch_id;
    }

    private static String hMacSha1(String str, String key)
            throws NoSuchAlgorithmException, InvalidKeyException {
        final String algorithm = "HmacSHA1";
        SecretKeySpec sk = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), algorithm);
        Mac mac = Mac.getInstance(algorithm);
        mac.init(sk);
        byte[] result = mac.doFinal(str.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(result);
    }

    private static String sha256hash(String str) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(str.getBytes(StandardCharsets.UTF_8));
        HexFormat hex = HexFormat.of().withLowerCase();
        return hex.formatHex(digest);
    }
}
