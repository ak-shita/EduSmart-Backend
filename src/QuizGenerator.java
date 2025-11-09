import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonArray;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class QuizGenerator {

    private static final  String API_KEY = "AIzaSyAiyMk4TrbXzFApn1o0uapgoxE2TzOHu1E";
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";
    
    public static String generate(String text) {
        text = text.trim();
        
        if (text.isEmpty()) {
            return "{\"questions\": []}";
        }

        
        try {
            System.out.println("Calling Gemini API for quiz generation...");

            String prompt = createQuizPrompt(text);

            String response = callGeminiAPI(prompt);

            String validatedJson = extractAndValidateJSON(response);
            
            System.out.println("Gemini API call successful! Quiz generated.");
            return validatedJson;
            
        } catch (Exception e) {
            System.err.println("ERROR calling Gemini API: " + e.getMessage());
            e.printStackTrace();
            return createErrorQuiz();
        }
    }
    
    private static String createQuizPrompt(String text) {
        if (text.length() > 15000) {
            text = text.substring(0, 15000) + "...";
        }
        
        return "You are an expert educator creating a quiz to test student understanding.\n\n" +
               
               "TASK: Create exactly 8 multiple-choice questions based on the content below.\n\n" +
               
               "REQUIREMENTS FOR EACH QUESTION:\n" +
               "1. Test actual understanding of the content (not trivial facts)\n" +
               "2. Have 4 options (only ONE is correct)\n" +
               "3. Wrong options should be plausible but clearly incorrect\n" +
               "4. Questions should cover different parts of the content\n" +
               "5. Mix of difficulty levels (some easy, some challenging)\n" +
               "6. Clear, unambiguous wording\n\n" +
               
               "CRITICAL: You MUST respond with ONLY valid JSON in this EXACT format:\n\n" +
               "{\n" +
               "  \"questions\": [\n" +
               "    {\n" +
               "      \"question\": \"Your question here?\",\n" +
               "      \"options\": [\"Option A\", \"Option B\", \"Option C\", \"Option D\"],\n" +
               "      \"correctAnswer\": 0\n" +
               "    },\n" +
               "    ... 7 more questions ...\n" +
               "  ]\n" +
               "}\n\n" +
               
               "Where correctAnswer is the index (0, 1, 2, or 3) of the correct option.\n" +
               "Do NOT include any text before or after the JSON.\n" +
               "Do NOT use markdown code blocks.\n" +
               "Just pure JSON.\n\n" +
               
               "CONTENT TO CREATE QUIZ FROM:\n\n" + text;
    }
    
    private static String callGeminiAPI(String prompt) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

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

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(urlWithKey))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
            .build();

        HttpResponse<String> response = client.send(request, 
            HttpResponse.BodyHandlers.ofString());

        System.out.println("API Response Status: " + response.statusCode());
        
        if (response.statusCode() != 200) {
            System.err.println("API Response Body: " + response.body());
            throw new Exception("API returned status code: " + response.statusCode());
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
    
    private static String extractAndValidateJSON(String response) {
        response = response.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();

        int startIndex = response.indexOf("{");
        int endIndex = response.lastIndexOf("}");
        
        if (startIndex != -1 && endIndex != -1) {
            response = response.substring(startIndex, endIndex + 1);
        }
        
        try {
            JsonObject quiz = JsonParser.parseString(response).getAsJsonObject();
            
            if (!quiz.has("questions")) {
                throw new Exception("Missing 'questions' field");
            }
            
            JsonArray questions = quiz.getAsJsonArray("questions");
            for (int i = 0; i < questions.size(); i++) {
                JsonObject q = questions.get(i).getAsJsonObject();
                
                if (!q.has("question") || !q.has("options") || !q.has("correctAnswer")) {
                    throw new Exception("Invalid question structure at index " + i);
                }
                
                JsonArray options = q.getAsJsonArray("options");
                if (options.size() != 4) {
                    throw new Exception("Question " + i + " must have exactly 4 options");
                }
                
                int correctAnswer = q.get("correctAnswer").getAsInt();
                if (correctAnswer < 0 || correctAnswer > 3) {
                    throw new Exception("Invalid correctAnswer at question " + i);
                }
            }
            
            return response;
            
        } catch (Exception e) {
            System.err.println("JSON validation error: " + e.getMessage());
            System.err.println("Response was: " + response);
            return createErrorQuiz();
        }
    }
    
    private static String createErrorQuiz() {
        JsonObject result = new JsonObject();
        JsonArray questions = new JsonArray();
        
        JsonObject q1 = new JsonObject();
        q1.addProperty("question", "The quiz generator requires AI API configuration. What should you do?");
        JsonArray options1 = new JsonArray();
        options1.add("Get a FREE Gemini API key from https://aistudio.google.com/app/apikey");
        options1.add("Give up on this feature");
        options1.add("Use a different app");
        options1.add("Hope it fixes itself");
        q1.add("options", options1);
        q1.addProperty("correctAnswer", 0);
        questions.add(q1);
        
        JsonObject q2 = new JsonObject();
        q2.addProperty("question", "After getting the API key, what's the next step?");
        JsonArray options2 = new JsonArray();
        options2.add("Paste it in QuizGenerator.java where it says YOUR_GEMINI_API_KEY_HERE");
        options2.add("Email it to someone");
        options2.add("Post it on social media");
        options2.add("Keep it secret and never use it");
        q2.add("options", options2);
        q2.addProperty("correctAnswer", 0);
        questions.add(q2);
        
        result.add("questions", questions);
        
        Gson gson = new Gson();
        return gson.toJson(result);
    }
}