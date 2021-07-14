package org.xyz.netty.client.handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.xyz.netty.message.Header;
import org.xyz.netty.message.MessageType;
import org.xyz.netty.message.NettyMessage;
import org.xyz.netty.util.NettyMessageBuilder;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/*
* 握手成功之后，由客户端主动发送心跳消息，服务端接收到心跳消息后，返回心跳应答消息。
* 由于心跳消息的目的是为了检测链路的可用性，因此不需要携带消息体
*
* channelRead:
*   当握手成功后，握手请求Handler会继续将握手消息向下透传，HeartBeatReqHandler接收到之后对消息进行判断，如果是握手成功消息，则启动无限循环定时器用于定期发送心跳消息
*   由于NIOEventLoop是一个schedule，因此它支持定时器的执行。心跳定时器的单位是毫秒，默认为5000，即每5秒发送一条心跳消息
*   为了统一在同一个Handler中处理所有心跳消息，所以同时判断了客户端接收到的消息如果是服务端的心跳应答消息，就打印出客户端接收的心跳消息
*
* HeaderBeatTask
*   心跳定时器的实现很简单，通过构造函数获取ChannelHandlerContext，构造心跳消息并发送
* */
@ChannelHandler.Sharable
@Slf4j
@Component
public class HeartBeatReqHandler extends ChannelInboundHandlerAdapter {
    private volatile ScheduledFuture<?> heartBeat;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        NettyMessage message = (NettyMessage) msg;
        // 握手成功，主动发送心跳消息
        if (message.getHeader() != null && message.getHeader().getType() == MessageType.LOGIN_RESP.value()) {
            /*
            * 通过java在做定时任务的时候最好使用scheduleThreadPoolExecutor的方式，因为这样可以保证里面始终以后线程是活的。scheduleThreadPoolExecutor有三种任务
                执行的方式：
                1.scheduleAtFixedRate(commod,initialDelay,period,unit)

                initialDelay是说系统启动后，需要等待多久才开始执行。

                period为固定周期时间，按照一定频率来重复执行任务。

                如果period设置的是3秒，系统执行要5秒；那么等上一次任务执行完就立即执行，也就是任务与任务之间的差异是5s；
                如果period设置的是3s，系统执行要2s；那么需要等到3S后再次执行下一次任务。


                2.scheduleWithFixedDelay(commod,initialDelay,delay,unit)

                initialDelay是说系统启动后，需要等待多久才开始执行。

                period为固定周期时间，按照一定频率来重复执行任务。
                这个方式必须等待上一个任务结束才开始计时period。
                如果设置的period为3s，任务执行耗时为5S，那么下次任务执行时间为第8S。


                3.schedule(commod,initialDelay，unit))比较简单,系统开始initialDelay后开始执行commod任务，执行完完事。
             * */
            // 创建周期性发送心跳的Task，丢给其他线程周期性执行
            heartBeat = ctx.executor().scheduleAtFixedRate(new HeaderBeatTask(ctx), 0, 5000, TimeUnit.MILLISECONDS);
            ctx.fireChannelRead(msg);
        } else if (message.getHeader() != null && message.getHeader().getType() == MessageType.HEARTBEAT_RESP.value()) {
            log.debug("Client receive netty heartbeat message: ----> " + message);
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    // 发送心跳的Task，实现Runnable，可以丢给其他线程执行
    private static class HeaderBeatTask implements Runnable {
        private final ChannelHandlerContext ctx;

        public HeaderBeatTask(final ChannelHandlerContext ctx) {
            this.ctx = ctx;
        }

        @Override
        public void run() {
            NettyMessage heartBeat = NettyMessageBuilder.builder(MessageType.HEARTBEAT_REQ);
            log.debug("Client send heartbeat message to netty: ----> " + heartBeat);
            ctx.writeAndFlush(heartBeat);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("发生错误："  + cause.getMessage());
        // 发生错误，停止周期发送心跳的Task
        if (heartBeat != null) {
            heartBeat.cancel(true);
            heartBeat = null;
        }
        ctx.fireExceptionCaught(cause);
    }
}
