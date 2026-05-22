package com.shengyi.reimbursementsystem.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/test")
    public String test(){
        return "项目启动成功！SpringBoot 3.2.12";
    }
}