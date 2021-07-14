package org.xyz.server.service.message.messagedispatch;

import org.xyz.api.entity.CmdResult;
import org.xyz.api.entity.SendRequest;

// 消息分发接口定义
public interface MessageDispatchService {
    void send(String instants, CmdResult request);
}
