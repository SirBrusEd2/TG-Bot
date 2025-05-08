package org.example;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Comparator;
import java.util.stream.Collectors;

public class DiagnosticTest {
    private String testName;
    private List<DiagnosticQuestion> questions;
    private Map<String, String> diagnosisMap;
    private Map<String, String> diagnosisRules;

    @JsonCreator
    public DiagnosticTest(
            @JsonProperty("testName") String testName,
            @JsonProperty("questions") List<DiagnosticQuestion> questions,
            @JsonProperty("diagnosisRules") Map<String, String> diagnosisRules) {
        this.testName = testName;
        this.questions = questions != null ? questions : new ArrayList<>();
        this.diagnosisMap = diagnosisRules != null ? diagnosisRules : new HashMap<>();
        this.diagnosisRules = diagnosisRules;
    }

    public void addQuestion(DiagnosticQuestion question) {
        questions.add(question);
    }

    public void addDiagnosisRule(String scoreRange, String diagnosis) {
        diagnosisMap.put(scoreRange, diagnosis);
    }

    public List<DiagnosticQuestion> getQuestions() {
        return new ArrayList<>(questions);
    }

    public String getTestName() {
        return testName;
    }

    public String evaluateDiagnosis(int totalScore) {
        if (diagnosisMap == null || diagnosisMap.isEmpty()) {
            return "Не определена";
        }

        // Сортируем диапазоны по возрастанию
        List<Map.Entry<String, String>> sortedRanges = diagnosisMap.entrySet()
                .stream()
                .sorted(Comparator.comparingInt(e -> parseRangeStart(e.getKey())))
                .collect(Collectors.toList());

        for (Map.Entry<String, String> entry : sortedRanges) {
            if (isScoreInRange(totalScore, entry.getKey())) {
                return entry.getValue();
            }
        }
        return "Не определена";
    }

    private int parseRangeStart(String range) {
        try {
            if (range.contains("-")) {
                return Integer.parseInt(range.split("-")[0].trim());
            } else if (range.startsWith(">=")) {
                return Integer.parseInt(range.substring(2).trim());
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private boolean isScoreInRange(int score, String range) {
        try {
            if (range.contains("-")) {
                String[] parts = range.split("-");
                int min = Integer.parseInt(parts[0].trim());
                int max = Integer.parseInt(parts[1].trim());
                return score >= min && score <= max;
            } else if (range.startsWith(">=")) {
                int min = Integer.parseInt(range.substring(2).trim());
                return score >= min;
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return false;
    }

    public Map<String, String> getDiagnosisRules() {
        return diagnosisRules;
    }

}
