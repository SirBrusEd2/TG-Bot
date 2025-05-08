// DiagnosisSession.java
package org.example;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

public class DiagnosisSession {
    private DiagnosticTest currentTest;
    private Map<String, Integer> collectedAnswers;
    private int currentQuestionIndex;

    public DiagnosisSession(DiagnosticTest test) {
        this.currentTest = test;
        this.collectedAnswers = new HashMap<>();
        this.currentQuestionIndex = 0;
    }

    public DiagnosticQuestion getNextQuestion() {
        if (currentQuestionIndex >= currentTest.getQuestions().size()) {
            return null;
        }
        return currentTest.getQuestions().get(currentQuestionIndex++);
    }

    public void recordAnswer(String parameterName, int value) {
        collectedAnswers.put(parameterName, value);
    }

    public boolean isComplete() {
        return currentQuestionIndex >= currentTest.getQuestions().size();
    }

    public String getDiagnosisResult() {
        int totalScore = calculateTotalScore();
        Map<String, String> rules = currentTest.getDiagnosisRules();

        // Сортируем правила по ключам (пороги баллов)
        List<Map.Entry<String, String>> sortedRules = rules.entrySet().stream()
                .sorted((e1, e2) -> compareRuleKeys(e1.getKey(), e2.getKey()))
                .collect(Collectors.toList());

        // Находим подходящее правило
        for (Map.Entry<String, String> entry : sortedRules) {
            String key = entry.getKey();
            if (matchesRule(totalScore, key)) {
                return entry.getValue();
            }
        }

        return "Неизвестная степень тяжести";
    }

    private boolean matchesRule(int score, String ruleKey) {
        if (ruleKey.startsWith("≥")) {
            int threshold = Integer.parseInt(ruleKey.substring(1).trim());
            return score >= threshold;
        } else if (ruleKey.contains("-")) {
            String[] parts = ruleKey.split("-");
            int min = Integer.parseInt(parts[0].trim());
            int max = Integer.parseInt(parts[1].trim());
            return score >= min && score <= max;
        } else {
            // Для отдельных значений
            int value = Integer.parseInt(ruleKey.trim());
            return score == value;
        }
    }

    private int compareRuleKeys(String key1, String key2) {
        // Извлекаем числовые значения для сравнения
        int val1 = extractMaxValue(key1);
        int val2 = extractMaxValue(key2);
        return Integer.compare(val1, val2);
    }

    private int extractMaxValue(String key) {
        if (key.startsWith("≥")) {
            return Integer.parseInt(key.substring(1).trim());
        } else if (key.contains("-")) {
            String[] parts = key.split("-");
            return Integer.parseInt(parts[1].trim());
        } else {
            return Integer.parseInt(key.trim());
        }
    }

    public int getCurrentQuestionNumber() {
        return currentQuestionIndex;
    }

    public int getTotalQuestions() {
        return currentTest.getQuestions().size();
    }

    public DiagnosticQuestion getCurrentQuestion() {
        if (currentQuestionIndex == 0 || currentQuestionIndex > currentTest.getQuestions().size()) {
            return null;
        }
        return currentTest.getQuestions().get(currentQuestionIndex - 1);
    }

    public List<DiagnosticQuestion> getQuestions() {
        return currentTest.getQuestions();
    }

    public int calculateTotalScore() {
        return collectedAnswers.values().stream().mapToInt(Integer::intValue).sum();
    }

    // Добавленные методы для доступа к данным сессии
    public Map<String, Integer> getCollectedAnswers() {
        return new HashMap<>(collectedAnswers);
    }

    public DiagnosticTest getCurrentTest() {
        return currentTest;
    }

    public boolean hasAnswerFor(String parameterName) {
        return collectedAnswers.containsKey(parameterName);
    }


    public Integer getAnswerFor(String parameterName) {
        return collectedAnswers.get(parameterName);
    }
}