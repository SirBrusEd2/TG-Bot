// DiagnosisSession.java
package org.example.model;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * Класс, представляющий сессию диагностики.
 * <p>
 * Отвечает за управление процессом прохождения диагностического теста,
 * включая навигацию по вопросам, сбор ответов и вычисление итогового результата.
 * </p>
 */
public class DiagnosisSession {
    private DiagnosticTest currentTest;
    private Map<String, Integer> collectedAnswers;
    private int currentQuestionIndex;

    /**
     * Создает новую сессию диагностики для указанного теста.
     *
     * @param test диагностический тест, который будет использоваться в сессии
     */
    public DiagnosisSession(DiagnosticTest test) {
        this.currentTest = test;
        this.collectedAnswers = new HashMap<>();
        this.currentQuestionIndex = 0;
    }

    /**
     * Возвращает следующий вопрос теста.
     *
     * @return следующий вопрос или null, если тест завершен
     *
     * <p>Особенности работы:
     * <ul>
     *     <li>Автоматически увеличивает счетчик текущего вопроса</li>
     *     <li>Возвращает null, если все вопросы пройдены</li>
     * </ul>
     */
    public DiagnosticQuestion getNextQuestion() {
        if (currentQuestionIndex >= currentTest.getQuestions().size()) {
            return null;
        }
        return currentTest.getQuestions().get(currentQuestionIndex++);
    }

    /**
     * Записывает ответ пользователя для указанного параметра.
     *
     * @param parameterName название параметра (характеристики)
     * @param value значение ответа
     *
     * <p>Ответы сохраняются во внутренней карте и используются для расчета итогового результата.
     */
    public void recordAnswer(String parameterName, int value) {
        collectedAnswers.put(parameterName, value);
    }

    /**
     * Проверяет, завершена ли сессия диагностики.
     *
     * @return true если все вопросы теста пройдены, иначе false
     */
    public boolean isComplete() {
        return currentQuestionIndex >= currentTest.getQuestions().size();
    }

    /**
     * Вычисляет и возвращает итоговый диагноз на основе собранных ответов.
     *
     * @return строку с описанием диагноза или "Неизвестная степень тяжести", если не найдено подходящего правила
     *
     * <p>Логика работы:
     * <ol>
     *     <li>Вычисляет общий балл как сумму всех ответов</li>
     *     <li>Сортирует правила диагностики по возрастанию пороговых значений</li>
     *     <li>Находит первое подходящее правило для полученного балла</li>
     *     <li>Возвращает соответствующий диагноз</li>
     * </ol>
     */
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

    /**
     * Проверяет, соответствует ли балл указанному правилу.
     *
     * @param score проверяемый балл
     * @param ruleKey ключ правила (например, "≥10" или "5-8")
     * @return true если балл соответствует правилу, иначе false
     */
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

    /**
     * Сравнивает два ключа правил для сортировки.
     *
     * @param key1 первый ключ правила
     * @param key2 второй ключ правила
     * @return результат сравнения числовых значений ключей
     */
    private int compareRuleKeys(String key1, String key2) {
        // Извлекаем числовые значения для сравнения
        int val1 = extractMaxValue(key1);
        int val2 = extractMaxValue(key2);
        return Integer.compare(val1, val2);
    }

    /**
     * Извлекает максимальное числовое значение из ключа правила.
     *
     * @param key ключ правила (например, "≥10" или "5-8")
     * @return числовое значение, представляющее верхнюю границу правила
     */
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

    /**
     * Возвращает номер текущего вопроса (начиная с 1).
     *
     * @return номер текущего вопроса
     */
    public int getCurrentQuestionNumber() {
        return currentQuestionIndex;
    }

    /**
     * Возвращает общее количество вопросов в тесте.
     *
     * @return общее количество вопросов
     */
    public int getTotalQuestions() {
        return currentTest.getQuestions().size();
    }

    /**
     * Возвращает текущий вопрос теста.
     *
     * @return текущий вопрос или null, если тест еще не начат или завершен
     */
    public DiagnosticQuestion getCurrentQuestion() {
        if (currentQuestionIndex == 0 || currentQuestionIndex > currentTest.getQuestions().size()) {
            return null;
        }
        return currentTest.getQuestions().get(currentQuestionIndex - 1);
    }

    /**
     * Возвращает список всех вопросов теста.
     *
     * @return неизменяемый список вопросов
     */
    public List<DiagnosticQuestion> getQuestions() {
        return currentTest.getQuestions();
    }

    /**
     * Вычисляет общий балл на основе собранных ответов.
     *
     * @return сумму всех значений ответов
     */
    public int calculateTotalScore() {
        return collectedAnswers.values().stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * Возвращает копию карты собранных ответов.
     *
     * @return новая карта с ответами пользователя
     */
    public Map<String, Integer> getCollectedAnswers() {
        return new HashMap<>(collectedAnswers);
    }

    /**
     * Возвращает текущий диагностический тест.
     *
     * @return объект DiagnosticTest, используемый в сессии
     */
    public DiagnosticTest getCurrentTest() {
        return currentTest;
    }

    /**
     * Проверяет, есть ли ответ для указанного параметра.
     *
     * @param parameterName название параметра
     * @return true если ответ для параметра существует, иначе false
     */
    public boolean hasAnswerFor(String parameterName) {
        return collectedAnswers.containsKey(parameterName);
    }

    /**
     * Возвращает значение ответа для указанного параметра.
     *
     * @param parameterName название параметра
     * @return значение ответа или null, если ответа нет
     */
    public Integer getAnswerFor(String parameterName) {
        return collectedAnswers.get(parameterName);
    }
}