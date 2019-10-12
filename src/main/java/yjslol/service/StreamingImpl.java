package yjslol.service;

import com.mongodb.client.FindIterable;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.Sorts;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.Optional;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.springframework.stereotype.Service;
import scala.Tuple2;
import yjslol.entity.ChampionCountPair;
import yjslol.entity.ChampionUsageRes;
import yjslol.entity.Game;
import yjslol.mongo.MongoDBUtil;
import yjslol.spark.MongoDBReceiver;

import javax.annotation.PostConstruct;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static yjslol.mongo.MongoDBUtil.COLLECTION_STREAMING;

@Service
public class StreamingImpl implements Streaming, Serializable {
    //    private static boolean isRunning = false;
    private MongoDBReceiver mongoDBReceiver = new MongoDBReceiver(1, 1556985600);

    @PostConstruct
    private void constructed() {
        MongoDBUtil.getCollection(COLLECTION_STREAMING, ChampionCountPair.class).drop();
        RunStatHolder.tillTime = 0;
        start();
    }

    @Override
    public boolean start() {
        if (RunStatHolder.isRunning) {
            return false;
        } else {
            new Thread(this::run).start();
            return true;
        }
    }

    @Override
    public ChampionUsageRes getCurrentChampionUsage(Integer lastT) {
        if (!RunStatHolder.isRunning) {
            return null;
        } else {
            if (lastT == null || lastT >= RunStatHolder.tillTime) {
                return null;
            } else {
                FindIterable<ChampionCountPair> findIterable;
                findIterable = MongoDBUtil.getCollection(COLLECTION_STREAMING, ChampionCountPair.class).find();

                List<ChampionCountPair> pairs = new ArrayList<>();
                findIterable.sort(Sorts.descending("count"))
                        .forEach((Consumer<? super ChampionCountPair>) pairs::add);
                ChampionUsageRes championUsageRes = new ChampionUsageRes();
                championUsageRes.setMap(pairs);
                championUsageRes.setTimestamp(mongoDBReceiver.getTillTime());
                return championUsageRes;
            }
        }
    }

    private void run() {
        RunStatHolder.isRunning = true;
        try {
            SparkConf conf = new SparkConf().setMaster("local[3]").setAppName("yjslol-streaming");
            JavaStreamingContext jsc = new JavaStreamingContext(conf, Durations.milliseconds(3700));
            jsc.checkpoint("./checkpoint/");

            JavaDStream<Game> gamsStream =
                    jsc.receiverStream(mongoDBReceiver); //2019-05-05 00:00:00


//            JavaPairDStream<String, Integer> championOnePairs =
//                    gamsStream.mapToPair(game -> new Tuple2<>(game.getChampion_name(), 1));

            JavaPairDStream<Tuple2<String, String>, Integer> championPosPairs =
                    gamsStream.mapToPair(game -> new Tuple2<>(
                                    new Tuple2<>(game.getChampion_name(), game.getPos()), 1
                            )
                    );

            JavaPairDStream<Tuple2<String, String>, Integer> championCounts = championPosPairs
                    .updateStateByKey((Function2<List<Integer>, Optional<Integer>, Optional<Integer>>)
                            (values, state) -> {
                                Integer newSum = 0;
                                if (state.isPresent()) {
                                    newSum = state.get();
                                }

                                for (Integer i : values) {
                                    newSum += i;
                                }

                                return Optional.of(newSum);
                            }
                    ).persist();

            championCounts.foreachRDD(rdd -> {
                        rdd.foreachPartition(
                                records -> {
                                    while (records.hasNext()) {
                                        Tuple2<Tuple2<String, String>, Integer> t = records.next();
                                        ChampionCountPair pair = new ChampionCountPair();
                                        pair.setCname(t._1._1);
                                        pair.setPos(t._1._2);
                                        pair.setCount(t._2);
                                        MongoDBUtil.getCollection(COLLECTION_STREAMING, ChampionCountPair.class).replaceOne(
                                                and(eq("cname", pair.getCname()), eq("pos", pair.getPos())),
                                                pair,
                                                new ReplaceOptions().upsert(true)
                                        );
                                    }
                                });
                        RunStatHolder.tillTime = mongoDBReceiver.getTillTime();
                    }
            );

//            championCounts
//                    .mapToPair(Tuple2::swap)
//                    .transformToPair(s -> s.sortByKey(false))
//                    .mapToPair(Tuple2::swap).print(5);
            championCounts.print(2);

            jsc.start();
            jsc.awaitTermination();
            jsc.stop();
        } catch (Throwable e) {
            e.printStackTrace();
            RunStatHolder.isRunning = false;
        }
    }


    private static class RunStatHolder {
        static boolean isRunning;
        static int tillTime;
    }
}
