import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonArray;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class NoteSimplifier {

    private static final String API_KEY = "AIzaSyAiyMk4TrbXzFApn1o0uapgoxE2TzOHu1E";
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";
    
    public static String simplify(String rawText) {
        rawText = rawText.trim();
        
        if (rawText.isEmpty()) {
            return "No text provided to simplify.";
        }
        
        
        
        try {
            System.out.println("Calling Gemini API for note simplification...");

            String prompt = createSmartPrompt(rawText);

            String response = callGeminiAPI(prompt);
            
            System.out.println("Gemini API call successful!");
            return response;
            
        } catch (Exception e) {
            System.err.println("ERROR calling Gemini API: " + e.getMessage());
            e.printStackTrace();
            return "ERROR: Failed to simplify notes using AI.\n\n" +
                   "Error message: " + e.getMessage() + "\n\n" +
                   "Please check:\n" +
                   "1. Your API key is correct\n" +
                   "2. You have internet connection\n" +
                   "3. Gemini API is accessible in your region";
        }
    }
    
    private static String createSmartPrompt(String text) {

        if (text.length() > 15000) {
            text = text.substring(0, 15000) + "...";
        }
        
        return "You are an expert educator helping students learn. Your task is to simplify this educational content into easy-to-understand notes.\n\n" +
               
               "IMPORTANT INSTRUCTIONS:\n" +
               "- Write in simple, clear language that a student can easily understand\n" +
               "- Break down complex concepts into simple explanations\n" +
               "- Use examples and analogies where helpful\n" +
               "- Keep the original order and flow of topics\n" +
               "- Be comprehensive - don't skip important information\n" +
               "- Only include sections that are actually present in the content\n" +
               "- If there are no definitions/examples/terms in the content, skip those sections entirely\n\n" +
               
               "FORMAT YOUR RESPONSE LIKE THIS:\n\n" +
               
               "📖 INTRODUCTION\n" +
               "[Write 2-3 sentences introducing what this content is about]\n\n" +
               
               "📚 MAIN CONTENT\n" +
               "[Break the content into logical sections. For each major topic/concept:\n" +
               "- Give it a clear heading\n" +
               "- Explain it in simple terms\n" +
               "- Use bullet points for lists\n" +
               "- Add examples if helpful]\n\n" +
               
               "🔑 KEY TAKEAWAYS\n" +
               "[List the 5-8 most important points students should remember]\n\n" +
               
               "📝 SUMMARY\n" +
               "[Write a comprehensive 3-4 sentence summary of everything covered]\n\n" +
               
               "NOW SIMPLIFY THIS CONTENT:\n\n" + text;
    }
    
    private static String callGeminiAPI(String prompt) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        
        //JSON payload for Gemini
        JsonObject content = new JsonObject();
        JsonArray contents = new JsonArray();
        JsonObject message = new JsonObject();
        JsonArray parts = new JsonArray();
        JsonObject part = new JsonObject();
        
        part.addProperty("text", prompt);
        parts.add(part);
        message.add("parts", parts);
        contents.add(message);
        content.add("contents", contents);
        
        String jsonPayload = content.toString();

        String urlWithKey = API_URL + "?key=" + API_KEY;
        
        //HTTP request
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(urlWithKey))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
            .build();
        
        // Send request
        HttpResponse<String> response = client.send(request, 
            HttpResponse.BodyHandlers.ofString());

        System.out.println("API Response Status: " + response.statusCode());
        
        if (response.statusCode() != 200) {
            System.err.println("API Response Body: " + response.body());
            throw new Exception("API returned status code: " + response.statusCode() + ". Check your API key and quota.");
        }

        JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
        
        if (jsonResponse.has("candidates")) {
            JsonArray candidates = jsonResponse.getAsJsonArray("candidates");
            if (candidates.size() > 0) {
                JsonObject candidate = candidates.get(0).getAsJsonObject();
                JsonObject contentObj = candidate.getAsJsonObject("content");
                JsonArray partsArray = contentObj.getAsJsonArray("parts");
                if (partsArray.size() > 0) {
                    return partsArray.get(0).getAsJsonObject().get("text").getAsString();
                }
            }
        }
        
        if (jsonResponse.has("error")) {
            JsonObject error = jsonResponse.getAsJsonObject("error");
            String errorMsg = error.get("message").getAsString();
            throw new Exception("Gemini API Error: " + errorMsg);
        }
        
        throw new Exception("Unexpected API response format");
    }
}