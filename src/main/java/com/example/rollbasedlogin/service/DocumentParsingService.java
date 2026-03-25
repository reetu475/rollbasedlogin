package com.example.rollbasedlogin.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for parsing and extracting content from various document formats
 * including PDF, DOC, DOCX, TXT, and other common resume formats
 */
public class DocumentParsingService {

    private static final java.util.logging.Logger log = java.util.logging.Logger.getLogger(DocumentParsingService.class.getName());
    private HuggingFaceAIService huggingFaceService;
    private EdenAIService edenAIService;
    
    public DocumentParsingService() {
        // Constructor for standalone use
    }
    
    public DocumentParsingService(HuggingFaceAIService huggingFaceService) {
        this.huggingFaceService = huggingFaceService;
    }
    
    public DocumentParsingService(HuggingFaceAIService huggingFaceService, EdenAIService edenAIService) {
        this.huggingFaceService = huggingFaceService;
        this.edenAIService = edenAIService;
    }

    /**
     * Extracts text content from a resume file URL
     * @param resumeUrl URL to the resume file
     * @return Extracted text content or empty string if parsing fails
     */
    public String extractResumeContent(String resumeUrl) {
        if (resumeUrl == null || resumeUrl.trim().isEmpty()) {
            return "";
        }

        try {
            System.out.println("Extracting content from resume: " + resumeUrl);
            
            // Handle different file types
            if (resumeUrl.toLowerCase().endsWith(".pdf")) {
                return extractPdfContent(resumeUrl);
            } else if (resumeUrl.toLowerCase().endsWith(".doc") || 
                      resumeUrl.toLowerCase().endsWith(".docx")) {
                return extractOfficeContent(resumeUrl);
            } else if (resumeUrl.toLowerCase().endsWith(".txt")) {
                return extractTextContent(resumeUrl);
            } else {
                // For other formats, try text extraction
                return extractTextContent(resumeUrl);
            }
        } catch (Exception e) {
            System.err.println("Failed to extract content from resume: " + resumeUrl + " - " + e.getMessage());
            return "";
        }
    }

    /**
     * Extracts text from PDF files using alternative parsing (no Maven required)
     */
    private String extractPdfContent(String pdfUrl) throws IOException {
        try {
            // Try to use PDFBox if available (manual download required)
            Class.forName("org.apache.pdfbox.pdmodel.PDDocument");
            return extractWithPDFBox(pdfUrl);
        } catch (ClassNotFoundException e) {
            // PDFBox not available, use alternative text extraction
            return extractPdfAlternative(pdfUrl);
        } catch (Exception e) {
            System.err.println("Error extracting PDF content: " + e.getMessage());
            return "PDF content extraction failed: " + e.getMessage();
        }
    }

    private String extractWithPDFBox(String pdfUrl) throws IOException {
        // This would use PDFBox if available
        // For now, return a message indicating PDFBox is needed
        return "PDF content extraction requires PDFBox library (manual download needed)";
    }

    private String extractPdfAlternative(String pdfUrl) throws IOException {
        try {
            // Read PDF as text file (limited success but better than nothing)
            java.io.File file = new java.io.File(pdfUrl);
            if (!file.exists()) {
                // Try to find the file in uploads directory
                String fileName = pdfUrl;
                if (pdfUrl.startsWith("resume/")) {
                    fileName = pdfUrl.substring(7);
                }
                
                // Search for file by name in uploads/resumes
                String uploadsDir = System.getProperty("user.dir") + "/uploads/resumes/";
                java.io.File dir = new java.io.File(uploadsDir);
                if (dir.exists() && dir.isDirectory()) {
                    // Make fileName final for lambda usage
                    final String finalFileName = fileName;
                    java.io.File[] files = dir.listFiles((d, name) -> 
                        name.toLowerCase().contains(finalFileName.toLowerCase()) || 
                        finalFileName.toLowerCase().contains(name.toLowerCase()));
                    
                    if (files != null && files.length > 0) {
                        file = files[0]; // Use the first match
                    }
                }
            }
            
            if (!file.exists()) {
                return "PDF file not found: " + pdfUrl;
            }
            
            // Read PDF content as text (will contain PDF structure but some text might be extractable)
            StringBuilder content = new StringBuilder();
            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(file))) {
                String line;
                int linesRead = 0;
                int maxLines = 1000; // Limit lines to prevent memory issues
                
                while ((line = reader.readLine()) != null && linesRead < maxLines) {
                    // Filter out PDF structure and keep readable text
                    if (line.length() > 10 && 
                        !line.startsWith("/") && 
                        !line.startsWith("%") && 
                        !line.startsWith("(") &&
                        !line.startsWith("<") &&
                        !line.startsWith(">") &&
                        !line.startsWith("[") &&
                        !line.startsWith("]")) {
                        content.append(line).append("\n");
                    }
                    linesRead++;
                }
            }
            
            String text = content.toString().trim();
            if (text.isEmpty()) {
                return "PDF appears to be empty or encrypted";
            }
            
            System.out.println("Extracted " + text.length() + " characters from PDF (alternative method)");
            return text;
            
        } catch (Exception e) {
            System.err.println("Failed to extract PDF content: " + e.getMessage());
            return "Failed to extract PDF content: " + e.getMessage();
        }
    }

    /**
     * Extracts text from Office documents (DOC, DOCX) using alternative parsing (no Maven required)
     */
    private String extractOfficeContent(String officeUrl) throws IOException {
        try {
            // Try to use Apache POI if available (manual download required)
            Class.forName("org.apache.poi.hwpf.HWPFDocument");
            return extractWithPOI(officeUrl);
        } catch (ClassNotFoundException e) {
            // POI not available, use alternative text extraction
            return extractOfficeAlternative(officeUrl);
        } catch (Exception e) {
            System.err.println("Error extracting Office document content: " + e.getMessage());
            return "Office document content extraction failed: " + e.getMessage();
        }
    }

    private String extractWithPOI(String officeUrl) throws IOException {
        // This would use Apache POI if available
        // For now, return a message indicating POI is needed
        return "Office document content extraction requires Apache POI library (manual download needed)";
    }

    private String extractOfficeAlternative(String officeUrl) throws IOException {
        try {
            // Read Office document as text file (limited success but better than nothing)
            java.io.File file = new java.io.File(officeUrl);
            if (!file.exists()) {
                // Try to find the file in uploads directory
                String fileName = officeUrl;
                if (officeUrl.startsWith("resume/")) {
                    fileName = officeUrl.substring(7);
                }
                
                // Search for file by name in uploads/resumes
                String uploadsDir = System.getProperty("user.dir") + "/uploads/resumes/";
                java.io.File dir = new java.io.File(uploadsDir);
                if (dir.exists() && dir.isDirectory()) {
                    // Make fileName final for lambda usage
                    final String finalFileName = fileName;
                    java.io.File[] files = dir.listFiles((d, name) -> 
                        name.toLowerCase().contains(finalFileName.toLowerCase()) || 
                        finalFileName.toLowerCase().contains(name.toLowerCase()));
                    
                    if (files != null && files.length > 0) {
                        file = files[0]; // Use the first match
                    }
                }
            }
            
            if (!file.exists()) {
                return "Office document file not found: " + officeUrl;
            }
            
            // Read Office document content as text (will contain document structure but some text might be extractable)
            StringBuilder content = new StringBuilder();
            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(file))) {
                String line;
                int linesRead = 0;
                int maxLines = 1000; // Limit lines to prevent memory issues
                
                while ((line = reader.readLine()) != null && linesRead < maxLines) {
                    // Filter out document structure and keep readable text
                    if (line.length() > 10 && 
                        !line.startsWith("/") && 
                        !line.startsWith("%") && 
                        !line.startsWith("(") &&
                        !line.startsWith("<") &&
                        !line.startsWith(">") &&
                        !line.startsWith("[") &&
                        !line.startsWith("]")) {
                        content.append(line).append("\n");
                    }
                    linesRead++;
                }
            }
            
            String text = content.toString().trim();
            if (text.isEmpty()) {
                return "Office document appears to be empty or encrypted";
            }
            
            System.out.println("Extracted " + text.length() + " characters from Office document (alternative method)");
            return text;
            
        } catch (Exception e) {
            System.err.println("Failed to extract Office document content: " + e.getMessage());
            return "Failed to extract Office document content: " + e.getMessage();
        }
    }

    /**
     * Extracts text from text files
     */
    private String extractTextContent(String textUrl) throws IOException {
        try (InputStream inputStream = openStream(textUrl)) {
            java.util.Scanner scanner = new java.util.Scanner(inputStream, "UTF-8");
            String content = scanner.useDelimiter("\\A").next();
            scanner.close();
            System.out.println("Extracted " + content.length() + " characters from text file");
            return content.trim();
        }
    }

    /**
     * Opens an input stream from a URL (supports both local file paths and HTTP URLs)
     */
    private InputStream openStream(String url) throws IOException {
        if (url.startsWith("http://") || url.startsWith("https://")) {
            try {
                URLConnection connection = new URL(url).openConnection();
                connection.setRequestProperty("User-Agent", "Mozilla/5.0");
                connection.setConnectTimeout(5000); // 5 second timeout
                connection.setReadTimeout(10000);   // 10 second timeout
                return connection.getInputStream();
            } catch (Exception e) {
                System.err.println("Network error accessing URL: " + url + " - " + e.getMessage());
                throw new IOException("Network access failed for URL: " + url, e);
            }
        } else {
            // Handle local file paths - try multiple approaches
            List<String> possiblePaths = new ArrayList<>();
            
            // 1. Try the exact path provided
            possiblePaths.add(url);
            
            // 2. If it's a relative path starting with "resume/", try with uploads/resumes prefix
            if (url.startsWith("resume/")) {
                String fileName = url.substring(7); // Remove "resume/" prefix
                possiblePaths.add("uploads/resumes/" + fileName);
                possiblePaths.add(System.getProperty("user.dir") + "/uploads/resumes/" + fileName);
            }
            
            // 3. If it's a relative path, try with uploads/resumes prefix
            if (!url.startsWith("/") && !url.startsWith("http") && !url.contains(":")) {
                possiblePaths.add("uploads/resumes/" + url);
                possiblePaths.add(System.getProperty("user.dir") + "/uploads/resumes/" + url);
            }
            
            // 4. Try with absolute path from current directory
            possiblePaths.add(System.getProperty("user.dir") + "/" + url);
            
            // 5. Try to find files by name pattern matching in uploads/resumes directory
            String uploadsDir = System.getProperty("user.dir") + "/uploads/resumes/";
            java.io.File dir = new java.io.File(uploadsDir);
            if (dir.exists() && dir.isDirectory()) {
                String fileName = url;
                if (url.startsWith("resume/")) {
                    fileName = url.substring(7);
                }
                
                // Make fileName final for lambda usage
                final String finalFileName = fileName;
                
                // Try to find files that match the name pattern
                java.io.File[] files = dir.listFiles((d, name) -> {
                    // Case insensitive matching
                    return name.toLowerCase().contains(finalFileName.toLowerCase()) || 
                           finalFileName.toLowerCase().contains(name.toLowerCase());
                });
                
                if (files != null && files.length > 0) {
                    // Try the most recent file that matches
                    java.io.File latestFile = files[0];
                    for (java.io.File file : files) {
                        if (file.lastModified() > latestFile.lastModified()) {
                            latestFile = file;
                        }
                    }
                    possiblePaths.add(latestFile.getAbsolutePath());
                }
            }
            
            // Try each possible path
            for (String path : possiblePaths) {
                try {
                    return new URL("file:" + path).openStream();
                } catch (IOException e) {
                    // Continue to next path
                    continue;
                }
            }
            
            // If all paths failed, try as classpath resource
            try {
                return getClass().getClassLoader().getResourceAsStream(url);
            } catch (Exception e) {
                throw new IOException("Could not open stream for any of the paths: " + String.join(", ", possiblePaths), e);
            }
        }
    }

    /**
     * Extracts skills from resume content using pattern matching
     */
    public List<String> extractSkills(String resumeContent) {
        List<String> skills = new ArrayList<>();
        if (resumeContent == null || resumeContent.isEmpty()) {
            return skills;
        }

        // Convert to lowercase for case-insensitive matching
        String content = resumeContent.toLowerCase();

        // Common technical skills patterns
        String[] skillPatterns = {
            "java", "python", "javascript", "typescript", "react", "angular", "vue", 
            "node", "spring", "hibernate", "mysql", "postgresql", "mongodb", "redis",
            "aws", "azure", "gcp", "docker", "kubernetes", "jenkins", "git", "github",
            "gitlab", "jira", "confluence", "linux", "windows", "macos", "html", "css",
            "sass", "less", "bootstrap", "material", "rest", "api", "graphql", "microservices",
            "agile", "scrum", "kanban", "devops", "ci/cd", "tdd", "bdd", "maven", "gradle",
            "npm", "yarn", "webpack", "babel", "eslint", "prettier", "jest", "mocha",
            "cypress", "selenium", "junit", "testng", "postman", "swagger", "docker",
            "kubernetes", "helm", "terraform", "ansible", "puppet", "chef", "sonarqube",
            "nexus", "artifactory", "rabbitmq", "kafka", "redis", "elasticsearch",
            "mongodb", "cassandra", "hadoop", "spark", "hive", "pig", "hbase", "zookeeper",
            "flume", "sqoop", "oozie", "mahout", "pig", "storm", "kafka", "spark"
        };

        for (String skill : skillPatterns) {
            if (content.contains(skill)) {
                skills.add(skill);
            }
        }

        // Remove duplicates and return
        return skills.stream().distinct().collect(java.util.stream.Collectors.toList());
    }

    /**
     * Extracts CGPA from resume content using regex patterns
     */
    public String extractCGPA(String resumeContent) {
        if (resumeContent == null || resumeContent.isEmpty()) {
            return "N/A";
        }

        // Common CGPA patterns
        String[] patterns = {
            "cgpa[\\s:]*([0-9]+\\.?[0-9]*)",
            "gpa[\\s:]*([0-9]+\\.?[0-9]*)",
            "cumulative[\\s:]*([0-9]+\\.?[0-9]*)",
            "cgpi[\\s:]*([0-9]+\\.?[0-9]*)",
            "([0-9]+\\.?[0-9]*)/10",
            "([0-9]+\\.?[0-9]*)/4",
            "([0-9]+\\.?[0-9]*)/10\\.0",
            "([0-9]+\\.?[0-9]*)/4\\.0"
        };

        for (String pattern : patterns) {
            Pattern p = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(resumeContent);
            if (m.find()) {
                String cgpa = m.group(1);
                try {
                    double value = Double.parseDouble(cgpa);
                    if (value >= 0 && value <= 10) {
                        return String.format("%.2f", value);
                    } else if (value >= 0 && value <= 4) {
                        // Convert 4-point scale to 10-point scale
                        return String.format("%.2f", value * 2.5);
                    }
                } catch (NumberFormatException e) {
                    // Continue to next pattern
                }
            }
        }

        return "N/A";
    }

    /**
     * Extracts education information from resume content
     */
    public String extractEducation(String resumeContent) {
        if (resumeContent == null || resumeContent.isEmpty()) {
            return "N/A";
        }

        String content = resumeContent.toLowerCase();

        // Common education patterns
        String[] educationPatterns = {
            "b\\.?tech", "btech", "b\\.?e\\.?", "be", "m\\.?tech", "mtech", "m\\.?e\\.?", "me",
            "b\\.?sc", "bsc", "m\\.?sc", "msc", "b\\.?a", "ba", "m\\.?a", "ma", "ph\\.?d", "phd",
            "diploma", "certificate", "mba", "bba", "bca", "mca", "b\\.?com", "bcom", "m\\.?com", "mcom"
        };

        for (String pattern : educationPatterns) {
            if (content.contains(pattern)) {
                return pattern.toUpperCase().replace(".", "");
            }
        }

        return "N/A";
    }

    /**
     * Extracts experience information from resume content
     */
    public String extractExperience(String resumeContent) {
        if (resumeContent == null || resumeContent.isEmpty()) {
            return "N/A";
        }

        // Pattern to match years of experience
        Pattern pattern = Pattern.compile("(\\d+)\\s*(years?|yrs?)\\s*experience", Pattern.CASE_INSENSITIVE);
        Matcher m = pattern.matcher(resumeContent);
        if (m.find()) {
            return m.group(1) + " years";
        }

        // Pattern to match "fresher" or "entry level"
        String lowerContent = resumeContent.toLowerCase();
        if (lowerContent.contains("fresher") || 
            lowerContent.contains("entry level") ||
            lowerContent.contains("0 years")) {
            return "0 years (Fresher)";
        }

        return "N/A";
    }

    /**
     * Analyzes resume content and returns a comprehensive analysis using Hugging Face AI if available
     */
    public ResumeAnalysis analyzeResume(String resumeUrl) {
        ResumeAnalysis analysis = new ResumeAnalysis();
        
        try {
            String content = extractResumeContent(resumeUrl);
            if (content.isEmpty()) {
                analysis.setValid(false);
                analysis.setError("Could not extract content from resume");
                return analysis;
            }

            analysis.setValid(true);
            analysis.setResumeContent(content);
            analysis.setExtractedLength(content.length());

            // Try to use Eden AI for enhanced analysis if service is available
            if (edenAIService != null) {
                System.out.println("=== USING EDEN AI FOR ENHANCED ANALYSIS ===");
                try {
                    EdenAIService.ResumeAnalysis aiAnalysis = edenAIService.analyzeResume(content);
                    if (aiAnalysis != null) {
                        // Use AI analysis if it provides better results than regex
                        if (!aiAnalysis.getCgpa().equals("N/A") || !aiAnalysis.getSkills().equals("N/A") || 
                            !aiAnalysis.getEducation().equals("N/A") || !aiAnalysis.getExperience().equals("N/A")) {
                            System.out.println("=== EDEN AI ANALYSIS SUCCESSFUL ===");
                            analysis.setCgpa(aiAnalysis.getCgpa());
                            analysis.setSkills(java.util.Arrays.asList(aiAnalysis.getSkills().split(",\\s*")));
                            analysis.setEducation(aiAnalysis.getEducation());
                            analysis.setExperience(aiAnalysis.getExperience());
                            return analysis;
                        } else {
                            System.out.println("=== EDEN AI RETURNED N/A VALUES, FALLING BACK TO REGEX ===");
                        }
                    } else {
                        System.out.println("=== EDEN AI RETURNED NULL, FALLING BACK TO REGEX ===");
                    }
                } catch (Exception e) {
                    System.err.println("=== EDEN AI FAILED, FALLING BACK TO REGEX ===");
                    System.err.println("AI Error: " + e.getMessage());
                }
            } else if (huggingFaceService != null) {
                System.out.println("=== USING HUGGING FACE AI FOR ENHANCED ANALYSIS ===");
                try {
                    HuggingFaceAIService.ResumeAnalysis aiAnalysis = huggingFaceService.analyzeResume(content);
                    if (aiAnalysis != null) {
                        // Use AI analysis if it provides better results than regex
                        if (!aiAnalysis.getCgpa().equals("N/A") || !aiAnalysis.getSkills().equals("N/A") || 
                            !aiAnalysis.getEducation().equals("N/A") || !aiAnalysis.getExperience().equals("N/A")) {
                            System.out.println("=== HUGGING FACE AI ANALYSIS SUCCESSFUL ===");
                            analysis.setCgpa(aiAnalysis.getCgpa());
                            analysis.setSkills(java.util.Arrays.asList(aiAnalysis.getSkills().split(",\\s*")));
                            analysis.setEducation(aiAnalysis.getEducation());
                            analysis.setExperience(aiAnalysis.getExperience());
                            return analysis;
                        } else {
                            System.out.println("=== HUGGING FACE AI RETURNED N/A VALUES, FALLING BACK TO REGEX ===");
                        }
                    } else {
                        System.out.println("=== HUGGING FACE AI RETURNED NULL, FALLING BACK TO REGEX ===");
                    }
                } catch (Exception e) {
                    System.err.println("=== HUGGING FACE AI FAILED, FALLING BACK TO REGEX ===");
                    System.err.println("AI Error: " + e.getMessage());
                }
            } else {
                System.out.println("=== NO AI SERVICES AVAILABLE, USING REGEX ONLY ===");
            }

            // Fallback to regex-based analysis
            System.out.println("=== USING REGEX-BASED ANALYSIS ===");
            analysis.setSkills(extractSkills(content));
            analysis.setCgpa(extractCGPA(content));
            analysis.setEducation(extractEducation(content));
            analysis.setExperience(extractExperience(content));

        } catch (Exception e) {
            System.err.println("Error analyzing resume: " + resumeUrl + " - " + e.getMessage());
            analysis.setValid(false);
            analysis.setError("Error during analysis: " + e.getMessage());
        }

        return analysis;
    }

    /**
     * Data class to hold resume analysis results
     */
    public static class ResumeAnalysis {
        private boolean valid;
        private String error;
        private String resumeContent;
        private List<String> skills;
        private String cgpa;
        private String education;
        private String experience;
        private int extractedLength;

        // Getters and setters
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        
        public String getResumeContent() { return resumeContent; }
        public void setResumeContent(String resumeContent) { this.resumeContent = resumeContent; }
        
        public List<String> getSkills() { return skills; }
        public void setSkills(List<String> skills) { this.skills = skills; }
        
        public String getCgpa() { return cgpa; }
        public void setCgpa(String cgpa) { this.cgpa = cgpa; }
        
        public String getEducation() { return education; }
        public void setEducation(String education) { this.education = education; }
        
        public String getExperience() { return experience; }
        public void setExperience(String experience) { this.experience = experience; }
        
        public int getExtractedLength() { return extractedLength; }
        public void setExtractedLength(int extractedLength) { this.extractedLength = extractedLength; }
    }
}