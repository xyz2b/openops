package org.xyz.server.service.channel;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.xyz.api.contants.RedisPrefix;
import org.xyz.api.entity.Client;
import org.xyz.api.util.DateUtils;
import org.xyz.api.util.ObjUtil;
import org.xyz.server.config.Config;
import org.xyz.server.constants.AttrConstants;

import javax.annotation.Resource;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ChannelService {
    @Resource
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private Config config;

    // 客户端ID --> 客户端Channel
    public static final Map<String, Channel> channelList = new ConcurrentHashMap<>(10000);
    // 客户端Channel --> 客户端ID
    public static final Map<Channel, String> reverseChannelList = new ConcurrentHashMap<>(10000);
    // 本项目中客户端ID是以客户端IP来表示的，所以同时只接受同一IP过来的一个连接

    public void put(String clientId, Channel channel) {
        try {
            // redis缓存
            // redis 保存 该 Netty Server 上所有 客户端 的列表
            // 将 当前客户端 信息 加入 Netty Server 客户端列表中
            redisTemplate.opsForSet().add(RedisPrefix.PREFIX_SERVER_CLIENTS + config.getInstanceId(), clientId);
            // redis 保存 每个客户端 对应的 Netty Server 信息
            // 添加当前客户端所关联的Netty Server信息
            redisTemplate.opsForHash().putAll(RedisPrefix.PREFIX_CLIENT + clientId, ObjUtil.ObjToByteMap(new Client(clientId, config.getInstanceId())));

            // 本地缓存
            // 客户端 --> 客户端SocketChannel 关联关系
            channelList.put(clientId, channel);
            // 客户端SocketChannel --> 客户端 关联关系
            reverseChannelList.put(channel, clientId);

            // channel属性标识
            // 给channel标识客户端ID
            channel.attr(AttrConstants.channelId).set(clientId);
            // 给channel绑定客户端sessionId，一次连接保持同样的值
            channel.attr(AttrConstants.sessionId).set(config.getInstanceId() + "_" + clientId + "_" + DateUtils.getCurrentDateTimeFormat());
            // 更新活跃时间
            channel.attr(AttrConstants.activeTime).set(DateUtils.dateToDateTime(new Date()));

        } catch (Exception e) {
            log.error("加入客户端失败:[" + clientId + "]", e);

            // 事务，如果发生异常，上述的操作全部回滚
            rollback(clientId, channel);
        }

    }

    private void rollback(String clientId, Channel channel) {
        if (redisTemplate.opsForSet().isMember(RedisPrefix.PREFIX_SERVER_CLIENTS + config.getInstanceId(), clientId) != null
                &&
                redisTemplate.opsForSet().isMember(RedisPrefix.PREFIX_SERVER_CLIENTS + config.getInstanceId(), clientId)) {
            redisTemplate.opsForSet().remove(RedisPrefix.PREFIX_SERVER_CLIENTS + config.getInstanceId(), clientId);
        }

        if (redisTemplate.hasKey(RedisPrefix.PREFIX_CLIENT + clientId) != null
                &&
                redisTemplate.hasKey(RedisPrefix.PREFIX_CLIENT + clientId)) {
            redisTemplate.delete(RedisPrefix.PREFIX_CLIENT + clientId);
        }

        if (channelList.get(clientId) != null) {
            channelList.remove(clientId);
        }

        if (StringUtils.hasText(reverseChannelList.get(channel))) {
            reverseChannelList.remove(channel);
        }

        channel.attr(AttrConstants.channelId).set(null);
        channel.attr(AttrConstants.sessionId).set(null);
        channel.attr(AttrConstants.activeTime).set(null);

    }

    public Channel get(String clientId) {
        return channelList.get(clientId);
    }

    // 通过channel，反查对应的客户端Id
    public String get(Channel channel) {
        return reverseChannelList.get(channel);
    }

    public Map<String, Channel> getAll() {
        return channelList;
    }

    public void remove(String clientId) {
        if (!StringUtils.hasText(clientId)) {
            return;
        }

        Channel clientChannel = channelList.get(clientId);
        if (clientChannel == null) {
            return;
        }

        try {
            String dateTime = clientChannel.attr(AttrConstants.activeTime).get();
            // 断开当前连接
            clientChannel.close();

            // 删除 该 Netty Server 上维护的客户端信息
            channelList.remove(clientId);
            reverseChannelList.remove(clientChannel);

            // 删除redis中维护的客户端信息
            redisTemplate.delete(RedisPrefix.PREFIX_CLIENT + clientId);

            // 删除redis中维护的该Netty Server上客户端的信息
            redisTemplate.opsForSet().remove(RedisPrefix.PREFIX_SERVER_CLIENTS + config.getInstanceId(), clientId);

            log.info("移除了客户端[{}], 上一次的活跃时间为[{}]",
                    clientChannel,
                    StringUtils.hasText(dateTime) ? dateTime : "");
        }catch (Exception e){
            log.error("移除客户端失败[" + clientId + "]", e);
        }

    }

    // 更新活跃时间
    public void updateActiveTime(Channel channel){
        String now = DateUtils.dateToDateTime(new Date());

        // 更新channel属性
        channel.attr(AttrConstants.activeTime).set(now);
        // 更新redis维护的信息
        redisTemplate.opsForHash().put(RedisPrefix.PREFIX_CLIENT + channel.attr(AttrConstants.channelId).get(),
                "lastActiveTime" , now);
    }

}
