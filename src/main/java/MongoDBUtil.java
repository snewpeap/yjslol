import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;

public class MongoDBUtil {
    private static MongoClient mongoClient = null;
    private static MongoDatabase Database_LOL = null;

    public static MongoClient getMongoClient() {
        if (mongoClient == null) {
            mongoClient = new MongoClient();
        }
        return mongoClient;
    }

    public static MongoDatabase getDatabase_LOL() {
        if (Database_LOL == null) {
            Database_LOL = getMongoClient().getDatabase("lol");
        }
        return Database_LOL;
    }
}
