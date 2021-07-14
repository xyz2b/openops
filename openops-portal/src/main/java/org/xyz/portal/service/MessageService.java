package org.xyz.portal.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.xyz.api.contants.RedisPrefix;
import org.xyz.api.entity.SendRequest;
import org.xyz.api.exception.ServiceException;
import org.xyz.portal.service.messagedispatch.MessageDispatchService;

import javax.annotation.Resource;
import java.util.*;

@Slf4j
@Service
public class MessageService {
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Resource(name = "MQDispatchServiceImpl")
    private MessageDispatchService messageDispatchService;

    // 返回不存在的客户端
    public Set<String> execute(SendRequest request) {
        // 检查是否有可用的netty服务端
        checkServer();
        // 查询redis中所有client
        Set<String> set = redisTemplate.keys(RedisPrefix.PREFIX_SERVER_CLIENTS + "*");
        // 检查是否有连接成功的客户端
        checkClient(set);
        //记录查询不存在的客户端
        Set<String> notExist = new HashSet<>();
        //<服务端地址,对应的客户端结果集>
        Map<String, List<String>> hostClientsMap = new HashMap<>(set.size());

        // 根据参数中的客户端标识,找出所在的服务器，向对应的服务器发起推送
        List<String> requestClients = request.getTo();
        for(String client : requestClients) {
            String host = (String) redisTemplate.opsForHash().get(RedisPrefix.PREFIX_CLIENT + client, "host");

            // redis中不存在对应的客户端
            if (!StringUtils.hasText(host)) {
                notExist.add(client);
            }

            // 服务端 和 客户端 的对应关系
            if(hostClientsMap.containsKey(host)){
                hostClientsMap.get(host).add(client);
            }else{
                List<String> clients = new LinkedList<>();
                clients.add(client);
                hostClientsMap.put(host,clients);
            }
        }

        log.info("不存在的客户端[{}]", notExist);

        for(Map.Entry<String,List<String>> entry: hostClientsMap.entrySet()){
            request.setTo(entry.getValue());
            messageDispatchService.send(entry.getKey(), request);
        }

        return notExist;
    }

    private void checkClient(Set<String> set) {
        if (CollectionUtils.isEmpty(set)) {
            throw new ServiceException("没有存在连接的客户端");
        }
    }

    private void checkServer() {
        if (redisTemplate.opsForHash().size(RedisPrefix.NETTY_SERVER) <= 0) {
            throw new ServiceException("没有可用的Netty服务端");
        }
    }
}