package yjslol.entity;

import java.io.Serializable;

public class Game implements Serializable {
    private String gid;
    private int time;
    private int length;
    private String result;
    private String champion_name;
    private String pos;
    private int cs;
    private int kill;
    private int death;
    private int assist;
    private int wards;

    public String getGid() {
        return gid;
    }

    public void setGid(String gid) {
        this.gid = gid;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getChampion_name() {
        return champion_name;
    }

    public void setChampion_name(String champion_name) {
        this.champion_name = champion_name;
    }

    public String getPos() {
        return pos;
    }

    public void setPos(String pos) {
        this.pos = pos;
    }

    public int getCs() {
        return cs;
    }

    public void setCs(int cs) {
        this.cs = cs;
    }

    public int getKill() {
        return kill;
    }

    public void setKill(int kill) {
        this.kill = kill;
    }

    public int getDeath() {
        return death;
    }

    public void setDeath(int death) {
        this.death = death;
    }

    public int getAssist() {
        return assist;
    }

    public void setAssist(int assist) {
        this.assist = assist;
    }

    public int getWards() {
        return wards;
    }

    public void setWards(int wards) {
        this.wards = wards;
    }
}
