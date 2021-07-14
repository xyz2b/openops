package org.xyz.portal.service.messagedispatch;

import org.xyz.api.entity.SendRequest;

// 消息分发接口定义
public interface MessageDispatchService {
    void send(String instants, SendRequest request);
}
