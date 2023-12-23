package com.heroku.java;

import jakarta.servlet.http.HttpServletResponse;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Value;
import lombok.extern.java.Log;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Controller
@Log
public class MailSample {

    @PostMapping("/mail/webhook")
    @ResponseBody
    public WebhookReply mailWebhook(
            @RequestBody String body,
            @RequestHeader Map<String, String> map,
            HttpServletResponse response
    ) {
        for (String key : map.keySet()) {
            log.info(key + " : " + map.get(key));
        }
        log.info("body = " + body);

        WebhookReply reply = new WebhookReply();
        reply.setStatus(HttpStatus.NO_CONTENT);

        // 204をセットする
        response.setStatus(HttpStatus.NO_CONTENT.value());

        return null;
    }

    @Data
    public class WebhookReply {
        private HttpStatus status;
    }

    @GetMapping("/mail/input")
    public String mailInput()
    {
        return "mail/input";
    }

    @PostMapping("/mail/send")
    public String mailSend(
            @RequestParam(name = "address", required = false) String address,
            @RequestParam(name = "subject", required = false) String subject,
            @RequestParam(name = "body", required = false) String body,
            Model model)
    {
        model.addAttribute("message", "終了");

        if (StringUtils.isEmpty(address) || !(address.endsWith("@nec.com") || address.endsWith("@ncontr.com"))) {
            model.addAttribute("message", "メールアドレス未指定か対象外アドレス");
            return "mail/send";
        }
        if (StringUtils.isEmpty(subject)) {
            model.addAttribute("message", "サブジェクト指定なし");
            return "mail/send";
        }
        if (StringUtils.isEmpty(body)) {
            model.addAttribute("message", "本文指定なし");
            return "mail/send";
        }
        //
        RestTemplate restTemplate = new RestTemplate();
        //
        Envelope envelope =
                new Envelope(new Address(address));
        //
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss.SSS");
        //
        Request request = Request.builder()
                .envelopes(Collections.singletonList(envelope))
                .from(new Address(address))
                .subject(subject)
                .body(new Body(body))
                .custom_args(new CustomArgs(sdf.format(new Date())))
                .build();
        //
        MailSetting mailSetting = new MailSetting();
        //
        RequestEntity<Request> requestEntity = RequestEntity
                .post(mailSetting.getUrl())
                .header("Authorization",
                        "Bearer " + mailSetting.getKey())
                .body(request);

        //
        ResponseEntity<RestResponse> response =
                restTemplate.exchange(requestEntity, RestResponse.class);

        log.info("mail response: " + response);

        return "mail/send";
    }

    @Builder
    @Getter
    private static class Request {
        private List<Envelope> envelopes;
        private Address from;
        private String subject;
        private Body body;
        private CustomArgs custom_args;
    }

    @Value
    private static class CustomArgs {
        String mail_id;
    }

    @Value
    private static class Envelope {
        Address to;
    }

    @Value
    private static class Address {
        String address;
    }

    @Value
    private static class Body {
        String text;
    }

    @Data
    private static class RestResponse {
        private String code;
        private String error;
        private String validationErrors;
    }

}
