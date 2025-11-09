import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import java.io.File;
import java.io.IOException;

public class PDFProcessor {
    
    public static String extractText(String pdfPath) {
        try {
            //Load PDF
            File pdfFile = new File(pdfPath);
            PDDocument document = Loader.loadPDF(pdfFile);

            PDFTextStripper stripper = new PDFTextStripper();
            
            // Extract
            String text = stripper.getText(document);
            document.close();
            text = text.replaceAll("\\s+", " ").trim();
            
            return text;
            
        } catch (IOException e) {
            System.err.println("Error extracting PDF: " + e.getMessage());
            e.printStackTrace();
            return "Error: Could not extract text from PDF. Please ensure it's a valid PDF file.";
        }
    }
}