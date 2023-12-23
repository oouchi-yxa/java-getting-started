package com.heroku.java;

import com.heroku.java.mail.MailRequest;
import com.heroku.java.mail.MailResponse;
import com.heroku.java.mail.MailSetting;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Value;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

@Controller
public class MailSample {

    @GetMapping("/mail/input")
    public String mailInput()
    {
        return "mail/input";
    }

    @GetMapping("/mail/send")
    public String mailSend()
    {
        //
        RestTemplate restTemplate = new RestTemplate();
        //
        Envelope envelope =
                new Envelope(new Address("oouchi-yxa@nec.com"));
        //
        Request request = Request.builder()
                .envelopes(Collections.singletonList(envelope))
                .from(new Address("oouchi-yxa@nec.com"))
                .subject("subject1")
                .body(new Body("body1"))
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

        return "mail/send";
    }



    @Builder
    @Getter
    private class Request {
        private List<Envelope> envelopes;
        private Address from;
        private String subject;
        private Body body;
    }

    @Value
    private class Envelope {
        Address to;
    }

    @Value
    private class Address {
        String address;
    }

    @Value
    private class Body {
        String text;
    }

    @Data
    private class RestResponse {
        private String code;
        private String error;
        private String validationErrors;
    }

}
