package org.xyz.server.netty.handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.xyz.netty.message.NettyMessage;
import org.xyz.server.constants.AttrConstants;
import org.xyz.server.service.channel.ChannelService;
import org.xyz.netty.message.MessageType;
import org.xyz.netty.util.NettyMessageBuilder;

import java.net.InetSocketAddress;

@Slf4j
@Component
@ChannelHandler.Sharable
public class ClientManagementHandler extends ChannelInboundHandlerAdapter {
    @Autowired
    private ChannelService channelService;

    // 用于记录和管理该Server上所连接的所有客户端的channel
    private static ChannelGroup clients = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    // 当客户端连接服务端之后（打开连接）
    // 获取客户端channel，并且放到ChannelGroup中进行管理
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        clients.add(ctx.channel());

        InetSocketAddress ipSocket = (InetSocketAddress)ctx.channel().remoteAddress();
        String clientIp = ipSocket.getAddress().getHostAddress();

        // 如果经过代理，这里获取真正的客户端IP作为clientId
        String remoteAddr = ctx.channel().attr(AttrConstants.remoteAddr).get();
        if (StringUtils.hasText(remoteAddr)) {
            log.info("remoteAddr: " + remoteAddr);
            clientIp = remoteAddr;
        }

        if (channelService.get(clientIp) == null) {
            channelService.put(clientIp, ctx.channel());
        } else {
            ctx.writeAndFlush(NettyMessageBuilder.builder(MessageType.BUSINESS_RESP, ("There is already a connection to the client " + clientIp).getBytes()));
            ctx.channel().close();
        }

        log.info("{} client channel {} save to channel group success\n", clientIp, ctx.channel().id().asLongText());
    }

    // 不管是否发生异常，都会执行handlerRemoved方法
    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        // 当触发handlerRemoved，ChannelGroup会自动移除对应客户端的channel
//        clients.remove(ctx.channel());

        String clientIp = channelService.get(ctx.channel());
        if (StringUtils.hasText(clientIp)) {
            channelService.remove(clientIp);
        }

        log.info("handler removed, " + ctx.channel().id().asLongText());
    }


    // 当传来的消息不能正确被编码，就会造成编码异常，然后异常会从pipeline一直往下传，传到这里
    // 发生错误时，该Handler会被自动移除，触发handlerRemoved事件
//    @Override
//    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
//        ctx.fireExceptionCaught(cause);
//    }


    // channelHandler 的生命周期
    // 对于服务端来说
    // 正常建立连接，打开 SocketChannel
    // ChannelHandler 回调方法的执行顺序为
    //      handlerAdded() -> channelRegistered() -> channelActive() -> channelRead() -> channelReadComplete()
    // 正常关闭连接，关闭 SocketChannel
    // ChannelHandler 回调方法的执行顺序为
    //      channelInactive() -> channelUnregistered() -> handlerRemoved()

    // 出现异常(没有channelRead，多了exceptionCaught)
    // handlerAdded() -> channelRegistered() -> channelActive() -> exceptionCaught() -> channelReadComplete()
    // channelInactive() -> channelUnregistered() -> handlerRemoved()


    // 在正常建立连接过程中，某个回调方法内如果执行了关闭context或channel的操作( ctx.close() 或 ctx.channel().close() )，则后续正常建立连接过程中的回调方法不会被执行
    // 如在channelRegistered中执行ctx.close
    //      handlerAdded() -> channelRegistered()

    // 在异常捕获回调方法中执行关闭context或channel的操作，也不会中断后续回调方法的执行(如在exceptionCaught中)
    // 在正常关闭连接过程的回调方法中执行关闭context或channel的操作，并不会中断后续回调方法的执行(如在channelInactive中)


    // 多个handler的方法执行顺序
    // 先顺序执行完所有handler的某个方法之后，再顺序执行所有handler的下一个方法
    // 比如，先顺序执行所有handler的handlerAdded()，之后再顺序执行channelRegistered()，以此类推
}
