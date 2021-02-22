package vk.nas.networking.nas2nas.schedule;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import vk.nas.networking.nas2nas.server.Nas2nasServer;

/**
 * 单线程定时任务
 */
@Configuration      //1.主要用于标记配置类，兼备Component的效果。
@EnableScheduling   // 2.开启定时任务
public class Nas2nasSchedule {
    @Autowired
    private Nas2nasServer server;

    @Scheduled(fixedRate=3000)
    private void scanNas() {
        server.scanNas();
    }

    @Scheduled(fixedRate=3000)
    private void syncData() {
        server.syncData();
    }
}
