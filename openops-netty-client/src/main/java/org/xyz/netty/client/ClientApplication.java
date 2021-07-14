package org.xyz.netty.client;

import io.netty.channel.ChannelFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.util.StringUtils;
import org.xyz.api.util.NetUtils;

@Slf4j
@SpringBootApplication
public class ClientApplication implements CommandLineRunner {
    private static String yamlHostKey = "hostIP";

    @Autowired
    private NettyClient nettyClient;

    @Value("${hostip}")
    private static String clientHost;

    @Value("${eureka.instance.instance-id}")
    private static String instanceId;

    public static void main(String[] args) {
        init();
        SpringApplication app = new SpringApplication(ClientApplication.class);
        // spring boot 设置为不以web方式启动，因为client不需要web，只需要启动netty即可
        app.setWebApplicationType(WebApplicationType.NONE);
        app.run(args);
    }

    private static void init(){
        //根据网卡动态设置ip（为了将netty的ip:port维护进redis）
        initIp();
    }

    private static void initIp() {
        //优先获取jvm参数中指定的ip
        // getProperty获取到的是 yamlKey的string值，并非操作系统的变量

        // System.getProperty 设置 JVM 系统的全局属性，相当于一个静态变量
        clientHost = System.getProperty(yamlHostKey);
        if (StringUtils.hasText(clientHost)) {
            log.info("jvm启动参中指定ip为[{}]", clientHost);
            return ;
        } else {
            String localIP = NetUtils.getLocalHost();
            System.setProperty(yamlHostKey, localIP);
            log.info("自动获取ip为[{}]", localIP);
        }
        log.info("若此ip不是与网关gateway通信的内网ip，请尝试通过启动参数指定[{}]","java -jar -Dhostip=x.x.x.x");
    }

    @Override
    public void run(String... args) throws Exception {
        nettyClient.connect();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> nettyClient.destroy()));
    }
}
