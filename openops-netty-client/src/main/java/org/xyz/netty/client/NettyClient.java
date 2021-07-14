package org.xyz.netty.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/*
* 断连重连:
*   当客户端感知断连事件之后，释放资源，重新发起连接
*
*   首先监听网络断连的事件，如果Channel关闭，则执行后续的重连任务
*   通过Bootstrap重新发起连接，客户端挂在closeFuture上监听链路关闭信号，一旦关闭，则创建重连定时器，5s之后重新发起连接，直到连接成功(调用的函数中的finally在连接不成功时一直在被执行)
*
*   服务端感知到断连事件之后，需要清空缓存的登录认证注册信息，以保证后续客户端能够正常重连
*
*
* connect:
*   增加NettyMessageDecoder用于Netty消息解码，为了防止由于单条消息过大导致的内存溢出或者畸形码流导致解码错位引起内存分配失败，对单条消息最大长度进行了上限限制。
*   增加NettyMessageEncoder Netty消息编码器，用于协议消息的自动编码
*   随后依次增加了读超时Handler、握手请求Handler、心跳消息Handler
*
*   连接服务端时，绑定了本地端口，主要用于服务端重复登录保护
*
* 利用Netty的ChannelPipeline和ChannelHandler机制，可以非常方便的实现功能解和业务产品的定制。例如本例中的心跳定时器、握手请求和后端的业务处理可以通过不同的Handler来实现，类似AOP
* 通过Handler Chain的机制可以方便的实现切面拦截和定制，相比于AOP它的性能更高
* */
@Component
@Slf4j
public class NettyClient {
    private ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);


    private EventLoopGroup group = new NioEventLoopGroup();;
    private Bootstrap bootstrap;
    private ChannelFuture channelFuture;

    private ClientInitializer clientInitializer;

    @Value("${hostip}")
    public String clientIp;

    @Value("${netty.client.port}")
    public int clientPort;

    @Value("${netty.server.port}")
    public int serverPort;

    @Value("${netty.server.host}")
    public String serverIp;

    @Autowired
    public NettyClient(ClientInitializer clientInitializer) {
        this.clientInitializer = clientInitializer;

        bootstrap = new Bootstrap();
        bootstrap.group(group).channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .handler(clientInitializer);
    }

    public void connect() {

        // 发起异步连接操作
        this.channelFuture = bootstrap.connect(new InetSocketAddress(serverIp, serverPort), new InetSocketAddress(clientIp, clientPort));

        channelFuture.channel().closeFuture().addListener(
                (Future<? super Void> future) -> {
                    if (future.isSuccess()) {
                        log.error("netty client [" + clientIp + ":" + clientPort + "] 连接 netty server [" + serverIp + ":" + serverPort + "] 失败，等待5秒钟进行下一次重连");
                        retryConnect();
                    } else {
                        log.error("netty client channel 关闭失败" + "，退出程序");
                        System.exit(-1);
                    }
                }
        );
    }

    private void retryConnect() {
        // 所有资源释放完成之后，清空资源，再次发起重连操作
        executor.execute(() -> {
                    try {
                        TimeUnit.SECONDS.sleep(5);
                        connect();
                    } catch (InterruptedException e) {
                        log.error("Sleep错误， " + e.getMessage() + "，退出程序");
                        System.exit(-1);
                    }
                }
        );
    }


    public void destroy() {
        if(channelFuture.channel() != null) { channelFuture.channel().close();}
        channelFuture.channel().closeFuture().syncUninterruptibly();
        group.shutdownGracefully();
    }

}
