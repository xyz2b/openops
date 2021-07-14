package org.xyz.server.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.xyz.api.contants.CommonConstants;
import org.xyz.api.entity.Response;
import org.xyz.api.entity.SendRequest;
import org.xyz.server.service.MessageHttpService;

@RestController
@Slf4j
public class MessageSendController {
    @Autowired
    private MessageHttpService messageHttpService;

    @RequestMapping("/message/send")
    public Response sendToAllClient(@RequestBody SendRequest request){
        messageHttpService.send(request);
        return new Response(CommonConstants.SUCCESS,CommonConstants.REQUST_SUC);
    }
}
