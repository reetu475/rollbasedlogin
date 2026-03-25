package com.example.rollbasedlogin.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.rollbasedlogin.model.Application;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class EdenAIService {

    @Value("${edenai.api.key}")
    private String apiKey;

    @Value("${edenai.api.url}")
    private String apiUrl;

    @Value("${edenai.similarity.url}")
    private String similarityUrl;

    @Value("${edenai.extraction.url}")
    private String extractionUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Analyze resume content using Eden AI for comprehensive parsing and ranking
     */
    public ResumeAnalysis analyzeResume(String resumeContent) {
        try {
            ResumeAnalysis analysis = new ResumeAnalysis();

            // Use Eden AI for comprehensive resume analysis
            ResumeAnalysis edenAnalysis = analyzeWithEdenAI(resumeContent);
            if (edenAnalysis != null) {
                return edenAnalysis;
            }

            // If Eden AI fails, return empty analysis
            return new ResumeAnalysis();
        } catch (Exception e) {
            // Return empty analysis if parsing fails
            return new ResumeAnalysis();
        }
    }

    /**
     * Use Eden AI for comprehensive resume analysis and job matching
     */
    public RankedApplication analyzeResumeForJob(String resumeContent, String jobDescription, String customKeywords) {
        try {
            RankedApplication rankedApp = new RankedApplication();

            // Extract resume information using Eden AI
            ResumeAnalysis analysis = analyzeWithEdenAI(resumeContent);
            if (analysis != null) {
                rankedApp.setCgpa(analysis.getCgpa());
                rankedApp.setSkills(analysis.getSkills());
                rankedApp.setEducation(analysis.getEducation());
            }

            // If Eden AI failed to extract information, return empty analysis
            if (rankedApp.getCgpa().equals("N/A") || rankedApp.getSkills().equals("N/A")
                    || rankedApp.getEducation().equals("N/A")) {
                RankedApplication emptyApp = new RankedApplication();
                emptyApp.setScore(0.0);
                emptyApp.setScorePercentage("0.0%");
                emptyApp.setCgpa("N/A");
                emptyApp.setSkills("N/A");
                emptyApp.setEducation("N/A");
                return emptyApp;
            }

            // Calculate similarity score using Eden AI
            double similarityScore = calculateSimilarityWithEdenAI(jobDescription, resumeContent);

            // Apply custom keyword bonus
            if (customKeywords != null && !customKeywords.trim().isEmpty()) {
                similarityScore += calculateKeywordBonus(resumeContent, customKeywords);
            }

            // Ensure score is within 0-100 range
            double finalScore = Math.min(100, Math.max(0, similarityScore * 100));

            rankedApp.setScore(finalScore);
            rankedApp.setScorePercentage(String.format("%.1f%%", finalScore));

            return rankedApp;
        } catch (Exception e) {
            // Return basic ranked application if analysis fails
            RankedApplication rankedApp = new RankedApplication();
            rankedApp.setScore(0.0);
            rankedApp.setScorePercentage("0.0%");
            rankedApp.setCgpa("N/A");
            rankedApp.setSkills("N/A");
            rankedApp.setEducation("N/A");
            return rankedApp;
        }
    }

    /**
     * Use Eden AI for comprehensive resume parsing with enhanced parameters
     */
    private ResumeAnalysis analyzeWithEdenAI(String resumeContent) {
        try {
            // Ensure resume content is not empty or null
            if (resumeContent == null || resumeContent.trim().isEmpty()) {
                System.out.println("=== RESUME CONTENT IS EMPTY ===");
                return null;
            }

            // Check if API key is configured
            if (apiKey == null || apiKey.trim().isEmpty()) {
                System.out.println("=== EDEN AI API KEY NOT CONFIGURED ===");
                // FALLBACK TO REGEX PARSING
                return extractWithRegex(resumeContent);
            }

            // Prepare request for Eden AI resume parser
            Map<String, Object> request = new HashMap<>();
            request.put("file_content", resumeContent);
            request.put("language", "en");

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + apiKey);
            headers.set("Content-Type", "application/json");
            headers.set("Accept", "application/json");
            headers.set("User-Agent", "ResumeParser/1.0");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            System.out.println("=== EDEN AI RESUME PARSER REQUEST ===");
            System.out.println("API URL: " + apiUrl);
            System.out.println("API Key: " + (apiKey != null ? "Configured" : "NOT CONFIGURED"));
            System.out.println("Resume content length: " + resumeContent.length());
            System.out.println("Request: " + objectMapper.writeValueAsString(request));

            // Use Eden AI resume parser
            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    entity,
                    String.class);

            System.out.println("=== EDEN AI RESUME PARSER RESPONSE ===");
            System.out.println("Response status: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getBody());

            // Parse the Eden AI response
            JsonNode jsonNode = objectMapper.readTree(response.getBody());

            System.out.println("=== EDEN AI JSON PARSING DEBUG ===");
            System.out.println("JSON node type: " + jsonNode.getNodeType());
            System.out.println("JSON node fields: " + jsonNode.fieldNames());

            ResumeAnalysis analysis = new ResumeAnalysis();

            // Try to extract information from Eden AI response
            if (jsonNode.has("result")) {
                JsonNode result = jsonNode.get("result");
                
                // Extract education
                if (result.has("education")) {
                    JsonNode educationNode = result.get("education");
                    if (educationNode.isArray() && educationNode.size() > 0) {
                        StringBuilder education = new StringBuilder();
                        for (JsonNode edu : educationNode) {
                            if (edu.has("degree")) {
                                if (education.length() > 0) education.append(", ");
                                education.append(edu.get("degree").asText());
                            }
                        }
                        analysis.setEducation(education.toString());
                    }
                }

                // Extract skills
                if (result.has("skills")) {
                    JsonNode skillsNode = result.get("skills");
                    if (skillsNode.isArray()) {
                        StringBuilder skills = new StringBuilder();
                        for (JsonNode skill : skillsNode) {
                            if (skills.length() > 0) skills.append(", ");
                            skills.append(skill.asText());
                        }
                        analysis.setSkills(skills.toString());
                    }
                }

                // Extract experience
                if (result.has("work_experience")) {
                    JsonNode expNode = result.get("work_experience");
                    if (expNode.isArray() && expNode.size() > 0) {
                        StringBuilder experience = new StringBuilder();
                        for (JsonNode exp : expNode) {
                            if (exp.has("job_title")) {
                                if (experience.length() > 0) experience.append(", ");
                                experience.append(exp.get("job_title").asText());
                            }
                        }
                        analysis.setExperience(experience.toString());
                    }
                }

                // Extract personal information for CGPA
                if (result.has("personal_infos")) {
                    JsonNode personalNode = result.get("personal_infos");
                    if (personalNode.has("education")) {
                        JsonNode eduNode = personalNode.get("education");
                        if (eduNode.isArray() && eduNode.size() > 0) {
                            for (JsonNode edu : eduNode) {
                                if (edu.has("gpa") || edu.has("grade")) {
                                    analysis.setCgpa(edu.get("gpa").asText());
                                    break;
                                }
                            }
                        }
                    }
                }
            }

            // If no structured data found, try alternative parsing
            if (analysis.getCgpa().equals("N/A") && analysis.getSkills().equals("N/A") &&
                    analysis.getEducation().equals("N/A") && analysis.getExperience().equals("N/A")) {
                System.out.println("=== EDEN AI RETURNED NO DATA, TRYING ALTERNATIVE PARSING ===");
                return parseRawResponse(response.getBody());
            }

            System.out.println("=== EDEN AI EXTRACTED ANALYSIS ===");
            System.out.println("CGPA: " + analysis.getCgpa());
            System.out.println("Skills: " + analysis.getSkills());
            System.out.println("Education: " + analysis.getEducation());
            System.out.println("Experience: " + analysis.getExperience());

            return analysis;
        } catch (Exception e) {
            System.out.println("=== EDEN AI ERROR ===");
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
            // FALLBACK TO REGEX PARSING
            return extractWithRegex(resumeContent);
        }
    }

    /**
     * Calculate semantic similarity between job description and resume using Eden AI
     */
    private double calculateSimilarityWithEdenAI(String jobDescription, String resumeContent) {
        try {
            // Prepare request for similarity calculation
            Map<String, Object> request = new HashMap<>();
            request.put("text_1", jobDescription);
            request.put("text_2", resumeContent);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + apiKey);
            headers.set("Content-Type", "application/json");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            // Use Eden AI similarity endpoint
            ResponseEntity<String> response = restTemplate.exchange(
                    similarityUrl,
                    HttpMethod.POST,
                    entity,
                    String.class);

            // Parse the similarity score from response
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            if (jsonNode.has("similarity")) {
                return jsonNode.get("similarity").asDouble(0.0);
            }
        } catch (Exception e) {
            System.err.println("Failed to get similarity from Eden AI: " + e.getMessage());
        }
        return 0.0;
    }

    /**
     * Use Eden AI for comprehensive job matching analysis
     */
    public List<RankedApplication> analyzeJobMatching(String jobDescription, List<ApplicationData> applications,
            String customKeywords) {
        List<RankedApplication> results = new ArrayList<>();

        try {
            for (ApplicationData app : applications) {
                try {
                    RankedApplication result = new RankedApplication();

                    // Analyze resume against job requirements using Eden AI
                    RankedApplication edenResult = analyzeResumeForJob(app.getResumeContent(), jobDescription,
                            customKeywords);

                    // Set application details
                    result.setApplication(app.getApplication());
                    result.setScore(edenResult.getScore());
                    result.setScorePercentage(edenResult.getScorePercentage());
                    result.setCgpa(edenResult.getCgpa());
                    result.setSkills(edenResult.getSkills());
                    result.setEducation(edenResult.getEducation());

                    results.add(result);

                } catch (Exception e) {
                    // Fallback to simple similarity calculation
                    RankedApplication fallback = analyzeResumeForJob(app.getResumeContent(), jobDescription,
                            customKeywords);
                    fallback.setApplication(app.getApplication());
                    results.add(fallback);
                }
            }

            // Sort by score in descending order
            results.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));

        } catch (Exception e) {
            // Fallback to standard ranking if comprehensive analysis fails
            return rankApplications(applications, jobDescription, customKeywords);
        }

        return results;
    }

    /**
     * Fallback method to extract resume information using regex patterns
     * This method is called when Eden AI fails or is not configured
     */
    private ResumeAnalysis extractWithRegex(String resumeContent) {
        ResumeAnalysis analysis = new ResumeAnalysis();

        // Extract CGPA using regex
        analysis.setCgpa(extractCGPA(resumeContent));

        // Extract skills using regex
        analysis.setSkills(extractSkills(resumeContent));

        // Extract education using regex
        analysis.setEducation(extractEducation(resumeContent));

        // Extract experience using regex
        analysis.setExperience(extractExperience(resumeContent));

        System.out.println("=== REGEX EXTRACTION RESULTS ===");
        System.out.println("CGPA: " + analysis.getCgpa());
        System.out.println("Skills: " + analysis.getSkills());
        System.out.println("Education: " + analysis.getEducation());
        System.out.println("Experience: " + analysis.getExperience());

        return analysis;
    }

    /**
     * Parse raw response text for structured data
     */
    private ResumeAnalysis parseRawResponse(String responseText) {
        ResumeAnalysis analysis = new ResumeAnalysis();

        if (responseText != null && !responseText.isEmpty()) {
            // Try to extract data from raw response
            analysis.setCgpa(extractCGPA(responseText));
            analysis.setSkills(extractSkills(responseText));
            analysis.setEducation(extractEducation(responseText));
            analysis.setExperience(extractExperience(responseText));
        }

        return analysis;
    }

    /**
     * Extract CGPA from resume content
     */
    private String extractCGPA(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "N/A";
        }

        String contentLower = content.toLowerCase();

        // Pattern 1: CGPA: 8.5/10 format
        Pattern pattern = Pattern.compile("(?i)cgpa\\s*[:\\s]*([0-9]+\\.?[0-9]*)\\s*/\\s*10");
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }

        // Pattern 2: CGPA: 8.5 format
        pattern = Pattern.compile("(?i)cgpa\\s*[:\\s]*([0-9]+\\.?[0-9]*)");
        matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }

        // Pattern 3: GPA: 3.5 format
        pattern = Pattern.compile("(?i)gpa\\s*[:\\s]*([0-9]+\\.?[0-9]*)");
        matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }

        // Pattern 4: Grade: 8.5 format
        pattern = Pattern.compile("(?i)grade\\s*[:\\s]*([0-9]+\\.?[0-9]*)");
        matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }

        // Pattern 5: 8.5 CGPA format
        pattern = Pattern.compile("(\\d+\\.?\\d*)\\s*(cgpa|gpa|grade)", Pattern.CASE_INSENSITIVE);
        matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }

        // Pattern 6: Standalone numbers in CGPA range (7.0-10.0)
        pattern = Pattern.compile("\\b([7-9]\\.\\d{1,2}|10\\.0*|10)\\b");
        matcher = pattern.matcher(content);
        while (matcher.find()) {
            String potentialCgpa = matcher.group(1);
            // Check context around the number
            int start = Math.max(0, matcher.start() - 100);
            int end = Math.min(content.length(), matcher.end() + 100);
            String context = content.substring(start, end).toLowerCase();

            if (context.contains("cgpa") || context.contains("gpa") || context.contains("grade") ||
                    context.contains("percentage") || context.contains("score") ||
                    context.contains("cgpa:") || context.contains("gpa:") || context.contains("grade:")) {
                return potentialCgpa;
            }
        }

        // Pattern 7: Numbers in brackets/parentheses
        pattern = Pattern.compile("\\((\\d+\\.?\\d*)\\)|\\[(\\d+\\.?\\d*)\\]");
        matcher = pattern.matcher(content);
        while (matcher.find()) {
            String potentialCgpa = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            if (potentialCgpa != null && (potentialCgpa.startsWith("7") || potentialCgpa.startsWith("8") ||
                    potentialCgpa.startsWith("9") || potentialCgpa.startsWith("10"))) {
                return potentialCgpa;
            }
        }

        return "N/A";
    }

    /**
     * Extract skills from resume content
     */
    private String extractSkills(String content) {
        // Look for common skill sections with enhanced patterns
        Pattern pattern = Pattern.compile(
                "(?i)(skills|technical skills|competencies|technical competencies|key skills|core competencies)\\s*[:\\n\\r]+([\\w\\s,\\-\\.\\+\\/]+?)(?=\\n\\s*\\n|\\n\\w+\\s*:|$)",
                Pattern.DOTALL);
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(2).trim().replaceAll("\\s+", " ");
        }

        // Try alternative pattern for bullet points or list format
        pattern = Pattern.compile(
                "(?i)(skills|technical skills|competencies|technical competencies|key skills|core competencies)\\s*[:\\n\\r]+([\\w\\s,\\-\\.\\+\\/]+?)(?=\\n\\s*\\n|\\n\\w+|$)",
                Pattern.DOTALL);
        matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(2).trim().replaceAll("\\s+", " ");
        }

        // Look for comma-separated skills in a single line with more comprehensive tech
        // stack
        pattern = Pattern.compile(
                "(?i)(java|python|react|angular|node\\.?js?|spring|hibernate|aws|docker|kubernetes|git|html|css|javascript|sql|mongodb|postgresql|mysql|typescript|vue|express|django|flask|tensorflow|pytorch|reactjs|angularjs|nodejs|springboot|hibernate|postgresql|mongodb|redis|kafka|jenkins|gitlab|github|azure|gcp|linux|unix|windows|macos)",
                Pattern.CASE_INSENSITIVE);
        StringBuilder skills = new StringBuilder();
        matcher = pattern.matcher(content);
        while (matcher.find()) {
            String skill = matcher.group(1);
            if (!skills.toString().toLowerCase().contains(skill.toLowerCase())) {
                if (skills.length() > 0)
                    skills.append(", ");
                skills.append(skill);
            }
        }

        if (skills.length() > 0) {
            return skills.toString();
        }

        // Look for skills in bullet points format
        pattern = Pattern.compile(
                "(?i)(-\\s*java|-\\s*python|-\\s*react|-\\s*angular|-\\s*node|-\\s*spring|-\\s*aws|-\\s*docker|-\\s*git|-\\s*html|-\\s*css|-\\s*javascript|-\\s*sql|-\\s*database|-\\s*framework|-\\s*technology)\\s+([\\w\\s,\\.]+)",
                Pattern.CASE_INSENSITIVE);
        matcher = pattern.matcher(content);
        StringBuilder bulletSkills = new StringBuilder();
        while (matcher.find()) {
            if (bulletSkills.length() > 0)
                bulletSkills.append(", ");
            bulletSkills.append(matcher.group(2).trim());
        }
        if (bulletSkills.length() > 0) {
            return bulletSkills.toString();
        }

        // Look for skills mentioned in projects or experience sections
        pattern = Pattern.compile(
                "(?i)(technologies|tools|stack|frameworks)\\s*[:\\n\\r]+([\\w\\s,\\-\\.\\+\\/]+?)(?=\\n\\s*\\n|\\n\\w+\\s*:|$)",
                Pattern.DOTALL);
        matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(2).trim().replaceAll("\\s+", " ");
        }

        return "N/A";
    }

    /**
     * Extract education from resume content
     */
    private String extractEducation(String content) {
        // Look for education sections
        Pattern pattern = Pattern.compile(
                "(?i)(education|qualifications|degrees?|academic background|educational qualifications)\\s*[:\\n\\r]+([\\w\\s,\\-\\.\\(\\)\\d]+?)(?=\\n\\s*\\n|\\n\\w+\\s*:|$)");
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(2).trim().replaceAll("\\s+", " ");
        }

        // Look for degree patterns
        pattern = Pattern.compile(
                "(?i)(b\\.?tech|b\\.?e\\.?|m\\.?tech|m\\.?e\\.?|b\\.?sc|m\\.?sc|b\\.?com|m\\.?com|ph\\.?d|diploma)\\s+in\\s+([\\w\\s]+)",
                Pattern.CASE_INSENSITIVE);
        matcher = pattern.matcher(content);
        StringBuilder education = new StringBuilder();
        while (matcher.find()) {
            if (education.length() > 0)
                education.append(", ");
            education.append(matcher.group(1)).append(" in ").append(matcher.group(2));
        }

        if (education.length() > 0) {
            return education.toString();
        }

        return "N/A";
    }

    /**
     * Extract experience from resume content
     */
    private String extractExperience(String content) {
        // Look for experience sections with enhanced patterns
        Pattern pattern = Pattern.compile(
                "(?i)(experience|work experience|professional experience|employment history)\\s*[:\\n\\r]+([\\w\\s,\\-\\.\\(\\)\\d]+?)(?=\\n\\s*\\n|\\n\\w+\\s*:|$)",
                Pattern.DOTALL);
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(2).trim().replaceAll("\\s+", " ");
        }

        // Try alternative pattern for bullet points or list format
        pattern = Pattern.compile(
                "(?i)(experience|work experience|professional experience|employment history)\\s*[:\\n\\r]+([\\w\\s,\\-\\.\\(\\)\\d]+?)(?=\\n\\s*\\n|\\n\\w+|$)",
                Pattern.DOTALL);
        matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(2).trim().replaceAll("\\s+", " ");
        }

        // Look for standalone experience mentions with company names
        pattern = Pattern.compile(
                "(?i)(\\d+\\s+years?\\s+of\\s+experience|intern|developer|engineer|analyst)\\s+at\\s+([\\w\\s]+)",
                Pattern.CASE_INSENSITIVE);
        matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1) + " at " + matcher.group(2);
        }

        // Look for internship or job descriptions
        pattern = Pattern.compile(
                "(?i)(intern|developer|engineer|analyst|software developer)\\s+at\\s+([\\w\\s]+),\\s*(\\w+\\s+\\d{4})",
                Pattern.CASE_INSENSITIVE);
        matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1) + " at " + matcher.group(2) + ", " + matcher.group(3);
        }

        // Look for experience in bullet points format
        pattern = Pattern.compile("(?i)(-\\s*developed|-\\s*collaborated|-\\s*technologies)\\s+([\\w\\s,\\.]+)",
                Pattern.CASE_INSENSITIVE);
        matcher = pattern.matcher(content);
        StringBuilder experience = new StringBuilder();
        while (matcher.find()) {
            if (experience.length() > 0)
                experience.append(", ");
            experience.append(matcher.group(2).trim());
        }
        if (experience.length() > 0) {
            return experience.toString();
        }

        // Look for years of experience
        pattern = Pattern.compile("(?i)(\\d+)\\s+years?\\s+of\\s+experience", Pattern.CASE_INSENSITIVE);
        matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1) + " years of experience";
        }

        return "N/A";
    }

    /**
     * Calculate keyword bonus for custom keywords
     */
    private double calculateKeywordBonus(String resumeContent, String customKeywords) {
        if (resumeContent == null || customKeywords == null) {
            return 0.0;
        }
        String resumeLower = resumeContent.toLowerCase();
        String[] keywords = customKeywords.toLowerCase().split("\\s*,\\s*|\\s+");
        int keywordMatches = 0;

        for (String keyword : keywords) {
            if (keyword.length() > 2 && resumeLower.contains(keyword)) {
                keywordMatches++;
            }
        }

        // Return bonus based on keyword matches (max 10 points)
        return Math.min(10.0, keywordMatches * 2.0);
    }

    /**
     * Rank applications using Eden AI
     */
    public List<RankedApplication> rankApplications(List<ApplicationData> applications, String jobDescription,
            String customKeywords) {
        List<RankedApplication> rankedApplications = new ArrayList<>();

        for (ApplicationData app : applications) {
            try {
                // Use comprehensive job matching with Eden AI
                RankedApplication rankedApp = analyzeResumeForJob(app.getResumeContent(), jobDescription,
                        customKeywords);

                // Set application details
                rankedApp.setApplication(app.getApplication());

                // If Eden AI analysis failed, use fallback analysis
                if (rankedApp.getCgpa().equals("N/A") && rankedApp.getSkills().equals("N/A")) {
                    ResumeAnalysis fallbackAnalysis = new ResumeAnalysis();
                    fallbackAnalysis.setCgpa(app.getCgpa());
                    fallbackAnalysis.setSkills(app.getSkills());
                    fallbackAnalysis.setEducation(app.getEducation());

                    rankedApp.setCgpa(fallbackAnalysis.getCgpa());
                    rankedApp.setSkills(fallbackAnalysis.getSkills());
                    rankedApp.setEducation(fallbackAnalysis.getEducation());
                }

                rankedApplications.add(rankedApp);
            } catch (Exception e) {
                // If ranking fails for an application, skip it
                continue;
            }
        }

        // Sort by score in descending order
        rankedApplications.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        return rankedApplications;
    }

    // Data classes
    public static class ResumeAnalysis {
        private String cgpa;
        private String skills;
        private String education;
        private String experience;

        // Getters and setters
        public String getCgpa() {
            return cgpa;
        }

        public void setCgpa(String cgpa) {
            this.cgpa = cgpa;
        }

        public String getSkills() {
            return skills;
        }

        public void setSkills(String skills) {
            this.skills = skills;
        }

        public String getEducation() {
            return education;
        }

        public void setEducation(String education) {
            this.education = education;
        }

        public String getExperience() {
            return experience;
        }

        public void setExperience(String experience) {
            this.experience = experience;
        }
    }

    public static class ApplicationData {
        private Application application;
        private String resumeContent;
        private String cgpa;
        private String skills;
        private String education;

        // Getters and setters
        public Application getApplication() {
            return application;
        }

        public void setApplication(Application application) {
            this.application = application;
        }

        public String getResumeContent() {
            return resumeContent;
        }

        public void setResumeContent(String resumeContent) {
            this.resumeContent = resumeContent;
        }

        public String getCgpa() {
            return cgpa;
        }

        public void setCgpa(String cgpa) {
            this.cgpa = cgpa;
        }

        public String getSkills() {
            return skills;
        }

        public void setSkills(String skills) {
            this.skills = skills;
        }

        public String getEducation() {
            return education;
        }

        public void setEducation(String education) {
            this.education = education;
        }
    }

    public static class RankedApplication {
        private Application application;
        private double score;
        private String scorePercentage;
        private String cgpa;
        private String skills;
        private String education;

        // Getters and setters
        public Application getApplication() {
            return application;
        }

        public void setApplication(Application application) {
            this.application = application;
        }

        public double getScore() {
            return score;
        }

        public void setScore(double score) {
            this.score = score;
        }

        public String getScorePercentage() {
            return scorePercentage;
        }

        public void setScorePercentage(String scorePercentage) {
            this.scorePercentage = scorePercentage;
        }

        public String getCgpa() {
            return cgpa;
        }

        public void setCgpa(String cgpa) {
            this.cgpa = cgpa;
        }

        public String getSkills() {
            return skills;
        }

        public void setSkills(String skills) {
            this.skills = skills;
        }

        public String getEducation() {
            return education;
        }

        public void setEducation(String education) {
            this.education = education;
        }
    }
}