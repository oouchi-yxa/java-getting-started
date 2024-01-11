package com.heroku.java.mail;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Value;
import lombok.extern.java.Log;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

@Controller
@Log
public class MailSendSample {

    /**
     * 入力画面表示（ mail/input.html ）
     * @return 入力画面
     */
    @GetMapping("/mail/input")
    public String mailInput()
    {
        return "mail/input";
    }

    /**
     * メール送信（autobuhn）
     * @param address メールアドレス
     * @param subject サブジェクト
     * @param body メール本文
     * @param name1 差し込みキー１
     * @param replace1 差し込み文字列１
     * @param name2 差し込みキー２
     * @param replace2 差し込み文字列２
     * @param model 画面表示値設定用
     * @return mail/send.html
     */
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
        // REST送信用便利クラス
        RestTemplate restTemplate = new RestTemplate();
        // 差し込み情報
        Envelope envelope =
                new Envelope(new Address(address), new LinkedHashMap<>());
        if (StringUtils.isNotEmpty(name1)) {
            envelope.getReplace_value().put(name1, StringUtils.defaultString(replace1));
        }
        if (StringUtils.isNotEmpty(name2)) {
            envelope.getReplace_value().put(name2, StringUtils.defaultString(replace2));
        }
        // メール案件番号ダミー（今は、適当に日時で）
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss.SSS");
        // 送信するリクエストの組み立て
        Request request = Request.builder()
                .envelopes(Collections.singletonList(envelope))
                .from(new Address(address))
                .subject(subject)
                .body(new Body(body))
                .custom_args(new CustomArgs(sdf.format(new Date())))
                .build();
        // 環境変数読み込み用クラス
        MailSetting mailSetting = new MailSetting();
        // 送信するリクエストの入れ物を作成
        RequestEntity<Request> requestEntity = RequestEntity
                .post(mailSetting.getUrl())
                .header("Authorization",
                        "Bearer " + mailSetting.getKey())
                .body(request);

        // 送信処理（API呼び出し）
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
