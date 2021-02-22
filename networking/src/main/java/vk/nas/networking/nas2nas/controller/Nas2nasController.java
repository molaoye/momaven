package vk.nas.networking.nas2nas.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import vk.nas.networking.nas2nas.server.Nas2nasServer;

@RestController
@RequestMapping("/nas")
public class Nas2nasController {

    @Autowired
    private Nas2nasServer server;

    @PostMapping("/list")
    @ResponseBody
    public String list(@RequestParam("nasList") String remoteNasList, @RequestParam("ip") String ip){
        return server.getNasList(remoteNasList, ip);
    }

    @PostMapping("/syncData")
    @ResponseBody
    public void syncData(@RequestParam("boxList") String boxList, @RequestParam("box2nasList") String box2nasList, @RequestParam("ip") String ip){
        server.syncDataUpdate(boxList, box2nasList, ip);
    }

}
