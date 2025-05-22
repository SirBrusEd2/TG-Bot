package org.example.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * Класс, представляющий диагностический тест.
 * <p>
 * Содержит набор вопросов и правила для интерпретации результатов тестирования.
 * Позволяет добавлять вопросы, управлять правилами диагностики и оценивать результаты.
 * </p>
 */
public class DiagnosticTest {
    private String testName;
    private List<DiagnosticQuestion> questions;
    private Map<String, String> diagnosisMap;
    private Map<String, String> diagnosisRules;

    /**
     * Конструктор для создания теста с десериализацией из JSON.
     *
     * @param testName название теста
     * @param questions список вопросов теста
     * @param diagnosisRules правила интерпретации результатов
     *
     * <p>Особенности:
     * <ul>
     *     <li>Использует аннотации Jackson для корректной десериализации</li>
     *     <li>Создает защищенные копии переданных коллекций</li>
     *     <li>Инициализирует пустые коллекции, если переданы null-значения</li>
     * </ul>
     */
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

    /**
     * Добавляет вопрос в тест.
     *
     * @param question объект DiagnosticQuestion для добавления
     */
    public void addQuestion(DiagnosticQuestion question) {
        questions.add(question);
    }

    /**
     * Добавляет правило интерпретации результатов.
     *
     * @param scoreRange диапазон баллов (например, "0-4" или ">=10")
     * @param diagnosis текст диагноза для указанного диапазона
     */
    public void addDiagnosisRule(String scoreRange, String diagnosis) {
        diagnosisMap.put(scoreRange, diagnosis);
    }

    /**
     * Возвращает список вопросов теста.
     *
     * @return неизменяемую копию списка вопросов
     */
    public List<DiagnosticQuestion> getQuestions() {
        return new ArrayList<>(questions);
    }

    /**
     * Возвращает название теста.
     *
     * @return название теста
     */
    public String getTestName() {
        return testName;
    }

    /**
     * Оценивает результат тестирования на основе общего балла.
     *
     * @param totalScore суммарный балл, полученный в результате тестирования
     * @return текст диагноза или "Не определена", если не найдено подходящего правила
     *
     * <p>Логика работы:
     * <ol>
     *     <li>Сортирует диапазоны баллов по возрастанию</li>
     *     <li>Проверяет принадлежность балла к каждому диапазону</li>
     *     <li>Возвращает первый подходящий диагноз</li>
     * </ol>
     */
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

    /**
     * Извлекает начальное значение диапазона.
     *
     * @param range строковое представление диапазона
     * @return числовое значение начала диапазона
     * @throws NumberFormatException если диапазон имеет неверный формат
     */
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

    /**
     * Проверяет принадлежность балла к указанному диапазону.
     *
     * @param score проверяемый балл
     * @param range диапазон в формате "X-Y" или ">=X"
     * @return true если балл попадает в диапазон, иначе false
     */
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

    /**
     * Возвращает правила диагностики.
     *
     * @return карту "диапазон баллов → диагноз"
     */
    public Map<String, String> getDiagnosisRules() {
        return diagnosisRules;
    }
}