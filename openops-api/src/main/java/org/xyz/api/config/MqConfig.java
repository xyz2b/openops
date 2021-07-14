package org.xyz.api.config;

import org.xyz.api.contants.Constants;

public class MqConfig {
    /**
     * 根据具体的实例名得到对应websocket服务的MQ topic地址
     * @param instanceId
     * @return
     */
    public static String getNettyServerTopic(String instanceId){
        return  Constants.MQ_TOPIC_PREFIX + getMqInstance(instanceId);
    }

    /**
     * 根据具体的实例名得到对应websocket服务的MQ topic地址
     * @param instanceId
     * @return
     */
    public static String getNettyServerGroup(String instanceId){
        return  Constants.MQ_GROUP_PREFIX + getMqInstance(instanceId);
    }

    /**
     * mq群组中不允许出现实例中的":"，故替换成"-"
     * @param instanceId
     * @return
     */
    private static String getMqInstance(String instanceId){
        return instanceId.replace(":","-").replace(".","-");
    }
}
