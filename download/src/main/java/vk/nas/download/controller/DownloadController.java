package vk.nas.download.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import vk.nas.download.server.DownloadServer;

@RestController
@RequestMapping("/download")
public class DownloadController {

    @Autowired
    private DownloadServer server;

    @PostMapping("/test")
    @ResponseBody
    public void test(@RequestParam("ip") String ip){
        server.test(ip);
    }

}
