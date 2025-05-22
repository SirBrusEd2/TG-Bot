package org.example.bot;

import org.example.service.TestConfigLoader;
import org.example.model.DiagnosisSession;
import org.example.model.DiagnosticQuestion;
import org.example.model.DiagnosticTest;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.util.*;

/**
 * Telegram-бот для медицинской диагностики по шкалам APACHE II/III.
 * <p>
 * Основные функции:
 * <ul>
 *     <li>Проведение диагностических тестов</li>
 *     <li>Управление сессиями диагностики для разных пользователей</li>
 *     <li>Оценка тяжести состояния и риска летальности</li>
 *     <li>Интерактивный опрос пользователей</li>
 * </ul>
 */
public class MedicalDiagnosisBot extends TelegramLongPollingBot {
    private final Map<String, DiagnosisSession> userSessions = new HashMap<>();
    private final List<DiagnosticTest> availableTests;
    private final Map<String, String> testCommands = Map.of(
            "/apacheii", "APACHE II (Acute Physiology And Chronic Health Evaluation II)",
            "/apacheiii", "APACHE III (Acute Physiology And Chronic Health Evaluation III)"
    );

    /**
     * Конструктор бота. Загружает доступные тесты из конфигурации.
     * <p>
     * При ошибке загрузки тестов инициализирует пустой список тестов.
     */
    public MedicalDiagnosisBot() {
        List<DiagnosticTest> loadedTests;
        try {
            loadedTests = TestConfigLoader.loadTests();
            System.out.println("Loaded tests: " + loadedTests);
        } catch (IOException e) {
            e.printStackTrace();
            loadedTests = new ArrayList<>();
        }
        this.availableTests = loadedTests;
    }

    /**
     * Обрабатывает входящие обновления от Telegram.
     *
     * @param update объект Update от Telegram API
     *
     * <p>Фильтрует сообщения без текста и делегирует обработку методу processMessage.
     */
    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) return;

        String chatId = update.getMessage().getChatId().toString();
        String messageText = update.getMessage().getText();

        try {
            SendMessage response = processMessage(chatId, messageText);
            execute(response);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    /**
     * Определяет тип сообщения и вызывает соответствующий обработчик.
     *
     * @param chatId идентификатор чата
     * @param message текст сообщения пользователя
     * @return подготовленное сообщение для отправки
     */
    private SendMessage processMessage(String chatId, String message) {
        if (message.startsWith("/start")) {
            return startCommand(chatId);
        } else if (testCommands.keySet().stream().anyMatch(message::startsWith)) {
            String testName = testCommands.get(message.split(" ")[0]);
            return startTest(chatId, testName);
        } else if (message.startsWith("/cancel")) {
            return cancelSession(chatId);
        } else {
            return handleUserResponse(chatId, message);
        }
    }

    /**
     * Обрабатывает команду /start - выводит список доступных тестов.
     *
     * @param chatId идентификатор чата
     * @return сообщение с приветствием и списком команд
     */
    private SendMessage startCommand(String chatId) {
        StringBuilder messageText = new StringBuilder("Добро пожаловать в медицинский диагностический бот!\n\nДоступные тесты:\n");
        testCommands.forEach((cmd, name) -> messageText.append(cmd).append(" - ").append(name).append("\n"));
        messageText.append("\nДля отмены теста используйте /cancel");

        return createMessage(chatId, messageText.toString());
    }

    /**
     * Начинает новый диагностический тест.
     *
     * @param chatId идентификатор чата
     * @param testName название теста
     * @return первый вопрос теста или сообщение об ошибке
     */
    private SendMessage startTest(String chatId, String testName) {
        DiagnosticTest test = availableTests.stream()
                .filter(t -> t.getTestName().equals(testName))
                .findFirst()
                .orElse(null);

        if (test == null) {
            return createMessage(chatId, "Тест " + testName + " не доступен");
        }

        DiagnosisSession session = new DiagnosisSession(test);
        userSessions.put(chatId, session);

        return askNextQuestion(chatId, session);
    }

    /**
     * Формирует следующий вопрос теста с учетом логики пропуска вопросов.
     *
     * @param chatId идентификатор чата
     * @param session текущая сессия диагностики
     * @return сообщение с вопросом или сообщение об ошибке
     */
    private SendMessage askNextQuestion(String chatId, DiagnosisSession session) {
        DiagnosticQuestion nextQuestion;
        do {
            nextQuestion = session.getNextQuestion();
            if (nextQuestion == null) {
                return createMessage(chatId, "Ошибка: нет вопросов в тесте");
            }

            if (shouldSkipQuestion(session, nextQuestion)) {
                session.recordAnswer(nextQuestion.getParameterName(), 0);
                continue;
            }
            break;
        } while (true);

        StringBuilder messageText = new StringBuilder();
        messageText.append("Вопрос ").append(session.getCurrentQuestionNumber())
                .append(" из ").append(session.getTotalQuestions()).append(":\n")
                .append(nextQuestion.getQuestionText()).append("\n\n");

        List<String> possibleAnswers = nextQuestion.getPossibleAnswers();
        for (int i = 0; i < possibleAnswers.size(); i++) {
            messageText.append(i + 1).append(". ").append(possibleAnswers.get(i)).append("\n");
        }

        addParameterHints(messageText, nextQuestion.getParameterName());
        return createMessage(chatId, messageText.toString());
    }

    /**
     * Определяет, нужно ли пропустить вопрос на основе предыдущих ответов.
     *
     * @param session текущая сессия диагностики
     * @param question проверяемый вопрос
     * @return true если вопрос следует пропустить, иначе false
     */
    private boolean shouldSkipQuestion(DiagnosisSession session, DiagnosticQuestion question) {
        Integer isVentilated = session.getAnswerFor("is_ventilated");

        if (question.getParameterName().equals("aado2") && (isVentilated == null || isVentilated == 0)) {
            return true;
        }

        if (question.getParameterName().equals("pao2") && isVentilated != null && isVentilated == 1) {
            return true;
        }

        return false;
    }

    /**
     * Добавляет подсказки для специфических параметров.
     *
     * @param messageText строитель текста сообщения
     * @param parameterName название параметра
     */
    private void addParameterHints(StringBuilder messageText, String parameterName) {
        switch (parameterName) {
            case "map":
                messageText.append("\nФормула: (Систолическое АД + 2 × Диастолическое АД) / 3");
                break;
            case "respiratory_rate":
                messageText.append("\nУкажите 'да', если пациент на ИВЛ");
                break;
            case "pao2":
                messageText.append("\nНе используется при ИВЛ с FiO₂ ≥ 0.5");
                break;
            case "aado2":
                messageText.append("\nТолько для ИВЛ с FiO₂ ≥ 0.5");
                break;
        }
    }

    /**
     * Обрабатывает ответ пользователя на вопрос теста.
     *
     * @param chatId идентификатор чата
     * @param message текст ответа пользователя
     * @return следующий вопрос или результаты теста
     */
    private SendMessage handleUserResponse(String chatId, String message) {
        DiagnosisSession session = userSessions.get(chatId);
        if (session == null) {
            return createMessage(chatId, "Нет активного теста. Начните тест командой /apacheii или /apacheiii");
        }

        try {
            DiagnosticQuestion currentQuestion = session.getCurrentQuestion();
            if (currentQuestion == null) {
                return createMessage(chatId, "Ошибка: вопрос не найден");
            }

            if (currentQuestion.getParameterName().equals("respiratory_rate") &&
                    !session.hasAnswerFor("is_ventilated")) {
                return handleVentilationQuestion(chatId, session, message);
            }

            List<String> possibleAnswers = currentQuestion.getPossibleAnswers();
            int answerIndex = Integer.parseInt(message) - 1;

            if (answerIndex < 0 || answerIndex >= possibleAnswers.size()) {
                return createMessage(chatId, "Пожалуйста, выберите номер ответа из предложенных");
            }

            String selectedAnswer = possibleAnswers.get(answerIndex);
            Integer answerValue = currentQuestion.getValueForAnswer(selectedAnswer);
            session.recordAnswer(currentQuestion.getParameterName(), answerValue);

            return checkTestCompletion(chatId, session);
        } catch (NumberFormatException e) {
            return createMessage(chatId, "Пожалуйста, введите номер ответа (1, 2, 3 и т.д.)");
        }
    }

    /**
     * Специальная обработка вопроса об ИВЛ.
     */
    private SendMessage handleVentilationQuestion(String chatId, DiagnosisSession session, String message) {
        if (message.equalsIgnoreCase("да") || message.equalsIgnoreCase("нет")) {
            boolean isVentilated = message.equalsIgnoreCase("да");
            session.recordAnswer("is_ventilated", isVentilated ? 1 : 0);
            return askNextQuestion(chatId, session);
        } else {
            return createMessage(chatId, "Пациент на ИВЛ? (ответьте 'да' или 'нет')");
        }
    }

    /**
     * Проверяет завершение теста и возвращает результаты или следующий вопрос.
     */
    private SendMessage checkTestCompletion(String chatId, DiagnosisSession session) {
        if (session.isComplete()) {
            int totalScore = session.calculateTotalScore();
            String diagnosis = session.getDiagnosisResult();
            String mortalityRisk = estimateMortalityRisk(totalScore, session.getCurrentTest().getTestName());

            userSessions.remove(chatId);
            return createMessage(chatId, formatTestResults(totalScore, diagnosis, mortalityRisk));
        }
        return askNextQuestion(chatId, session);
    }

    /**
     * Форматирует результаты теста для вывода пользователю.
     */
    private String formatTestResults(int score, String diagnosis, String mortalityRisk) {
        return "Диагностика завершена.\n\n" +
                "Общий балл: " + score + "\n" +
                "Оценка тяжести: " + diagnosis + "\n" +
                "Прогнозируемый риск летальности: " + mortalityRisk + "\n\n" +
                "Для нового теста используйте /apacheii или /apacheiii";
    }

    /**
     * Оценивает риск летальности на основе баллов и типа теста.
     */
    private String estimateMortalityRisk(int score, String testName) {
        if (testName.equals("APACHE II (Acute Physiology And Chronic Health Evaluation II)")) {
            if (score < 10) return "~15%";
            else if (score < 20) return "~25%";
            else if (score < 30) return "~50%";
            else return ">80%";
        } else if (testName.equals("APACHE III (Acute Physiology And Chronic Health Evaluation III)")) {
            if (score < 30) return "~10%";
            else if (score < 45) return "~20%";
            else if (score < 55) return "~30%";
            else if (score < 65) return "~50%";
            else if (score < 75) return "~65%";
            else if (score < 85) return "~75%";
            else return ">85%";
        }
        return "Неизвестно";
    }

    /**
     * Отменяет текущую сессию тестирования.
     */
    private SendMessage cancelSession(String chatId) {
        userSessions.remove(chatId);
        return createMessage(chatId, "Тест отменен. Для начала нового теста используйте /apacheii или /apacheiii");
    }

    /**
     * Создает объект сообщения для отправки.
     */
    private SendMessage createMessage(String chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        return message;
    }

    /**
     * Возвращает имя бота.
     */
    @Override
    public String getBotUsername() {
        return "Patient_condition_bot";
    }

    /**
     * Возвращает токен бота.
     */
    @Override
    public String getBotToken() {
        return "7118289957:AAF7sOeJQcsefUPJnOoWA2QlB2pI8GVbZX8";
    }
}