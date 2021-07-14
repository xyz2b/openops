package org.xyz.portal.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import org.xyz.api.contants.CommonConstants;
import org.xyz.api.entity.Response;
import org.xyz.api.entity.SendRequest;
import org.xyz.portal.service.MessageService;

import javax.validation.Valid;
import java.util.Set;

@Slf4j
@RestController
public class MessageController {

    @Autowired
    private MessageService messageService;

    @RequestMapping(value="/message/send",method = RequestMethod.POST)
    public Response send(@RequestBody SendRequest request){
        log.info("requestID: " + request.getRequestId());
        Response result = null;
        Set<String> notExist = messageService.execute(request);
        if(!CollectionUtils.isEmpty(notExist)){
            result = new Response("存在找不到的客户端: ", notExist.toString());
        }else{
            result = new Response(CommonConstants.SUCCESS, "success", request.getRequestId());
        }
        return result;
    }
}
