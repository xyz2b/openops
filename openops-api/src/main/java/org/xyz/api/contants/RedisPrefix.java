package org.xyz.api.contants;

public class RedisPrefix {
    // netty server的服务地址，类型-hash<instanceId, nettyServerPort>，key为netty节点的实例名(ip:port, springboot的地址和端口)，value为netty server端口
    public static String NETTY_SERVER = "netty_server";

    //netty server实例与对应连接的权重关系
    public static String NETTY_SERVER_WEIGHT = "netty_server_weight";

    //客户端连接前缀，后缀为客户端标识-hash
    public static String PREFIX_CLIENT = "client_";

    //netty server服务所连接的客户端id集合前缀-list
    public static String PREFIX_SERVER_CLIENTS = "server_clients_";
}
