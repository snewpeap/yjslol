package yjslol.spark;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import org.apache.spark.storage.StorageLevel;
import org.apache.spark.streaming.receiver.Receiver;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import yjslol.entity.Game;
import yjslol.entity.Summoner;
import yjslol.mongo.MongoDBUtil;

import java.io.Serializable;
import java.util.*;

import static com.mongodb.client.model.Aggregates.project;
import static com.mongodb.client.model.Projections.*;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public class MongoDBReceiver extends Receiver<Game> implements Runnable {
//    private int prevTime;
//    private int tillTime;

    public MongoDBReceiver(StorageLevel storageLevel) {
        super(storageLevel);
    }

    public MongoDBReceiver() {
        this(0, 1556640000); //2019-10-01 00:00:00
    }

    public MongoDBReceiver(int tillTime) {
        this(0, tillTime);
    }

    public MongoDBReceiver(int prevTime, int tillTime) {
        super(StorageLevel.MEMORY_AND_DISK());

        TimeHolder.prevTime = prevTime;
        TimeHolder.tillTime = tillTime;
    }

    @Override
    public void onStart() {
        this.run();
    }

    @Override
    public void onStop() {
    }

    @Override
    public void run() {
        try {
            CodecRegistry pojoCodecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
                    fromProviders(PojoCodecProvider.builder().automatic(true).build()));
            MongoCollection<Summoner> gamesCollections = MongoDBUtil.getDatabase_LOL()
                    .getCollection("games", Summoner.class)
                    .withCodecRegistry(pojoCodecRegistry);

            List<Game> games = new ArrayList<>(2800);
            Document filterDoc;
            while (!isStopped()) {
                int tt = TimeHolder.tillTime;
                int pt = TimeHolder.prevTime;
                filterDoc =
                        new Document("input", "$games").append("as", "game")
                                .append("cond", new Document(
                                                "$and",
                                                Arrays.asList(new Document(
                                                                "$gt",
                                                                Arrays.asList(
                                                                        "$$game.time",
                                                                        pt
                                                                )
                                                        ), new Document(
                                                                "$lte",
                                                                Arrays.asList(
                                                                        "$$game.time",
                                                                        tt
                                                                )
                                                        )
                                                )
                                        )
                                );

                AggregateIterable<Summoner> findIterable = gamesCollections
                        .aggregate(
                                Collections.singletonList(
                                        project(
                                                fields(
                                                        excludeId(),
                                                        include("sid", "sname", "pos", "rank"),
                                                        computed(
                                                                "games",
                                                                new Document(
                                                                        "$filter",
                                                                        filterDoc
                                                                )
                                                        )
                                                )
                                        )
                                )
                        );

                Iterator<Summoner> summonerIterator = findIterable.iterator();
                Summoner summoner;
                while (summonerIterator.hasNext()) {
                    summoner = summonerIterator.next();
                    if (summoner.getGames() != null) {
                        games.addAll(summoner.getGames());
                    }
                }

                System.out.println("获取从 " + pt + " 到 " + tt + " 的对局记录");
                store(games.iterator());

                if (tt > Integer.valueOf(String.valueOf(System.currentTimeMillis() / 1000))) {
                    stop("超过当前日期，停了吧");
                }
                Thread.sleep(1000);

                games.clear();
                TimeHolder.prevTime = tt;
                TimeHolder.tillTime += 24 * 60 * 60;
            }
        } catch (Throwable t) {
            t.printStackTrace();
            restart("获取数据出错", t);
        }
    }

    public int getTillTime() {
        return TimeHolder.tillTime;
    }

    private static class TimeHolder implements Serializable {
        static int prevTime;
        static int tillTime;
    }
}
