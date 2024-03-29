import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.*;
import java.util.List;


/**
 * MongoDB database class.
 */
public class Database {
    MongoClient mongoClient;
    MongoDatabase database;

    /**
     * Constructor.
     *
     * @param name name of database
     */
    public Database(String name) {
        MongoClient mongoClient = MongoClients.create();
        database = mongoClient.getDatabase(name);
    }

    /**
     * Inserts tokens based on classifier (i.e. positive, neutral, negative)
     *
     * @param collectionName name of collection
     * @param classifier classifier type
     * @param cleanTokenizedTweet list of clean and tokenized tweet
     */
    public void insertTokens(String collectionName, String classifier, List<String> cleanTokenizedTweet) {
        MongoCollection<Document> collection = database.getCollection(collectionName);

        for(String token : cleanTokenizedTweet) {
            if(collection.find(eq("_id", token)).first() == null) {
                Document doc = new Document();

                switch(classifier) {
                    case "positive":
                        doc = new Document("_id", token).append("positive" , 1);
                        incrementWordClassCounter(collectionName, "positive");
                        break;
                    case "neutral":
                        doc = new Document("_id", token).append("neutral" , 1);
                        incrementWordClassCounter(collectionName, "neutral");
                        break;
                    case "negative":
                        doc = new Document("_id", token).append("negative" , 1);
                        incrementWordClassCounter(collectionName, "negative");
                        break;
                }

                collection.insertOne(doc);
            } else {
                switch(classifier) {
                    case "positive":
                        collection.updateOne(eq("_id", token), inc("positive", 1));
                        incrementWordClassCounter(collectionName, "positive");
                        break;
                    case "neutral":
                        collection.updateOne(eq("_id", token), inc("neutral", 1));
                        incrementWordClassCounter(collectionName, "neutral");
                        break;
                    case "negative":
                        collection.updateOne(eq("_id", token), inc("negative", 1));
                        incrementWordClassCounter(collectionName, "negative");
                        break;
                }
            }
        }
    }

    /**
     * Increments word class counter by one
     *
     * @param collectionName name of collection
     * @param classifier type of classifier
     */
    private void incrementWordClassCounter(String collectionName, String classifier) {
        MongoCollection<Document> collection_total = database.getCollection(collectionName + "_totals");

        if(collection_total.find(eq("_id", "wordTotal")).first() == null) {
            Document doc = new Document("_id", "wordTotal")
                    .append("positive" , 0)
                    .append("neutral" , 0)
                    .append("negative" , 0);
            collection_total.insertOne(doc);
        }

        collection_total.updateOne(eq("_id", "wordTotal"), inc(classifier, 1));
    }

    /**
     * Increments tweet class counter by one
     *
     * @param collectionName name of collection
     * @param classifier type of classifier
     */
    public void incrementTweetClassCounter(String collectionName, String classifier) {
        MongoCollection<Document> collection_total = database.getCollection(collectionName + "_totals");

        if(collection_total.find(eq("_id", "tweetTotal")).first() == null) {
            Document doc = new Document("_id", "tweetTotal")
                    .append("positive" , 0)
                    .append("neutral" , 0)
                    .append("negative" , 0);
            collection_total.insertOne(doc);
        }

        collection_total.updateOne(eq("_id", "tweetTotal"), inc(classifier, 1));
    }

    /**
     * Gets total of specific classifier (e.g. amount of positive tweets)
     *
     * @param collectionName name of collection
     * @param classifier type of classifier
     * @return amount of specific classified tweets
     */
    public int getClassifierTotal(String collectionName, String classifier) {
        MongoCollection<Document> collection_total = database.getCollection(collectionName + "_totals");

        try {
            return (int) collection_total.find(eq("_id", "tweetTotal")).first().get(classifier);
        } catch(Exception e) {
            return 0;
        }
    }

    /**
     * Gets total of all classified tweets (e.g. total of positive, neutral and negative tweets)
     *
     * @param collectionName name of collection
     * @return amount of total classified tweets
     */
    public int getTotalSet(String collectionName) {
        MongoCollection<Document> collection_total = database.getCollection(collectionName + "_totals");
        String[] classes = {"positive", "neutral", "negative"};
        int total = 0;

        for(String classifier : classes) {
            try {
                total += (int) collection_total.find(eq("_id", "tweetTotal")).first().get(classifier);
            } catch (Exception e) {
                total += 0;
            }
        }

        return total;
    }

    /**
     * Gets total of words in a specific classifier (e.g. positive, neutral, negative)
     *
     * @param collectionName name of collection
     * @param classifier type of classifier
     * @param token token/word
     * @return amount of words in class
     */
    public int getClassifierWordTotal(String collectionName, String classifier, String token) {
        MongoCollection<Document> collection_total = database.getCollection(collectionName);

        try {
            return (int) collection_total.find(eq("_id", token)).first().get(classifier);
        } catch(Exception e){
            return 0;
        }
    }

    /**
     * Gets total amount of words from all classifiers
     *
     * @param collectionName name of collection
     * @return Amount of total words
     */
    public int getTotalWordSet(String collectionName) {
        MongoCollection<Document> collection_total = database.getCollection(collectionName + "_totals");
        String[] classes = {"positive", "neutral", "negative"};
        int total = 0;

        for(String classifier : classes) {
            try {
                total += (int) collection_total.find(eq("_id", "wordTotal")).first().get(classifier);
            } catch(Exception e) {
                total += 0;
            }
        }

        return total;
    }

    /**
     * Checks if collection exists
     *
     * @param collectionName name of collection
     * @return Boolean value if collection exists
     */
    public boolean checkIfCollectionExists(String collectionName) {
        MongoCollection<Document> collection = database.getCollection(collectionName + "_training_totals");

        try {
            return !collection.find().first().equals(null);
        }
        catch(Exception e) {
            return false;
        }
    }
}
