import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;

public class EduSmartServer {
    private static final int PORT = 8080;

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        
        // Routes
        server.createContext("/", new FileHandler());
        server.createContext("/upload", new UploadHandler());
        server.createContext("/simplify", new SimplifyHandler());
        server.createContext("/quiz", new QuizHandler());
        
        server.setExecutor(null);
        server.start();
        
        System.out.println("EduSmart Server started at http://localhost:" + PORT);
        System.out.println("Open your browser and visit: http://localhost:8080");
    }

    static class FileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) path = "/index.html";
            
            File file = new File("web" + path);
            
            if (file.exists() && !file.isDirectory()) {
                String contentType = getContentType(path);
                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.sendResponseHeaders(200, file.length());
                
                OutputStream os = exchange.getResponseBody();
                Files.copy(file.toPath(), os);
                os.close();
            } else {
                String response = "404 - File Not Found";
                exchange.sendResponseHeaders(404, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        }
        
        private String getContentType(String path) {
            if (path.endsWith(".html")) return "text/html";
            if (path.endsWith(".css")) return "text/css";
            if (path.endsWith(".js")) return "application/javascript";
            return "text/plain";
        }
    }

    // Handle PDF upload
    static class UploadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                InputStream is = exchange.getRequestBody();
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                
                byte[] data = new byte[1024];
                int bytesRead;
                while ((bytesRead = is.read(data)) != -1) {
                    buffer.write(data, 0, bytesRead);
                }
                
                String filename = "uploads/temp_" + System.currentTimeMillis() + ".pdf";
                Files.write(Paths.get(filename), buffer.toByteArray());

                String extractedText = PDFProcessor.extractText(filename);

                String jsonResponse = "{\"text\": " + escapeJson(extractedText) + "}";
                
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, jsonResponse.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(jsonResponse.getBytes());
                os.close();

                new File(filename).delete();
            }
        }
    }

    // Simplify notes
    static class SimplifyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                String text = new String(exchange.getRequestBody().readAllBytes());

                String simplified = NoteSimplifier.simplify(text);

                String jsonResponse = "{\"simplified\": " + escapeJson(simplified) + "}";
                
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, jsonResponse.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(jsonResponse.getBytes());
                os.close();
            }
        }
    }

    // Generate quiz
    static class QuizHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {

                String text = new String(exchange.getRequestBody().readAllBytes());

                String quiz = QuizGenerator.generate(text);

                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, quiz.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(quiz.getBytes());
                os.close();
            }
        }
    }

    private static String escapeJson(String text) {
        return "\"" + text.replace("\\", "\\\\")
                         .replace("\"", "\\\"")
                         .replace("\n", "\\n")
                         .replace("\r", "\\r")
                         .replace("\t", "\\t") + "\"";
    }
}