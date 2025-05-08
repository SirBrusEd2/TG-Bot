package org.example;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.*;

public class DiagnosticQuestion {
    private String questionText;
    private String parameterName;
    private Map<String, Integer> answerValues;

    @JsonCreator
    public DiagnosticQuestion(
            @JsonProperty("questionText") String questionText,
            @JsonProperty("parameterName") String parameterName,
            @JsonProperty("answers") LinkedHashMap<String, Integer> answers) {
        this.questionText = questionText;
        this.parameterName = parameterName;
        this.answerValues = answers != null ? new LinkedHashMap<>(answers) : new LinkedHashMap<>();
    }

    public String getQuestionText() {
        return questionText;
    }

    public String getParameterName() {
        return parameterName;
    }

    public Map<String, Integer> getAnswerValues() {
        return new HashMap<>(answerValues);
    }

    public List<String> getPossibleAnswers() {
        return new ArrayList<>(answerValues.keySet());
    }

    public Integer getValueForAnswer(String answer) {
        return answerValues.get(answer);
    }
}
