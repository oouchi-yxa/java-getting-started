package com.heroku.java;

import jakarta.servlet.http.HttpServletResponse;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Value;
import lombok.extern.java.Log;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.text.SimpleDateFormat;
import java.util.*;

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

    @PostMapping(value="/mail/webhook2", consumes= MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String mailWebhook2(
            @RequestBody String json,
            @RequestBody WebhookReceive data,
            @RequestHeader Map<String, String> map,
            HttpServletResponse response
    ) {
        for (String key : map.keySet()) {
            log.info(key + " : " + map.get(key));
        }
        log.info("data(json) = " + json);
        log.info("data(class) = " + data);

        // 204をセットする
        response.setStatus(HttpStatus.NO_CONTENT.value());

        return null;
    }

    @Data
    private class WebhookReply {
        private HttpStatus status;
    }

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
            @RequestParam(name = "name1", required = false) String name1,
            @RequestParam(name = "replace1", required = false) String replace1,
            @RequestParam(name = "name2", required = false) String name2,
            @RequestParam(name = "replace2", required = false) String replace2,
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
                new Envelope(new Address(address), new LinkedHashMap<>());
        if (StringUtils.isNotEmpty(name1)) {
            envelope.getReplace_value().put(name1, StringUtils.defaultString(replace1));
        }
        if (StringUtils.isNotEmpty(name2)) {
            envelope.getReplace_value().put(name2, StringUtils.defaultString(replace2));
        }
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
        LinkedHashMap<String, String> replace_value;
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
