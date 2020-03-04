package com.wizzstudio.githubproxy.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping(path = "/")
public class TestController {
    @GetMapping(path = "/ver")
    public @ResponseBody
    String version() {
        return "0214-0017";
    }
}
