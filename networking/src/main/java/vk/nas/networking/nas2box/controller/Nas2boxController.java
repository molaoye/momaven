package vk.nas.networking.nas2box.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import vk.nas.networking.nas2box.server.Nas2boxServer;

@RestController
@RequestMapping("/box")
public class Nas2boxController {

    @Autowired
    private Nas2boxServer server;

    @PostMapping("/report")
    @ResponseBody
    public String report(@RequestParam("ip") String ip){
        return server.report(ip);
    }

    @PostMapping("/nasList")
    @ResponseBody
    public String nasList(@RequestParam("ip") String ip){
        return server.getNasList(ip);
    }

    @PostMapping("/accessNas")
    @ResponseBody
    public String accessNas(@RequestParam("ip") String ip){
        return server.getAccessNas(ip);
    }

}
