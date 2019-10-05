import com.mongodb.MongoClientSettings;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import entity.Game;
import entity.Summoner;
import org.apache.spark.storage.StorageLevel;
import org.apache.spark.streaming.receiver.Receiver;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import java.util.*;

import static com.mongodb.client.model.Aggregates.project;
import static com.mongodb.client.model.Projections.*;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public class MongoDBReceiver extends Receiver<Game> {
    private int prevTime;
    private int tillTime;

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
        super(StorageLevel.MEMORY_AND_DISK_2());

        this.prevTime = prevTime;
        this.tillTime = tillTime;
    }

    @Override
    public void onStart() {
        new Thread(this::readBD).start();
    }

    @Override
    public void onStop() {

    }

    public void readBD() {
        try {
            CodecRegistry pojoCodecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
                    fromProviders(PojoCodecProvider.builder().automatic(true).build()));
            MongoCollection<Summoner> gamesCollections = MongoDBUtil.getDatabase_LOL()
                    .getCollection("games", Summoner.class)
                    .withCodecRegistry(pojoCodecRegistry);

            List<Game> games = new ArrayList<>(2800);
            while (!isStopped()) {
                Document filterDoc =
                        new Document("input", "$games")
                                .append("as", "game")
                                .append("cond",
                                        new Document(
                                                "$and",
                                                Arrays.asList(
                                                        new Document(
                                                                "$gt",
                                                                Arrays.asList(
                                                                        "$$game.time",
                                                                        prevTime
                                                                )
                                                        ),
                                                        new Document(
                                                                "$lte",
                                                                Arrays.asList(
                                                                        "$$game.time",
                                                                        tillTime
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
                                                        include("sid", "sname", "rank"),
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

                System.out.println("获取从 " + prevTime + " 到 " + tillTime + " 的对局记录");
                store(games.iterator());

                Thread.sleep(3000);

                games.clear();
                prevTime = tillTime;
                tillTime += 2 * 60 * 60;
                if (tillTime > Integer.valueOf(String.valueOf(System.currentTimeMillis() / 1000))) {
                    stop("超过当前日期，停了吧");
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
            restart("获取数据出错", t);
        }
    }
}
