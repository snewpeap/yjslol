package yjslol.service;

import yjslol.entity.ChampionUsageRes;

public interface Streaming {
    boolean start();

    ChampionUsageRes getCurrentChampionUsage(String pos);

    enum POS{
        TOP("上单"),
        JUG("打野"),
        MID("中单"),
        ADC("下路"),
        SUP("辅助");

        final String pos;

        POS(String pos) {
            this.pos = pos;
        }
    }
}
