package org.xyz.eureka.listener;

import com.netflix.appinfo.InstanceInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.cloud.netflix.eureka.server.event.*;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.xyz.api.contants.RedisPrefix;
import org.xyz.eureka.config.ComConfig;

import javax.annotation.Resource;

@Component
@Slf4j
public class EurekaStateChangeListener {
    @Autowired
    private ComConfig comConfig;

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    // 服务注册事件
    @EventListener(condition = "#event.replication==false")
    public void listen(EurekaInstanceRegisteredEvent event) {
        InstanceInfo instanceInfo = event.getInstanceInfo();
        log.info("服务注册事件： " + instanceInfo.getAppName() + "-" + instanceInfo.getIPAddr() + "-" + instanceInfo.getPort());
        if (comConfig.getNettyServerApplicationName().equalsIgnoreCase(instanceInfo.getAppName())) {
            if(!redisTemplate.opsForHash().hasKey(RedisPrefix.NETTY_SERVER, instanceInfo.getInstanceId())) {
                redisTemplate.opsForHash().put(RedisPrefix.NETTY_SERVER, instanceInfo.getInstanceId(), "");
            }
        }
    }

    // 服务下线事件
    @EventListener
    public void listen(EurekaInstanceCanceledEvent event) {
        String appName = event.getAppName();
        String instanceId = event.getServerId();
        if (comConfig.getNettyServerApplicationName().equalsIgnoreCase(appName)) {
            redisTemplate.opsForHash().delete(RedisPrefix.NETTY_SERVER, instanceId);
            redisTemplate.delete(RedisPrefix.PREFIX_SERVER_CLIENTS + instanceId);
        }
        log.info("服务下线事件: " + appName.toLowerCase() + ":" + instanceId);
    }

    // Eureka 注册中心可用事件
    @EventListener
    public void listen(EurekaRegistryAvailableEvent event) {
        log.info("Eureka 注册中心可用事件");
    }

    // 服务续约事件
    @EventListener
    public void listen(EurekaInstanceRenewedEvent event) {
        InstanceInfo instanceInfo = event.getInstanceInfo();
        log.info("服务续约事件： " + instanceInfo.getAppName() + "-" + instanceInfo.getIPAddr() + "-" + instanceInfo.getPort());

    }

    // Eureka Server启动事件
    @EventListener
    public void listen(EurekaServerStartedEvent event) {
        log.info("Eureka Server启动事件");
    }
}
