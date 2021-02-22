package vk.nas.networking.nas2box.schedule;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import vk.nas.networking.nas2box.server.Nas2boxServer;

/**
 * 单线程定时任务
 */
@Configuration      //1.主要用于标记配置类，兼备Component的效果。
@EnableScheduling   // 2.开启定时任务
public class Nas2boxSchedule {
    @Autowired
    private Nas2boxServer server;

    @Scheduled(fixedRate=3000)
    private void scanBox() {
        server.scanBox();
    }

    @Scheduled(fixedRate=3000)
    private void updateBox2nasList() {
        server.updateBox2nasList();
    }

    @Scheduled(fixedRate=3000)
    private void printAllList() {
        server.printAllList();
    }
}
