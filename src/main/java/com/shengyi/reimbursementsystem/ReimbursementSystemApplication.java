package com.shengyi.reimbursementsystem;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.shengyi.reimbursementsystem.mapper")
public class ReimbursementSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReimbursementSystemApplication.class, args);
    }

}
