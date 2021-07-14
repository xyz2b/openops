package org.xyz.server.controller;

import io.netty.channel.Channel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.xyz.server.service.channel.ChannelService;

import java.util.Map;

@RestController
public class ChannelController {
    @Autowired
    private ChannelService channelService;

    @RequestMapping(value="channels")
    public Map<String, Channel> getChannels(){
        return channelService.getAll();
    }
}
