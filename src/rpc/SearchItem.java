package rpc;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import db.DBConnection;
import db.DBConnectionFactory;
import entity.Item;
import external.TicketMasterClient;

/**
 * Servlet implementation class SearchItem
 */
@WebServlet("/search") // The Endpoint of the url
public class SearchItem extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public SearchItem() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		//response.getWriter().append("Served at: ").append(request.getContextPath());
		
//		/////////////////////////////// Step 1: Return html & JSON object //////////////
//		response.setContentType("text/html");
//		PrintWriter writer = response.getWriter();
//		
//		if (request.getParameter("username") != null) {
//			String username = request.getParameter("username");
//			
//			JSONObject obj = new JSONObject();
//			try {
//				obj.put("username", username);
//			} catch (JSONException e) {
//				e.printStackTrace();
//			}
//			writer.print(obj);
//			
////			writer.println("<html><body>");
////			writer.println("<h1>Hello " + username + "</h1>");
////			writer.println("</body></html>");		
//		}	
//		writer.close();
		/////////////////////////////// Step 2: Return a list of Username //////////////
//		response.setContentType("application/json"); // content type
//		PrintWriter writer = response.getWriter(); // complete body
//		
//		JSONArray array = new JSONArray();
//		try {
//			array.put(new JSONObject().put("username", "Jayden"));
//			array.put(new JSONObject().put("username", "Taryn"));
//		} catch (JSONException e) {
//			e.printStackTrace();
//		}
//		RpcHelper.writeJsonArray(response, array);
/////////////////////////////// Step 3: Connect TicketMaster API with SearchItem Servlet //////////////
		
		// allow access only if session exists
		HttpSession session = request.getSession(false);
		if (session == null) {
			response.setStatus(403);
			return;
		}
		// optional
		String userId = session.getAttribute("user_id").toString(); 	 // after login, request from session
				
		double lat = Double.parseDouble(request.getParameter("lat"));
		double lon = Double.parseDouble(request.getParameter("lon"));

//		TicketMasterClient client = new TicketMasterClient();
//		// RpcHelper.writeJsonArray(response, client.search(lat, lon, null));
//		List<Item> items = client.search(lat, lon, null);
//		JSONArray array = new JSONArray();
//		for (Item item : items) {
//			array.put(item.toJSONObject());
//		}
//		RpcHelper.writeJsonArray(response, array);
		
		String term = request.getParameter("term");						// Move the doGet() logic from Servlet to DataBase
		DBConnection connection = DBConnectionFactory.getConnection();
        try {
        	List<Item> items = connection.searchItems(lat, lon, term);
        	Set<String> favoritedItemIds = connection.getFavoriteItemIds(userId);

        	JSONArray array = new JSONArray();
        	for (Item item : items) {
        		//array.put(item.toJSONObject());
        		JSONObject obj = item.toJSONObject();
				obj.put("favorite", favoritedItemIds.contains(item.getItemId()));
				array.put(obj);

        	}
        	RpcHelper.writeJsonArray(response, array);

         } catch (Exception e) {
         	e.printStackTrace();
         } finally {
         	connection.close();
         }	
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// allow access only if session exists
		HttpSession session = request.getSession(false);
		if (session == null) {
			response.setStatus(403);
			return;
		}
		// optional
		String userId = session.getAttribute("user_id").toString(); 
		
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}
