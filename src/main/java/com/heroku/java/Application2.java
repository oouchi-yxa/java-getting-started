package com.heroku.java;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class Application2 {

    @GetMapping("/index2")
    public String index() {
        return "index";
    }

}
