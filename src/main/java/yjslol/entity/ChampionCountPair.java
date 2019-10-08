package yjslol.entity;

import java.io.Serializable;

public class ChampionCountPair implements Serializable {
    private String cname;
    private String pos;
    private int count;

    public String getCname() {
        return cname;
    }

    public void setCname(String cname) {
        this.cname = cname;
    }

    public String getPos() {
        return pos;
    }

    public void setPos(String pos) {
        this.pos = pos;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
