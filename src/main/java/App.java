import entity.Game;
import org.apache.spark.SparkConf;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import scala.Tuple2;

public class App {

    public static void main(String[] args) throws Exception{
        SparkConf conf = new SparkConf().setMaster("local[2]").setAppName("yjslol");
        JavaStreamingContext jsc = new JavaStreamingContext(conf, Durations.seconds(2));

        JavaDStream<Game> gamsStream =
                jsc.receiverStream(new MongoDBReceiver(0, 1556985600)); //2019-05-05 00:00:00
        JavaPairDStream<String, Integer> championOnePairs =
                gamsStream.mapToPair(game -> new Tuple2<>(game.getChampion_name(), 1));

        JavaPairDStream<String, Integer> championCounts = championOnePairs.reduceByKey(Integer::sum);

        championCounts.print(200);

        jsc.start();
        jsc.awaitTermination();
    }
}
