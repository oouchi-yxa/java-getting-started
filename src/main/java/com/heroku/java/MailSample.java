package com.heroku.java;

import com.heroku.java.mail.MailRequest;
import com.heroku.java.mail.MailResponse;
import com.heroku.java.mail.MailSetting;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestTemplate;

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
        MailRequest request = new MailRequest();
        //
        MailSetting mailSetting = new MailSetting();
        //
        RequestEntity<MailRequest> requestEntity = RequestEntity
                .post(mailSetting.getUrl())
                .header("Authorization", "Bearer " + mailSetting.getKey()) // (1)
                .body(request);

        ResponseEntity<MailResponse> response =
                restTemplate.exchange(requestEntity, MailResponse.class);


        return "mail/send";
    }
}
