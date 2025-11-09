import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonArray;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class TestAPI {

    private static final String API_KEY = "AIzaSyAiyMk4TrbXzFApn1o0uapgoxE2TzOHu1E";
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";
    
    public static void main(String[] args) {
        System.out.println("TESTING GEMINI API CONNECTION");

        if (API_KEY.equals("AIzaSyAiyMk4TrbXzFApn1o0uapgoxE2TzOHu1E") || API_KEY.isEmpty()) {
            System.err.println("ERROR: API key not configured!");
            System.err.println("\nSteps to fix:");
            System.err.println("1. Go to: https://aistudio.google.com/app/apikey");
            System.err.println("2. Click 'Create API key'");
            System.err.println("3. Copy your key (starts with AIzaSy...)");
            System.err.println("4. Paste it in this file where it says YOUR_GEMINI_API_KEY_HERE");
            System.err.println("5. Compile: javac -cp \"lib/*\" TestAPI.java");
            System.err.println("6. Run: java -cp \".;lib/*\" TestAPI");
            return;
        }
        
        System.out.println("✓ API Key found: " + API_KEY.substring(0, 10) + "...");
        System.out.println("✓ API URL: " + API_URL);
        System.out.println("\nSending test request to Gemini...\n");
        
        try {
            String testPrompt = "Say 'Hello! The API is working correctly.' in a friendly way.";

            JsonObject content = new JsonObject();
            JsonArray contents = new JsonArray();
            JsonObject message = new JsonObject();
            JsonArray parts = new JsonArray();
            JsonObject part = new JsonObject();
            
            part.addProperty("text", testPrompt);
            parts.add(part);
            message.add("parts", parts);
            contents.add(message);
            content.add("contents", contents);
            
            String jsonPayload = content.toString();
            String urlWithKey = API_URL + "?key=" + API_KEY;

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(urlWithKey))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();
            
            // Send request
            HttpResponse<String> response = client.send(request, 
                HttpResponse.BodyHandlers.ofString());
            
            System.out.println("Response Status Code: " + response.statusCode());
            
            if (response.statusCode() == 200) {

                JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
                
                if (jsonResponse.has("candidates")) {
                    JsonArray candidates = jsonResponse.getAsJsonArray("candidates");
                    if (candidates.size() > 0) {
                        JsonObject candidate = candidates.get(0).getAsJsonObject();
                        JsonObject contentObj = candidate.getAsJsonObject("content");
                        JsonArray partsArray = contentObj.getAsJsonArray("parts");
                        if (partsArray.size() > 0) {
                            String aiResponse = partsArray.get(0).getAsJsonObject().get("text").getAsString();
                            
                            System.out.println("\n" + "=".repeat(50));
                            System.out.println("SUCCESS! API IS WORKING!");
                            System.out.println("=".repeat(50));
                            System.out.println("\nGemini's Response:");
                            System.out.println(aiResponse);
                            System.out.println("\n" + "=".repeat(50));
                            System.out.println("✓ Your API is configured correctly!");
                            System.out.println("✓ You can now use it in your EduSmart app!");
                            System.out.println("=".repeat(50));
                            return;
                        }
                    }
                }
                
                if (jsonResponse.has("error")) {
                    JsonObject error = jsonResponse.getAsJsonObject("error");
                    String errorMsg = error.get("message").getAsString();
                    System.err.println("\nAPI ERROR: " + errorMsg);
                    System.err.println("\nPossible issues:");
                    System.err.println("- Your API key might be invalid");
                    System.err.println("- API might be restricted in your region");
                    System.err.println("- You might have exceeded quota");
                    return;
                }
                
            } else {
                System.err.println("\nHTTP ERROR: Status " + response.statusCode());
                System.err.println("Response: " + response.body());
                System.err.println("\nPossible issues:");
                System.err.println("- Wrong API key format");
                System.err.println("- API key not activated");
                System.err.println("- Network/firewall issues");
            }
            
        } catch (Exception e) {
            System.err.println("\nEXCEPTION: " + e.getMessage());
            e.printStackTrace();
            System.err.println("\nPossible issues:");
            System.err.println("- No internet connection");
            System.err.println("- Firewall blocking requests");
            System.err.println("- Invalid API URL");
        }
    }
}