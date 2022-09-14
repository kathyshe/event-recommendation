package db.mongodb;

import java.text.ParseException;

import org.bson.Document;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;


public class MongoDBCreation {
	 // Run as Java application to create MongoDB collections with index.
	  public static void main(String[] args) throws ParseException {
			// Step 1, connetion to MongoDB
			MongoClient mongoClient = MongoClients.create(); // this == create("localhost", "27107");
			MongoDatabase db = mongoClient.getDatabase(MongoDBUtil.DB_NAME);

			// Step 2, remove old collections. // different from mysql, sequence of dropping doesn't matter
			db.getCollection("users").drop();
			db.getCollection("items").drop();

			// Step 3, create new collections
			IndexOptions indexOptions = new IndexOptions().unique(true);
			db.getCollection("users").createIndex(new Document("user_id", 1), indexOptions);
			db.getCollection("items").createIndex(new Document("item_id", 1), indexOptions);

			// Step 4, insert fake user data and create index.
			db.getCollection("users").insertOne(
					new Document().append("user_id", "1217").append("password", "19931217")
							.append("first_name", "Jayden").append("last_name", "Jiang"));

			mongoClient.close();
			System.out.println("Import is done successfully.");
	  }

}
