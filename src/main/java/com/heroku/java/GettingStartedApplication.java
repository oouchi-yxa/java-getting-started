package com.heroku.java;

import com.heroku.java.mail.MailRequest;
import com.heroku.java.mail.MailResponse;
import com.heroku.java.mail.MailSetting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Map;

@SpringBootApplication
@Controller
public class GettingStartedApplication {
    private final DataSource dataSource;

    @Autowired
    public GettingStartedApplication(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/database")
    String database(Map<String, Object> model) {
        try (Connection connection = dataSource.getConnection()) {
            final var statement = connection.createStatement();
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS ticks (tick timestamp)");
            statement.executeUpdate("INSERT INTO ticks VALUES (now())");

            final var resultSet = statement.executeQuery("SELECT tick FROM ticks");
            final var output = new ArrayList<>();
            while (resultSet.next()) {
                output.add("Read from DB: " + resultSet.getTimestamp("tick"));
            }

            model.put("records", output);
            return "database";

        } catch (Throwable t) {
            model.put("message", t.getMessage());
            return "error";
        }
    }

    @GetMapping("/mail/send")
    public String mailSend() {
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

        return "";
    }

    public static void main(String[] args) {
        SpringApplication.run(GettingStartedApplication.class, args);
    }
}
