package vk.nas.cloud.schedule;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import vk.nas.cloud.server.CloudServer;

/**
 * 单线程定时任务
 */
@Configuration      //1.主要用于标记配置类，兼备Component的效果。
@EnableScheduling   // 2.开启定时任务
public class CloudSchedule {
    @Autowired
    private CloudServer server;

    @Scheduled(fixedRate=3000)
    private void test() {
        server.test("");
    }

}
