package yjslol.entity;

import java.util.List;

public class ChampionUsageRes {
    private int timestamp;
    private List<ChampionCountPair> map;

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    public List<ChampionCountPair> getMap() {
        return map;
    }

    public void setMap(List<ChampionCountPair> map) {
        this.map = map;
    }
}
