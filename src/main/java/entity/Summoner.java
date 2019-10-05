package entity;

import java.util.List;

public class Summoner {
    private String sid;
    private String sname;
    private int rank;
    private List<Game> games;

    public String getSid() {
        return sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }

    public String getSname() {
        return sname;
    }

    public void setSname(String sname) {
        this.sname = sname;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public List<Game> getGames() {
        return games;
    }

    public void setGames(List<Game> games) {
        this.games = games;
    }

    @Override
    public String toString() {
        return "Summoner{" +
                "sid='" + sid + '\'' +
                ", sname='" + sname + '\'' +
                ", rank=" + rank +
                ", games=[" + (games == null ? "null]" : games.size() + " elements]") +
                '}';
    }
}
