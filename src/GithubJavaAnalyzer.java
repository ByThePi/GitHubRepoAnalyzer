package githubrepoanalyzer;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Scanner;
import java.io.BufferedReader;

/**
*
* @author Enes Soylu enes.soylu3@ogr.sakarya.edu
* @since 01.04.2024
* <p>
* Kullanıcıdan istenilen repoyu klonlayıp
* Analize tabi tutan sınıf
* 
* Ek not: Girilen URL' bağlı repo program vasıtasıyla
* Lokale klonlanır ve program sonlandıktan sonra repo silinmez
* Klonlama işleminin gerçekleştiğini göstermek adına
* Program sonlandıktan sonra silme özelliği eklenmemiştir
* Arkada kalıntı oluşmaması adına
* İlgili dosyanın silinmesi tavsiye edilir
* </p>
*/

public class GithubJavaAnalyzer {

    public static void main(String[] args) throws IOException, InterruptedException {
        // Kullanıcıdan Github deposu URL'si alınır
    	Scanner scanner = new Scanner(System.in);  // Scanner nesnesi oluşturulur
    	
        System.out.println("Github deposu URL'sini giriniz: ");
        String url = scanner.nextLine();
        
        String repoName = url.substring(url.lastIndexOf("/") + 1, url.lastIndexOf(".git"));
        
        String relativePath = repoName;
        String absolutePath = Paths.get(System.getProperty("user.dir"), relativePath).toString();
        
        // Repo klonlanir
        System.out.println("Repo klonlaniyor...");
        try {
        	cloneRepo(url, absolutePath);
        } catch (IOException e) {
        	e.printStackTrace();
        	return; // Klonlama işlemi hata verirse programdan çıkılır
        }
        
        // Klonlanan repo içerisindeki tüm dosyalar kontrol edilir
        File dir = new File(absolutePath);
        if (dir.exists()) {
        	analyzeJavaFilesRecursively(dir);
        } else { // Dosya bulunamazsa hata kodu verir
            System.out.println("Dosya veya dizin bulunamadı!");
        }
    }

    private static void analyzeJavaFilesRecursively(File file) throws IOException {
    	// Öz yinelemeli olarak tüm dosyaları incelemeye yarayan metod
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    analyzeJavaFilesRecursively(f);
                }
            }
        } else if (file.getName().endsWith(".java")) {
            if (!containsInterfaceEnum(file)) {
            	analyzeJavaFile(file);
            }
        }
    }

    private static boolean containsInterfaceEnum(File file) throws IOException {
    	// Dosyanın interface veya enum olup olmadığını kontrol eden metod
        try (BufferedReader reader = Files.newBufferedReader(file.toPath())) {
            String line;
            while ((line = reader.readLine()) != null) {
            	String trimmedLine = line.trim();
                if (trimmedLine.startsWith("public interface") || 
                        trimmedLine.startsWith("private interface") ||
                        trimmedLine.startsWith("protected interface") ||
                        trimmedLine.startsWith("interface") ||
                        trimmedLine.startsWith("public enum") ||
                        trimmedLine.startsWith("private enum") ||
                        trimmedLine.startsWith("protected enum") ||
                        trimmedLine.startsWith("enum")) {
                    return true;
                }
            }
        }
        return false;
    }
    
    public static void analyzeJavaFile(File file) throws IOException {
        // Javadoc, yorum ve kod satırları sayılır
    	String regexStart = "/\\*(?!\\*)"; // Çoklu yorum için regex başlangıç
    	String regexJavadocStart = "/\\*\\*(?!\\*)"; // Javadoc için regex başlangıç
    	String regexEnd = "\\*/"; // Çoklu yorum için regex bitiş
    	
    	Pattern patternStart = Pattern.compile(regexStart);
    	Pattern patternJavadocStart = Pattern.compile(regexJavadocStart);
    	Pattern patternEnd = Pattern.compile(regexEnd);
    	
    	Pattern patternComment = Pattern.compile("//");
    	
    	boolean inJavadoc = false, inComment = false;
    	int methodCount = countMethods(file);
    	int commentCodeCombined = 0;
    	int javadocExtra = 0, commentExtra = 0;
    	int javadocCount = 0, commentCount = 0, codeCount = 0, emptyCount = 0, totalCount = 0;
        try (var reader = Files.newBufferedReader(Paths.get(file.getPath()))) {
            String line;
            while ((line = reader.readLine()) != null) {
            	totalCount++;
                
            	// Javadoc sayımı başlar
            	if (line.trim().startsWith("/**")) {
                    inJavadoc = true;
                    javadocCount--;
                    javadocExtra += 2;
            	}// Javadoc sayılır
                if (inJavadoc) {
                	javadocCount++;
                } // Javadoc sayımı biter
            	if (line.trim().endsWith("*/") && inJavadoc) {
            		inJavadoc = false;
            		javadocCount--;
            	}

            	Matcher matcherStart = patternStart.matcher(line);
            	Matcher matcherJavadocStart = patternJavadocStart.matcher(line);
                Matcher matcherEnd = patternEnd.matcher(line);
                // Çoklu yorum satırları sayımı başlar
                if (matcherStart.find() && !matcherJavadocStart.find()) {
                	inComment = true;
                	commentCount--;
                    commentExtra += 2;
                    }// Çoklu yorum satırları sayılır
                if (inComment) {
                    commentCount++;
                    }// Çoklu yorum satırları sayımı biter
                if (matcherEnd.find() && inComment) {
                	inComment = false;
                	commentCount--;
                    }
                
                Matcher matcherComment = patternComment.matcher(line);
                // Tekli yorum satırları sayılır
                if (matcherComment.find() && !inComment) {
                	commentCount++;
                	String beforeComment = line.substring(0, matcherComment.start()).trim();
                	if (!beforeComment.isEmpty()) {
                		commentCodeCombined++;
                	}
                }
                //Boşluk sayımı yapan metod
                emptyCount = countEmptyLines(line, inComment, emptyCount);
            }
            
            // Kod satır sayısı hesaplanır
            codeCount = (totalCount - emptyCount - javadocCount - javadocExtra - commentCount - commentExtra + commentCodeCombined);
        
            // Yorum sapma yüzdesi hesaplanır
            double YG = (((double) javadocCount + (double) commentCount) * 0.8) / (double) methodCount;
            double YH = ((double) codeCount / (double) methodCount) * 0.3;
            double YSY = ((100 * YG) / YH) - 100;
        

            // Hesaplanan bilgiler yazdırılır
            System.out.println("Sınıf: " + file.getName());
            System.out.println("Javadoc Satır Sayısı: " + javadocCount);
            System.out.println("Yorum Satır Sayısı: " + commentCount);
            System.out.println("Kod Satır Sayısı: " + codeCount);
            System.out.println("LOC: " + totalCount);
            System.out.println("Fonksiyon Sayısı: " + methodCount);
            System.out.println("Yorum Sapma Yüzdesi: " + String.format("%.2f", YSY) + "%");
            System.out.println("-----------------------------------------");
            }
        }
    
    private static int countEmptyLines(String line, boolean inComment, int emptyCount) {
    	// Boş satırlar kontrol edilir
    	// Boşluklar, "" ile değiştirilir
        if (line.replaceAll("\\s", "").isEmpty() && !inComment) {
            emptyCount++;
        }
        return emptyCount;
    }
    
    private static int countMethods(File file) throws IOException {
        int methodCount = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // Metod mu kontrol edilir
                if (isMethodDeclaration(line)) {
                    methodCount++;
                }
            }
        }
        return methodCount;
    }

    private static boolean isMethodDeclaration(String line) {
        // Sınıfların, değişkenlerin ve diğer yapıların metod olarak değerlendirmemesine yarar
        return line.matches("^\\b(public|private|protected|static|final|synchronized|abstract|native)\\b.*\\(.*\\)\\s*\\{?\\s*") &&
               !line.contains("=") && // "=" içeriyorsa, değişken olma ihtimaline karşı
               !line.contains("class ") && // "class " içeriyorsa, içeriyorsa muhtemelen 
               !line.contains("interface ") && // "interface " içeriyorsa, arayüz olma ihtimaline karşı
               !line.contains(" enum ") && // " enum " içeriyorsa, enum olma ihtimaline karşı
               !line.contains("extends") && // "extends" içeriyorsa, sınıf kalıtımları olma ihtimaline karşı
               !line.contains("implements") && // "implements" içeriyorsa, arayüz olma ihtimaline karşı
               !line.endsWith(";"); // ";" içeriyorsa, diğer yapılardan biri olma ihtimaline karşı
    }

    public static void cloneRepo(String repoUrl, String destinationPath) throws IOException {

    	
        // Dosya var mı kontrol edilir
        File dir = new File(destinationPath);
        if (dir.exists()) {
            System.out.println("Dosya mevcut!");
            return;
        }
        // Klonlama işlemi başlatılır
        try {
            Git.gitClone(Paths.get(destinationPath), repoUrl);
            System.out.println("Klonlama işlemi tamamlandı!");
        } catch (IOException | InterruptedException e) {
            System.out.println("Klonlama işlemi başarısız!");
            e.printStackTrace();
        }
    }
}