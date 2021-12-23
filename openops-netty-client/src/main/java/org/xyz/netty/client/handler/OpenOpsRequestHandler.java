package org.xyz.netty.client.handler;

import io.netty.channel.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.xyz.api.entity.CmdResult;
import org.xyz.api.util.DateUtils;
import org.xyz.netty.message.MessageType;
import org.xyz.netty.message.NettyMessage;
import org.xyz.netty.util.NettyMessageBuilder;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.*;

@Slf4j
@ChannelHandler.Sharable
@Component
public class OpenOpsRequestHandler extends ChannelDuplexHandler {
    // TODO: 线程池数量动态计算，执行时长较长和执行时间较短的任务（通过超时时间判断，任务执行超时取消任务）
    private static ExecutorService executor = Executors.newFixedThreadPool(32);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        NettyMessage message = (NettyMessage) msg;
        if (message.getHeader() != null && message.getHeader().getType() == MessageType.CMD_EXEC.value()) {
            log.info("执行命令：" + message.getBody());
            log.info("channel: " + ctx.channel().id().asLongText());

            Map<String, Object> attachment = message.getHeader().getAttachment();
            attachment.put("remoteAddr", "127.0.0.1");

            executor.submit(() ->
                {
                    Map<String, String> currentEnv = System.getenv();
                    ProcessBuilder pb = new ProcessBuilder("/bin/sh", (String) message.getBody());
//                    pb.directory(new File("/tmp"));
                    Map<String, String> env = pb.environment();
                    env.clear();
                    for (Map.Entry<String, String> entry : currentEnv.entrySet()) {
                        env.put(entry.getKey(), entry.getValue());
                    }
                    log.error(pb.environment().toString());

                    int runningStatus = 0;
                    String s = null;
                    CmdResult cmdResult = new CmdResult();
                    StringBuilder stdOut = new StringBuilder(1000);
                    StringBuilder stdErrOut = new StringBuilder(1000);
                    StringBuilder err = new StringBuilder(100);

                    try {
                        Process p = pb.start();
                        cmdResult.setStartTime(DateUtils.dateToDateTime(new Date()));

                        BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
                        BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));

                        int stdOutLine = 0;
                        // 默认读取100行
                        while ((s = stdInput.readLine()) != null && stdOutLine < 100) {
                            stdOut.append(s);
                            stdOutLine++;
                        }

                        int stdErrOutLine = 0;
                        // 默认读取100行
                        while ((s = stdError.readLine()) != null && stdErrOutLine < 100) {
                            stdErrOut.append(s);
                            stdErrOutLine++;
                        }

                        try {
                            runningStatus = p.waitFor();

                            cmdResult.setEndTime(DateUtils.dateToDateTime(new Date()));

                            // TODO: 取消机制
//                            p.destroy();

                            cmdResult.setStatus(runningStatus);
                            log.info("UUID: " + attachment.get("UUID") +", " + "执行状态：" + runningStatus);
                        } catch (InterruptedException e) {
                            log.error("进程被中断，" + "UUID: " + attachment.get("UUID") +", " + "获取执行状态出错：" + e.getMessage());
                            err.append(e.getMessage());
                        }
                    } catch (Exception e) {
                        log.error("UUID: " + attachment.get("UUID") +", " + "执行命令出错：" + e.getMessage());
                        err.append(e.getMessage());
                    }
                    cmdResult.setStdOut(stdOut.toString());
                    cmdResult.setStdErrOut(stdErrOut.toString());
                    cmdResult.setError(err.toString());
                    cmdResult.setUuid(attachment.get("UUID").toString());
                    cmdResult.setClient(attachment.get("remoteAddr").toString());

                    log.info("UUID: " + attachment.get("UUID") +", " + "执行结果: " + cmdResult);
                    log.info("UUID: " + attachment.get("UUID") +", " + "channel: " + ctx.channel().id().asLongText());
                    ChannelFuture future = ctx.writeAndFlush(NettyMessageBuilder.builder(MessageType.CMD_EXEC_RESULT, attachment, cmdResult));
                    future.addListener((ChannelFuture channelFuture) -> {
                        if (future.isDone()) {
                            if (future.isSuccess()) {
                                log.info("UUID: " + attachment.get("UUID") +", " + "发送成功！！！！");
                            } else if (future.isCancelled()) {
                                log.info("UUID: " + attachment.get("UUID") +", " + "cancelled!!");
                            } else  {
                                log.error("UUID: " + attachment.get("UUID") +", " + future.cause().getMessage());
                            }
                        } else {
                            log.error("UUID: " + attachment.get("UUID") +", " + "not done!!!!!!");
                        }
                    });
                }
            );

        } else {
            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }
}
