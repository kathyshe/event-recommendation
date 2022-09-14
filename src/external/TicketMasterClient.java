package external;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import entity.Item;
import entity.Item.ItemBuilder;

public class TicketMasterClient {
	// final URL
	private static final String HOST = "https://app.ticketmaster.com";
	private static final String ENDPOINT = "/discovery/v2/events.json";
	private static final String DEFAULT_KEYWORD = "event";
	private static final String API_KEY = "YHcoosIdLux6CTUVgroRQUa6Q1WwxMZj"; // consumer key
	
	public List<Item> search(double lat, double lon, String keyword) { // The interface was JSONArray at first
///// before set URL, need to handle corner cases /////
		if (keyword == null) {
			keyword = DEFAULT_KEYWORD;
		}
		try {
			// purpose: convert the formats which URL can not parse to those URL support [转译]
			// for backend test 
			keyword = URLEncoder.encode(keyword, "UTF-8"); // "Rick Sun" => "Rick%20Sun"
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
///// set URL /////
		// find parameters from TicketMaster, and add it here if needed:
		String geoHash = GeoHash.encodeGeohash(lat, lon, 8);
		String query = String.format("apikey=%s&geoPoint=%s&keyword=%s&radius=%s", API_KEY, geoHash, keyword, 50);
		String url = HOST + ENDPOINT + "?" + query;
		StringBuilder responseBody = new StringBuilder();
		try {
			// Create a URLConnection instance that represents a connection to the remote
			// object referred to by the URL. The HttpUrlConnection class allows us to
			// perform basic HTTP requests without the use of any additional libraries.
			HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection(); // Note
			connection.setRequestMethod("GET"); // request method to TicketMaster

			// Get the status code from an HTTP response message. To execute the request we
			// can use the getResponseCode(), connect(), getInputStream() or
			// getOutputStream() methods.
			int responseCode = connection.getResponseCode(); // 2 uses: sent the request & the corresponding response body
															 // how this function is realized
			System.out.println("Sending requets to url: " + url);
			System.out.println("Response code: " + responseCode);

			if (responseCode != 200) {
				return new ArrayList<>();
			}
			// Create a BufferedReader to help read text from a character-input stream.
			// Provide for the efficient reading of characters, arrays, and lines.
			BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream())); // get input stream: input of client is the response of the server
			String line;
			while ((line = reader.readLine()) != null) {
				responseBody.append(line);
			}
			reader.close(); // not necessary, but help release the memory space
			
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			// Extract events array only.
			JSONObject obj = new JSONObject(responseBody.toString());
			if (!obj.isNull("_embedded")) { 
				JSONObject embedded = obj.getJSONObject("_embedded"); // response structure : "_embedded" key -> "events" key
																	  // Documentations of Discovery API
				return getItemList(embedded.getJSONArray("events"));
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return new ArrayList<>(); // need to purify this response
	}
	
	// Convert JSONArray to a list of item objects.
	private List<Item> getItemList(JSONArray events) throws JSONException {
		List<Item> itemList = new ArrayList<>();
		for (int i = 0; i < events.length(); ++i) {
			JSONObject event = events.getJSONObject(i);
			
			ItemBuilder builder = new ItemBuilder();
			if (!event.isNull("id")) {
				builder.setItemId(event.getString("id"));
			}
			if (!event.isNull("name")) {
				builder.setName(event.getString("name"));
			}
			if (!event.isNull("url")) {
				builder.setUrl(event.getString("url"));
			}
			if (!event.isNull("distance")) {
				builder.setDistance(event.getDouble("distance"));
			}
			
			builder.setAddress(getAddress(event));
			builder.setCategories(getCategories(event));
			builder.setImageUrl(getImageUrl(event));
			
			itemList.add(builder.build());
		}

		return itemList;	
	}
	
	/**
	 * Helper methods
	 */
	
	// Helper 1: the helper of getting Address
	private String getAddress(JSONObject event) throws JSONException { // TicketMaster documentation: event -> array item object
		// whether the needed key "_embedded" exists or not
		if (!event.isNull("_embedded")) { 
			JSONObject embedded = event.getJSONObject("_embedded"); // _embedded is a JSON object
			// find the "venues" key
			if (!embedded.isNull("venues")) {
				JSONArray venues = embedded.getJSONArray("venues"); // venues is a JSON array
				// find the first place of the event (required by front end); if found, return result:
				for (int i = 0; i < venues.length(); ++i) {
					JSONObject venue = venues.getJSONObject(i);
					StringBuilder builder = new StringBuilder();
					if (!venue.isNull("address")) {
						JSONObject address = venue.getJSONObject("address");
						// three strings under address:
						if (!address.isNull("line1")) {
							builder.append(address.getString("line1"));
						}
						
						if (!address.isNull("line2")) {
							builder.append(",");
							builder.append(address.getString("line2"));
						}
						
						if (!address.isNull("line3")) {
							builder.append(",");
							builder.append(address.getString("line3"));
						}
					}
					
					if (!venue.isNull("city")) {
						JSONObject city = venue.getJSONObject("city");
						builder.append(",");
						builder.append(city.getString("name"));
					}
					
					String result = builder.toString();
					if (!result.isEmpty()) {
						return result;
					}
				}
			}
		}
		return "";	
	}
	
	// Helper 2: the helper of getting ImageURL
	private String getImageUrl(JSONObject event) throws JSONException {
		if (!event.isNull("images")) {
			JSONArray array = event.getJSONArray("images");
			for (int i = 0; i < array.length(); i++) {
				JSONObject image = array.getJSONObject(i);
				if (!image.isNull("url")) {
					return image.getString("url");
				}
			}
		}
		return "";
	}
	
	// Helper 3: the helper of getting Categories
	private Set<String> getCategories(JSONObject event) throws JSONException {
		
		Set<String> categories = new HashSet<>();
		if (!event.isNull("classifications")) {
			JSONArray classifications = event.getJSONArray("classifications");
			for (int i = 0; i < classifications.length(); ++i) {
				JSONObject classification = classifications.getJSONObject(i);
				if (!classification.isNull("segment")) {
					JSONObject segment = classification.getJSONObject("segment");
					if (!segment.isNull("name")) {
						categories.add(segment.getString("name"));
					}
				}
			}
		}
		return categories;
	}




	/**
	 * Main entry to test TicketMasterClient.
	 */
////	This part is for server test use, comment it in real use senario
//	public static void main(String[] args) {
//		TicketMasterClient client = new TicketMasterClient();
//		List<Item> events = client.search(37.38, -122.08, null);
////		try {
////		    for (int i = 0; i < events.length(); ++i) {
////		       JSONObject event = events.getJSONObject(i);
////		       System.out.println(event.toString(2));
////		    }
////		} catch (Exception e) {
////	                  e.printStackTrace();
////		}	
//		for (Item event : events) {
//			System.out.println(event.toJSONObject());
//		}
//	}

}

