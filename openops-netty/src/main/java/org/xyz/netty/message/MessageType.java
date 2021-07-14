package org.xyz.netty.message;

public enum MessageType {
    BUSINESS_REQ((byte)0), BUSINESS_RESP((byte)1), BUSINESS_ONE_WAY((byte)2), LOGIN_REQ((byte)3), LOGIN_RESP((byte)4), HEARTBEAT_REQ((byte)5), HEARTBEAT_RESP((byte)6),
    CMD_EXEC((byte)7), CMD_EXEC_RESULT((byte)8);

    private byte msgType;

    private MessageType(byte msgType) {
        this.msgType = msgType;
    }

    public byte value() {
        return msgType;
    }
}

