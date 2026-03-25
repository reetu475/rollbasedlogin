package com.example.rollbasedlogin.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.rollbasedlogin.model.Application;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class HuggingFaceAIService {

    @Value("${huggingface.api.key:}")
    String apiKey;

    @Value("${huggingface.api.url:https://api-inference.huggingface.co/models/sentence-transformers/all-MiniLM-L6-v2}")
    String apiUrl;

    @Value("${huggingface.extraction.url}")
    private String extractionApiUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Analyze resume content using Hugging Face model for semantic similarity
     * Focus on ranking rather than field extraction
     */
    public ResumeAnalysis analyzeResume(String resumeContent) {
        try {
            ResumeAnalysis analysis = new ResumeAnalysis();

            // Use Hugging Face model for semantic analysis
            ResumeAnalysis modelAnalysis = analyzeResumeSemantically(resumeContent);
            if (modelAnalysis != null) {
                return modelAnalysis;
            }

            // If model fails, return empty analysis
            return new ResumeAnalysis();
        } catch (Exception e) {
            // Return empty analysis if parsing fails
            return new ResumeAnalysis();
        }
    }

    /**
     * Analyze resume semantically using cross-encoder model for better ranking
     */
    private ResumeAnalysis analyzeResumeSemantically(String resumeContent) {
        try {
            // Ensure resume content is not empty or null
            if (resumeContent == null || resumeContent.trim().isEmpty()) {
                return null;
            }

            // Check if API key is configured
            if (apiKey == null || apiKey.trim().isEmpty()) {
                System.out.println("=== HUGGING FACE API KEY NOT CONFIGURED ===");
                return null; // No fallback to regex parsing
            }

            // For semantic analysis, we'll use the cross-encoder model
            // This model is better suited for ranking and similarity analysis
            ResumeAnalysis analysis = new ResumeAnalysis();

            // Use Hugging Face model for field extraction
            ResumeAnalysis modelExtraction = extractWithHuggingFaceModel(resumeContent);
            if (modelExtraction != null) {
                analysis.setCgpa(modelExtraction.getCgpa());
                analysis.setSkills(modelExtraction.getSkills());
                analysis.setEducation(modelExtraction.getEducation());
                analysis.setExperience(modelExtraction.getExperience());

                // Only return if we have meaningful data
                if (!analysis.getCgpa().equals("N/A") || !analysis.getSkills().equals("N/A") ||
                        !analysis.getEducation().equals("N/A") || !analysis.getExperience().equals("N/A")) {
                    return analysis;
                }
            }

            return null;
        } catch (Exception e) {
            System.out.println("=== SEMANTIC ANALYSIS ERROR ===");
            System.out.println("Error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Use Hugging Face model for comprehensive resume analysis and job matching
     */
    public RankedApplication analyzeResumeForJob(String resumeContent, String jobDescription, String customKeywords) {
        try {
            RankedApplication rankedApp = new RankedApplication();

            // Extract resume information using Hugging Face model
            ResumeAnalysis analysis = extractWithHuggingFaceModel(resumeContent);
            if (analysis != null) {
                rankedApp.setCgpa(analysis.getCgpa());
                rankedApp.setSkills(analysis.getSkills());
                rankedApp.setEducation(analysis.getEducation());
            }

            // If Hugging Face model failed to extract information, return empty analysis
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

            // Calculate similarity score using sentence-transformers model
            double similarityScore = calculateSimilarity(jobDescription, resumeContent);

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
     * Use Hugging Face model for resume parsing with enhanced parameters
     */
    private ResumeAnalysis extractWithHuggingFaceModel(String resumeContent) {
        try {
            // Ensure resume content is not empty or null
            if (resumeContent == null || resumeContent.trim().isEmpty()) {
                System.out.println("=== RESUME CONTENT IS EMPTY ===");
                return null;
            }

            // Check if API key is configured
            if (apiKey == null || apiKey.trim().isEmpty()) {
                System.out.println("=== HUGGING FACE API KEY NOT CONFIGURED ===");
                // FALLBACK TO REGEX PARSING
                return extractWithRegex(resumeContent);
            }

            // Prepare request for resume parsing model with optimized parameters
            Map<String, Object> request = new HashMap<>();
            request.put("inputs", resumeContent);

            // Add optimized parameters for better parsing
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("max_length", 1500); // Increased for better parsing
            parameters.put("temperature", 0.1); // Lower for more deterministic results
            parameters.put("top_p", 0.9);
            parameters.put("do_sample", false); // Deterministic for consistency
            request.put("parameters", parameters);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + apiKey);
            headers.set("Content-Type", "application/json");
            headers.set("Accept", "application/json");
            headers.set("User-Agent", "ResumeParser/1.0");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            System.out.println("=== HUGGING FACE MODEL REQUEST ===");
            System.out.println("Model URL: " + extractionApiUrl);
            System.out.println("API Key: " + (apiKey != null ? "Configured" : "NOT CONFIGURED"));
            System.out.println("Resume content length: " + resumeContent.length());
            System.out.println(
                    "Resume content preview: " + resumeContent.substring(0, Math.min(200, resumeContent.length())));
            System.out.println("Request: " + objectMapper.writeValueAsString(request));

            // Use specialized extraction model for better structured data extraction
            ResponseEntity<String> response = restTemplate.exchange(
                    extractionApiUrl,
                    HttpMethod.POST,
                    entity,
                    String.class);

            System.out.println("=== HUGGING FACE MODEL RESPONSE ===");
            System.out.println("Response status: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getBody());

            // Parse the model response
            JsonNode jsonNode = objectMapper.readTree(response.getBody());

            System.out.println("=== JSON PARSING DEBUG ===");
            System.out.println("JSON node type: " + jsonNode.getNodeType());
            System.out.println("JSON node fields: " + jsonNode.fieldNames());
            if (jsonNode.isArray()) {
                System.out.println("Array size: " + jsonNode.size());
                for (int i = 0; i < jsonNode.size(); i++) {
                    System.out.println("Array element " + i + ": " + jsonNode.get(i));
                }
            }

            ResumeAnalysis analysis = new ResumeAnalysis();

            // Try to extract information from model response with multiple formats
            if (jsonNode.has("education")) {
                analysis.setEducation(jsonNode.get("education").asText());
            } else if (jsonNode.has("educations")) {
                analysis.setEducation(jsonNode.get("educations").asText());
            } else if (jsonNode.has("education_history")) {
                analysis.setEducation(jsonNode.get("education_history").asText());
            } else if (jsonNode.has("academic_background")) {
                analysis.setEducation(jsonNode.get("academic_background").asText());
            } else if (jsonNode.has("degrees")) {
                analysis.setEducation(jsonNode.get("degrees").asText());
            } else if (jsonNode.has("qualifications")) {
                analysis.setEducation(jsonNode.get("qualifications").asText());
            } else {
                System.out.println("=== NO EDUCATION FIELD FOUND ===");
            }

            if (jsonNode.has("skills")) {
                analysis.setSkills(jsonNode.get("skills").asText());
            } else if (jsonNode.has("technical_skills")) {
                analysis.setSkills(jsonNode.get("technical_skills").asText());
            } else if (jsonNode.has("competencies")) {
                analysis.setSkills(jsonNode.get("competencies").asText());
            } else if (jsonNode.has("key_skills")) {
                analysis.setSkills(jsonNode.get("key_skills").asText());
            } else if (jsonNode.has("programming_languages")) {
                analysis.setSkills(jsonNode.get("programming_languages").asText());
            } else if (jsonNode.has("technologies")) {
                analysis.setSkills(jsonNode.get("technologies").asText());
            } else {
                System.out.println("=== NO SKILLS FIELD FOUND ===");
            }

            if (jsonNode.has("cgpa") || jsonNode.has("gpa")) {
                String cgpa = jsonNode.has("cgpa") ? jsonNode.get("cgpa").asText() : jsonNode.get("gpa").asText();
                analysis.setCgpa(cgpa);
            } else if (jsonNode.has("grade") || jsonNode.has("grades")) {
                String grade = jsonNode.has("grade") ? jsonNode.get("grade").asText() : jsonNode.get("grades").asText();
                analysis.setCgpa(grade);
            } else if (jsonNode.has("percentage")) {
                analysis.setCgpa(jsonNode.get("percentage").asText());
            } else if (jsonNode.has("score")) {
                analysis.setCgpa(jsonNode.get("score").asText());
            } else if (jsonNode.has("cgpa_gpa")) {
                analysis.setCgpa(jsonNode.get("cgpa_gpa").asText());
            } else {
                System.out.println("=== NO CGPA/GPA FIELD FOUND ===");
            }

            if (jsonNode.has("experience")) {
                analysis.setExperience(jsonNode.get("experience").asText());
            } else if (jsonNode.has("work_experience")) {
                analysis.setExperience(jsonNode.get("work_experience").asText());
            } else if (jsonNode.has("experiences")) {
                analysis.setExperience(jsonNode.get("experiences").asText());
            } else if (jsonNode.has("professional_experience")) {
                analysis.setExperience(jsonNode.get("professional_experience").asText());
            } else if (jsonNode.has("employment_history")) {
                analysis.setExperience(jsonNode.get("employment_history").asText());
            } else if (jsonNode.has("work_history")) {
                analysis.setExperience(jsonNode.get("work_history").asText());
            } else {
                System.out.println("=== NO EXPERIENCE FIELD FOUND ===");
            }

            // If no structured data found, try alternative parsing
            if (analysis.getCgpa().equals("N/A") && analysis.getSkills().equals("N/A") &&
                    analysis.getEducation().equals("N/A") && analysis.getExperience().equals("N/A")) {
                System.out.println("=== HUGGING FACE MODEL RETURNED NO DATA, TRYING ALTERNATIVE PARSING ===");

                // Try parsing as array response
                if (jsonNode.isArray() && jsonNode.size() > 0) {
                    JsonNode firstElement = jsonNode.get(0);
                    if (firstElement.isObject()) {
                        // Try to extract from first array element
                        if (firstElement.has("generated_text")) {
                            String generatedText = firstElement.get("generated_text").asText();
                            // Try to parse the generated text for structured data
                            return parseGeneratedText(generatedText);
                        }
                    }
                }

                // Try to parse the raw response text
                return parseRawResponse(response.getBody());
            }

            System.out.println("=== EXTRACTED ANALYSIS ===");
            System.out.println("CGPA: " + analysis.getCgpa());
            System.out.println("Skills: " + analysis.getSkills());
            System.out.println("Education: " + analysis.getEducation());
            System.out.println("Experience: " + analysis.getExperience());

            return analysis;
        } catch (Exception e) {
            System.out.println("=== HUGGING FACE MODEL ERROR ===");
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
            // FALLBACK TO REGEX PARSING
            return extractWithRegex(resumeContent);
        }
    }

    /**
     * Comprehensive job matching using Hugging Face models for complete analysis
     */
    public List<RankedApplication> rankApplications(List<ApplicationData> applications, String jobDescription,
            String customKeywords) {
        List<RankedApplication> rankedApplications = new ArrayList<>();

        for (ApplicationData app : applications) {
            try {
                // Use comprehensive job matching with Hugging Face
                RankedApplication rankedApp = analyzeResumeForJob(app.getResumeContent(), jobDescription,
                        customKeywords);

                // Set application details
                rankedApp.setApplication(app.getApplication());

                // If Hugging Face analysis failed, use fallback analysis
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

    /**
     * Calculate semantic similarity between job description and resume using
     * Hugging Face
     */
    private double calculateSimilarity(String jobDescription, String resumeContent) {
        try {
            // Get embeddings for both job description and resume
            double[] jobEmbedding = getEmbedding(jobDescription);
            double[] resumeEmbedding = getEmbedding(resumeContent);

            // Calculate cosine similarity between embeddings
            return cosineSimilarity(jobEmbedding, resumeEmbedding);
        } catch (Exception e) {
            // Fallback to simple keyword matching if API fails
            return calculateFallbackSimilarity(jobDescription, resumeContent);
        }
    }

    /**
     * Get embeddings for a text using Hugging Face sentence-transformers model
     */
    private double[] getEmbedding(String text) {
        try {
            // Prepare request for embeddings
            Map<String, Object> request = new HashMap<>();
            request.put("inputs", text);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + apiKey);
            headers.set("Content-Type", "application/json");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            // Use the configured sentence-transformers model
            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    entity,
                    String.class);

            // Parse the embedding from response
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            if (jsonNode.isArray()) {
                double[] embedding = new double[jsonNode.size()];
                for (int i = 0; i < jsonNode.size(); i++) {
                    embedding[i] = jsonNode.get(i).asDouble(0.0);
                }
                return embedding;
            }
        } catch (Exception e) {
            System.err.println("Failed to get embedding: " + e.getMessage());
        }
        return new double[0];
    }

    /**
     * Calculate cosine similarity between two vectors
     */
    private double cosineSimilarity(double[] vecA, double[] vecB) {
        double dot = 0.0, normA = 0.0, normB = 0.0;
        for (int i = 0; i < vecA.length; i++) {
            dot += vecA[i] * vecB[i];
            normA += vecA[i] * vecA[i];
            normB += vecB[i] * vecB[i];
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * Use Hugging Face for comprehensive job matching analysis
     */
    public List<RankedApplication> analyzeJobMatching(String jobDescription, List<ApplicationData> applications,
            String customKeywords) {
        List<RankedApplication> results = new ArrayList<>();

        // Prepare job description analysis request
        String jobAnalysisPrompt = "Analyze this job description and extract key requirements: " + jobDescription;

        try {
            // Get job requirements using Hugging Face
            String jobRequirements = analyzeJobDescription(jobDescription);

            for (ApplicationData app : applications) {
                try {
                    RankedApplication result = new RankedApplication();

                    // Analyze resume against job requirements
                    String matchingPrompt = String.format(
                            "Compare this resume: %s\n\nAgainst these job requirements: %s\n\nProvide a detailed matching score from 0-100 and extract CGPA, skills, and education.",
                            app.getResumeContent(), jobRequirements);

                    // Get comprehensive analysis
                    Map<String, Object> analysis = getComprehensiveAnalysis(matchingPrompt);

                    // Extract results
                    double score = ((Number) analysis.getOrDefault("score", 0)).doubleValue();
                    String cgpa = (String) analysis.getOrDefault("cgpa", "N/A");
                    String skills = (String) analysis.getOrDefault("skills", "N/A");
                    String education = (String) analysis.getOrDefault("education", "N/A");

                    // Apply custom keyword bonus
                    if (customKeywords != null && !customKeywords.trim().isEmpty()) {
                        double keywordBonus = calculateKeywordBonus(app.getResumeContent(), customKeywords);
                        score += keywordBonus;
                    }

                    // Ensure score is within 0-100 range
                    score = Math.min(100, Math.max(0, score));

                    result.setApplication(app.getApplication());
                    result.setScore(score);
                    result.setScorePercentage(String.format("%.1f%%", score));
                    result.setCgpa(cgpa);
                    result.setSkills(skills);
                    result.setEducation(education);

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
     * Analyze job description to extract key requirements
     */
    private String analyzeJobDescription(String jobDescription) {
        try {
            // Prepare request for job description analysis
            Map<String, Object> request = new HashMap<>();
            request.put("inputs", "Extract key requirements, skills, and qualifications from this job description: "
                    + jobDescription);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + apiKey);
            headers.set("Content-Type", "application/json");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            // Use a text generation model for job analysis
            String analysisUrl = "https://router.huggingface.co/models/gpt2";
            ResponseEntity<String> response = restTemplate.exchange(
                    analysisUrl,
                    HttpMethod.POST,
                    entity,
                    String.class);

            return response.getBody();
        } catch (Exception e) {
            return jobDescription; // Return original if analysis fails
        }
    }

    /**
     * Get comprehensive analysis from Hugging Face model
     */
    private Map<String, Object> getComprehensiveAnalysis(String prompt) {
        Map<String, Object> result = new HashMap<>();

        try {
            Map<String, Object> request = new HashMap<>();
            request.put("inputs", prompt);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + apiKey);
            headers.set("Content-Type", "application/json");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            // Use a text generation model for comprehensive analysis
            String analysisUrl = "https://router.huggingface.co/models/gpt2";
            ResponseEntity<String> response = restTemplate.exchange(
                    analysisUrl,
                    HttpMethod.POST,
                    entity,
                    String.class);

            String analysisText = response.getBody();

            // Extract score from analysis text
            result.put("score", extractScoreFromText(analysisText));
            result.put("cgpa", extractCGPAFromText(analysisText));
            result.put("skills", extractSkillsFromText(analysisText));
            result.put("education", extractEducationFromText(analysisText));

        } catch (Exception e) {
            result.put("score", 0);
            result.put("cgpa", "N/A");
            result.put("skills", "N/A");
            result.put("education", "N/A");
        }

        return result;
    }

    /**
     * Extract score from analysis text
     */
    private double extractScoreFromText(String text) {
        Pattern pattern = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(?:%|percent|score|out of 100)",
                Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return Double.parseDouble(matcher.group(1));
        }
        return 0.0;
    }

    /**
     * Extract CGPA from analysis text
     */
    private String extractCGPAFromText(String text) {
        Pattern pattern = Pattern.compile("(?i)(cgpa|gpa|grade)\\s*[:\\s]*([0-9]+\\.?[0-9]*)");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(2);
        }
        return "N/A";
    }

    /**
     * Extract skills from analysis text
     */
    private String extractSkillsFromText(String text) {
        Pattern pattern = Pattern.compile(
                "(?i)(skills|technical skills|competencies)\\s*[:\\n\\r]+([\\w\\s,\\-\\.\\+\\/]+?)(?=\\n\\s*\\n|\\n\\w+\\s*:|$)");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(2).trim().replaceAll("\\s+", " ");
        }
        return "N/A";
    }

    /**
     * Extract education from analysis text
     */
    private String extractEducationFromText(String text) {
        Pattern pattern = Pattern.compile(
                "(?i)(education|qualifications|degrees?)\\s*[:\\n\\r]+([\\w\\s,\\-\\.\\(\\)\\d]+?)(?=\\n\\s*\\n|\\n\\w+\\s*:|$)");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(2).trim().replaceAll("\\s+", " ");
        }
        return "N/A";
    }

    /**
     * Extract experience from analysis text
     */
    private String extractExperienceFromText(String text) {
        // Enhanced pattern to capture experience information more effectively
        Pattern pattern = Pattern.compile(
                "(?i)(experience|work experience|professional experience|employment history|internship|job)\\s*[:\\n\\r]+([\\w\\s,\\-\\.\\(\\)\\d]+?)(?=\\n\\s*\\n|\\n\\w+\\s*:|$|\\n\\s*$)",
                Pattern.DOTALL);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(2).trim().replaceAll("\\s+", " ");
        }

        // Try alternative pattern for bullet points or list format
        pattern = Pattern.compile(
                "(?i)(experience|work experience|professional experience|employment history|internship|job)\\s*[:\\n\\r]+([\\w\\s,\\-\\.\\(\\)\\d]+?)(?=\\n\\s*\\n|\\n\\w+|$)",
                Pattern.DOTALL);
        matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(2).trim().replaceAll("\\s+", " ");
        }

        // Look for standalone experience mentions
        pattern = Pattern.compile(
                "(?i)(\\d+\\s+years?\\s+of\\s+experience|intern|developer|engineer|analyst)\\s+at\\s+([\\w\\s]+)",
                Pattern.CASE_INSENSITIVE);
        matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1) + " at " + matcher.group(2);
        }

        return "N/A";
    }

    /**
     * Fallback similarity calculation using simple text matching
     */
    private double calculateFallbackSimilarity(String jobDescription, String resumeContent) {
        String jobLower = jobDescription.toLowerCase();
        String resumeLower = resumeContent.toLowerCase();

        // Count matching words
        String[] jobWords = jobLower.split("\\s+");
        int matches = 0;
        int totalWords = jobWords.length;

        for (String word : jobWords) {
            if (resumeLower.contains(word) && word.length() > 3) {
                matches++;
            }
        }

        return (double) matches / Math.max(1, totalWords);
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
     * Parse generated text from Hugging Face model response
     */
    private ResumeAnalysis parseGeneratedText(String generatedText) {
        ResumeAnalysis analysis = new ResumeAnalysis();

        // Try to extract structured data from generated text
        if (generatedText != null && !generatedText.isEmpty()) {
            // Look for common patterns in generated text
            analysis.setCgpa(extractCGPA(generatedText));
            analysis.setSkills(extractSkills(generatedText));
            analysis.setEducation(extractEducation(generatedText));
            analysis.setExperience(extractExperience(generatedText));
        }

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
     * Fallback method to extract resume information using regex patterns
     * This method is called when Hugging Face AI fails or is not configured
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
