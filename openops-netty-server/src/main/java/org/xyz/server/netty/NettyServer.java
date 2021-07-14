package org.xyz.server.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioChannelOption;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.xyz.api.contants.RedisPrefix;
import org.xyz.netty.contants.NettyConstant;

import javax.annotation.Resource;

@Slf4j
@Component
public class NettyServer {
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ServerBootstrap serverBootstrap;
    private ChannelFuture channelFuture;

    private ServerInitializer serverInitializer;

    private static final int initPort = 9003;
    private static final int tryMaxCount = 3;
    private static int tryCount = 0;
    private static String nettyPort = "";

    @Value("${hostip}")
    public String hostIP;

    @Value("${eureka.instance.instance-id}")
    public String instanceId;

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    public NettyServer(ServerInitializer serverInitializer) {
        this.serverInitializer = serverInitializer;

        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
        serverBootstrap = new ServerBootstrap();

        serverBootstrap.option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(NioChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.SO_REUSEADDR, true)
                .childOption(ChannelOption.SO_RCVBUF, 32 * 1024)
                .childOption(ChannelOption.SO_SNDBUF, 32 * 1024)
                .childOption(EpollChannelOption.SO_REUSEPORT, true)
                .childOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);

        serverBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(this.serverInitializer);
    }

    public ChannelFuture start() {
        try {
            // 启动netty服务绑定端口
            channelFuture = bind(serverBootstrap, hostIP, initPort);

            // 服务初始化
            init();
        } catch (Exception e) {
            log.error("Netty start error:", e);
        }

        return channelFuture;
    }
    
    private void init() {
        try {
            // 设置redis中记录的Netty Server服务地址的连接端口
            setRedisWebsocketPort();
        } catch (Exception e) {
            log.error("Netty Server 初始化失败，程序退出...", e);
            System.exit(-1);
        }
    }

    private void setRedisWebsocketPort() {
        // 判断 Netty Server 是否成功注册到注册中心上，
        // 如果成功注册，注册中心会把 Netty Server 的 instanceId(springboot的地址和端口以及springboot服务名) 作为 key 写入 redis，这里就能拿到这个 key，然后设置其 value 为 Netty Server 的启动端口
        // 如果没有注册成功，则redis中不会存在 Netty Server instanceId 的 key
        if (redisTemplate.opsForHash().hasKey(RedisPrefix.NETTY_SERVER, instanceId)) {
            redisTemplate.opsForHash().put(RedisPrefix.NETTY_SERVER, instanceId, nettyPort);
            log.info("设置实例[{}]的netty端口为[{}].", instanceId, nettyPort);
        } else {
            log.info("注册中心不存在[{}]的实例, netty初始化失败...", instanceId);
            tryCount++;
            if(tryCount <= tryMaxCount){
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    log.error("Netty Server 启动失败，继续尝试", e);
                }
                setRedisWebsocketPort();
            } else {
                log.error("重试启动 Netty Server [{}] 次，达到最大尝试次数退出，请检查注册中心是否正常", tryCount);
                System.exit(-1);
            }
        }
    }

    private static ChannelFuture bind(final ServerBootstrap serverBootstrap, String bindIp, final int bindPort) {
        return serverBootstrap.bind(bindIp, bindPort).addListener((Future<? super Void> future) -> {
           if (future.isSuccess()) {
               log.info("netty在[" + bindIp + ":" + bindPort + "]启动成功!");
               nettyPort = String.valueOf(bindPort);
           } else {
               log.info("netty在[" + bindIp + ":" + bindPort + "]启动失败,继续尝试启动...");
               bind(serverBootstrap, bindIp, bindPort + 1);
           }
        });
    }

    public void destroy() {
        if(channelFuture.channel() != null) { channelFuture.channel().close();}
        workerGroup.shutdownGracefully();
        bossGroup.shutdownGracefully();
    }
}
