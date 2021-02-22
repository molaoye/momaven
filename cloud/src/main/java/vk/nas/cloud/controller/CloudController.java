package vk.nas.cloud.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import vk.nas.cloud.server.CloudServer;

@RestController
@RequestMapping("/cloud")
public class CloudController {

    @Autowired
    private CloudServer server;

    @PostMapping("/test")
    @ResponseBody
    public void test(@RequestParam("ip") String ip){
        server.test(ip);
    }

}
