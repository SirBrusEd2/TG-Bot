package org.example;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class MedicalDiagnosisBot extends TelegramLongPollingBot {
    private final Map<String, DiagnosisSession> userSessions = new HashMap<>();
    private final List<DiagnosticTest> availableTests;
    private final Map<String, String> testCommands = Map.of(
            "/apacheii", "APACHE II (Acute Physiology And Chronic Health Evaluation II)",
            "/apacheiii", "APACHE III (Acute Physiology And Chronic Health Evaluation III)"
    );

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

    private SendMessage startCommand(String chatId) {
        StringBuilder messageText = new StringBuilder("Добро пожаловать в медицинский диагностический бот!\n\nДоступные тесты:\n");
        testCommands.forEach((cmd, name) -> messageText.append(cmd).append(" - ").append(name).append("\n"));
        messageText.append("\nДля отмены теста используйте /cancel");

        return createMessage(chatId, messageText.toString());
    }

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

    private SendMessage askNextQuestion(String chatId, DiagnosisSession session) {
        DiagnosticQuestion nextQuestion;
        do {
            nextQuestion = session.getNextQuestion();
            if (nextQuestion == null) {
                return createMessage(chatId, "Ошибка: нет вопросов в тесте");
            }

            // Пропускаем вопросы, которые не должны задаваться
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

    private boolean shouldSkipQuestion(DiagnosisSession session, DiagnosticQuestion question) {
        Integer isVentilated = session.getAnswerFor("is_ventilated");

        // Пропускаем A-aDO2 если пациент не на ИВЛ
        if (question.getParameterName().equals("aado2") && (isVentilated == null || isVentilated == 0)) {
            return true;
        }

        // Пропускаем PaO2 если пациент на ИВЛ
        if (question.getParameterName().equals("pao2") && isVentilated != null && isVentilated == 1) {
            return true;
        }

        return false;
    }

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

            // Обработка вопроса про ИВЛ
            if (currentQuestion.getParameterName().equals("respiratory_rate") &&
                    !session.hasAnswerFor("is_ventilated")) {
                return handleVentilationQuestion(chatId, session, message);
            }

            // Стандартная обработка вопроса
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

    private SendMessage handleVentilationQuestion(String chatId, DiagnosisSession session, String message) {
        if (message.equalsIgnoreCase("да") || message.equalsIgnoreCase("нет")) {
            boolean isVentilated = message.equalsIgnoreCase("да");
            session.recordAnswer("is_ventilated", isVentilated ? 1 : 0);
            return askNextQuestion(chatId, session);
        } else {
            return createMessage(chatId, "Пациент на ИВЛ? (ответьте 'да' или 'нет')");
        }
    }

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

    private String formatTestResults(int score, String diagnosis, String mortalityRisk) {
        return "Диагностика завершена.\n\n" +
                "Общий балл: " + score + "\n" +
                "Оценка тяжести: " + diagnosis + "\n" +
                "Прогнозируемый риск летальности: " + mortalityRisk + "\n\n" +
                "Для нового теста используйте /apacheii или /apacheiii";
    }

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

    private SendMessage cancelSession(String chatId) {
        userSessions.remove(chatId);
        return createMessage(chatId, "Тест отменен. Для начала нового теста используйте /apacheii или /apacheiii");
    }

    private SendMessage createMessage(String chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        return message;
    }

    @Override
    public String getBotUsername() {
        return "MedicalDiagnosisBot";
    }

    @Override
    public String getBotToken() {
        return "7599633503:AAGZ6N_MRq1a7fH8ay9VkFdZU43Rw8lSAi4";
    }
}

