package org.xyz.portal.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.xyz.api.entity.SendRequest;
import org.xyz.portal.service.messagedispatch.MQDispatchServiceImpl;

import java.util.ArrayList;
import java.util.List;

@RestController
public class HelloController {
    @Autowired
    private MQDispatchServiceImpl mqDispatchService;

    @GetMapping("/hello")
    public String hello() {
        List<String> to = new ArrayList<>();
        to.add("127.0.0.1");
        mqDispatchService.send("192.168.0.117:9000", new SendRequest(to, "date"));
        return "ok";
    }
}
