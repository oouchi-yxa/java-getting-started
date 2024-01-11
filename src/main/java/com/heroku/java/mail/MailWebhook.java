package com.heroku.java.mail;

import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.extern.java.Log;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
@Log
public class MailWebhook {

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
            HttpServletResponse response
    ) {
        for (String key : map.keySet()) {
            log.info(key + " : " + map.get(key));
        }
        log.info("data(String) = " + data);

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

}
