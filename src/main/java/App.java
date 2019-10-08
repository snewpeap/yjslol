import org.apache.spark.SparkConf;

public class App {

    public static void main(String[] args) throws Exception {
        SparkConf conf = new SparkConf().setMaster("local[3]").setAppName("yjslol");
        StreamingImpl streaming = new StreamingImpl(conf);
        streaming.run();
    }
}
