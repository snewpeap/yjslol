import entity.Game;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.Optional;
import org.apache.spark.api.java.function.Function3;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.State;
import org.apache.spark.streaming.StateSpec;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaMapWithStateDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import scala.Tuple2;

import java.util.ArrayList;

class StreamingImpl {
    private SparkConf conf;
    private Function3<String, Optional<Integer>, State<Integer>, Tuple2<String, Integer>>
            updateT2Func = (key, one, state) -> {
        int sum = one.orElse(0) + (state.exists() ? state.get() : 0);
        Tuple2<String, Integer> newT2 = new Tuple2<>(key, sum);
        state.update(sum);
        return newT2;
    };

    StreamingImpl(SparkConf conf) {
        this.conf = conf;
    }

    void run() throws Exception {
        JavaStreamingContext jsc = new JavaStreamingContext(conf, Durations.seconds(3));
        jsc.checkpoint("./checkpoint/");

        JavaDStream<Game> gamsStream =
                jsc.receiverStream(new MongoDBReceiver(0, 1556985600)); //2019-05-05 00:00:00
        JavaPairDStream<String, Integer> championOnePairs =
                gamsStream.mapToPair(game -> new Tuple2<>(game.getChampion_name(), 1));

        JavaMapWithStateDStream<String, Integer, Integer, Tuple2<String, Integer>> championCounts = championOnePairs
                .mapWithState(
                        StateSpec
                                .function(updateT2Func)
                                .initialState(jsc.sparkContext().parallelizePairs(new ArrayList<>()))
                );

        championCounts
                .stateSnapshots().mapToPair(Tuple2::swap)
                .transformToPair(s -> s.sortByKey(false))
                .mapToPair(Tuple2::swap).print(20);

        jsc.start();
        jsc.awaitTermination();
    }
}
