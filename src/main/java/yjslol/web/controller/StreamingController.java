package yjslol.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import yjslol.entity.ChampionUsageRes;
import yjslol.service.Streaming;

@CrossOrigin
@RestController
public class StreamingController {

    @Autowired
    private Streaming streaming;

    @GetMapping("/streaming/champion/current")
    public ChampionUsageRes getChampionUsage(@RequestParam(required = false) String pos) {
        return streaming.getCurrentChampionUsage(pos);
    }

    @PostMapping("/streaming/start")
    public boolean startStreaming() {
        return streaming.start();
    }
}
