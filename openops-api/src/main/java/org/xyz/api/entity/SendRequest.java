package org.xyz.api.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SendRequest extends AbstractRequest implements Serializable {

    /**
     * 发送目的地(客户端)
     */
    private List<String> to = new ArrayList<>();
    /**
     * 发送内容
     */
    private String msg;

    /**
     * 推送发起者默认系统
     */
    private String from = "system";

    private Integer delay;

    public SendRequest(List<String> to, String msg) {
        this.to = to;
        this.msg = msg;
    }
}

