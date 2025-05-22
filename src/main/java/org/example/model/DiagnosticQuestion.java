package org.example.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.*;

/**
 * Класс, представляющий вопрос диагностического теста.
 * <p>
 * Содержит текст вопроса, название параметра для диагностики и возможные варианты ответов
 * с соответствующими числовыми значениями для расчета итогового результата.
 * </p>
 */
public class DiagnosticQuestion {
    private String questionText;
    private String parameterName;
    private Map<String, Integer> answerValues;

    /**
     * Конструктор для создания вопроса с десериализацией из JSON.
     *
     * @param questionText текст вопроса, отображаемый пользователю
     * @param parameterName название параметра, используемое для диагностики
     * @param answers карта возможных ответов с соответствующими баллами
     *
     * <p>Особенности:
     * <ul>
     *     <li>Использует аннотации Jackson для корректной десериализации из JSON</li>
     *     <li>Создает защищенную копию переданной карты ответов</li>
     *     <li>Сохраняет порядок ответов через LinkedHashMap</li>
     * </ul>
     */
    @JsonCreator
    public DiagnosticQuestion(
            @JsonProperty("questionText") String questionText,
            @JsonProperty("parameterName") String parameterName,
            @JsonProperty("answers") LinkedHashMap<String, Integer> answers) {
        this.questionText = questionText;
        this.parameterName = parameterName;
        this.answerValues = answers != null ? new LinkedHashMap<>(answers) : new LinkedHashMap<>();
    }

    /**
     * Возвращает текст вопроса.
     *
     * @return текст вопроса, который должен быть отображен пользователю
     */
    public String getQuestionText() {
        return questionText;
    }

    /**
     * Возвращает название параметра, связанного с вопросом.
     *
     * @return название параметра, используемое для диагностики
     */
    public String getParameterName() {
        return parameterName;
    }

    /**
     * Возвращает карту возможных ответов с соответствующими баллами.
     *
     * @return неизменяемую копию карты "ответ → балл"
     *
     * <p>Гарантирует безопасность данных, возвращая копию внутренней структуры.
     */
    public Map<String, Integer> getAnswerValues() {
        return new HashMap<>(answerValues);
    }

    /**
     * Возвращает список возможных вариантов ответа.
     *
     * @return список текстов ответов в порядке их добавления
     *
     * <p>Порядок ответов сохраняется благодаря использованию LinkedHashMap в конструкторе.
     */
    public List<String> getPossibleAnswers() {
        return new ArrayList<>(answerValues.keySet());
    }

    /**
     * Возвращает числовое значение для указанного ответа.
     *
     * @param answer текстовый вариант ответа
     * @return соответствующее значение балла или null, если ответ не найден
     */
    public Integer getValueForAnswer(String answer) {
        return answerValues.get(answer);
    }
}