package org.xyz.server.netty.handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.xyz.netty.message.Header;
import org.xyz.netty.message.MessageType;
import org.xyz.netty.message.NettyMessage;
import org.xyz.netty.util.NettyMessageBuilder;
import org.xyz.server.constants.AttrConstants;
import org.xyz.server.service.channel.ChannelService;
import org.xyz.server.util.LogUtil;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/*
* 开头定义重复登录保护和IP认证白名单列表，主要用于提升握手的可靠性
*
* channelRead:
*   先进行接入认证，首先根据客户端的源地址进行重复登录判断，如果客户端已经登录成功，拒绝重复登录，以防止由于客户端重复登录导致的句柄泄露
*   随后通过ChannelHandlerContext的Channel接口获取客户端的InetSocketAddress地址，从中取得发送方的源地址信息，通过源地址进行白名单校验，校验通过握手成功，否则握手失败
*   最后构造握手应答消息返回给客户端
*
* exceptionCaught:
*   当发生异常关闭链路的时候，需要将客户端的信息从登录注册表中去除，以保证后续客户端可以重连成功
* */

@Slf4j
@Component
@ChannelHandler.Sharable
public class LoginAuthRespHandler extends ChannelInboundHandlerAdapter {
    private Map<String, Boolean> nodeCheck = new ConcurrentHashMap<String, Boolean>();
    private String[] whiteList = {"127.0.0.1", "10.39.170.38"};

    @Autowired
    private ChannelService channelService;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        NettyMessage message = (NettyMessage) msg;

        // 如果是握手请求消息，处理，其他消息透传
        if (message.getHeader() != null && message.getHeader().getType() == MessageType.LOGIN_REQ.value()) {
            String remoteAddress = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();

            // 标记 真正 的客户端地址，中间有代理的情况
            Object remoteAddr = message.getHeader().getAttachment().get("remoteAddr");
            if (remoteAddr != null) {
                log.info("login remoteAddr: " + (String) remoteAddr);
                ctx.channel().attr(AttrConstants.remoteAddr).set((String) remoteAddr);
                remoteAddress = (String) remoteAddr;
            }

            NettyMessage loginResp = null;
            // 登录校验
            if (valid(remoteAddress)) {    // 校验成功
                // 标记 channel 登录属性
                LogUtil.markAsLogin(ctx.channel());

                // 将客户端加入缓存中
                addClientToRedis(ctx);

                nodeCheck.put(remoteAddress.toString(), true);
                loginResp = NettyMessageBuilder.builder(MessageType.LOGIN_RESP, (byte) 0);
            } else { // 校验失败
                loginResp = NettyMessageBuilder.builder(MessageType.LOGIN_RESP, (byte) -1);
            }

            log.info("The login response is: " + loginResp + " body [" + loginResp.getBody() + "]");
            ctx.writeAndFlush(loginResp);
        } else {
            ctx.fireChannelRead(msg);
        }
    }

//    private void saveClientInfo(ChannelHandlerContext ctx) {
//        InetSocketAddress ipSocket = (InetSocketAddress) ctx.channel().remoteAddress();
//        String clientIp = ipSocket.getAddress().getHostAddress();
//
//        // TODO: 将客户端ID和SocketChannel绑定关系保存到redis
//    }

    private void addClientToRedis(ChannelHandlerContext ctx) {
        InetSocketAddress ipSocket = (InetSocketAddress)ctx.channel().remoteAddress();
        String clientIp = ipSocket.getAddress().getHostAddress();

        // 如果经过代理，这里获取真正的客户端IP作为clientId
        String remoteAddr = ctx.channel().attr(AttrConstants.remoteAddr).get();
        if (StringUtils.hasText(remoteAddr)) {
            log.info("addClientToRedis remoteAddr: " + remoteAddr);
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

    private boolean valid(String remoteAddress) {
        NettyMessage loginResp = null;
        // 重复登录，拒绝
        if (nodeCheck.containsKey(remoteAddress)) {
            log.error("重复登录，拒绝");
            return false;
        } else {
            boolean isOK = false;
            // IP白名单认证
            for (String WIP : whiteList) {
                if (WIP.equals(remoteAddress)) {
                    isOK = true;
                    break;
                }
            }
            return isOK;
        }
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        String remoteAddress = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();

        // 标记 真正 的客户端地址，中间有代理的情况
        String remoteAddr = ctx.channel().attr(AttrConstants.remoteAddr).get();
        if (remoteAddr != null) {
            log.info("remove remoteAddr: " + remoteAddr);
            remoteAddress = remoteAddr;
        }
        nodeCheck.remove(remoteAddress); // 删除缓存

        removeClientFromRedis(ctx);
    }

    private void removeClientFromRedis(ChannelHandlerContext ctx) {
            // 从redis中移除客户端信息
            String clientIp = channelService.get(ctx.channel());
            if (StringUtils.hasText(clientIp)) {
                channelService.remove(clientIp);
            }

            log.info("handler removed, " + ctx.channel().id().asLongText());
    }
}
