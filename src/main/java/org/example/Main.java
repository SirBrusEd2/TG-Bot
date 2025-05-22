package org.example;

import org.example.bot.MedicalDiagnosisBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

/**
 * Главный класс приложения для запуска Telegram-бота медицинской диагностики.
 * <p>
 * Содержит точку входа в приложение и отвечает за инициализацию и регистрацию бота
 * в Telegram API.
 * </p>
 */
public class Main {
    /**
     * Точка входа в приложение.
     * <p>
     * Создает экземпляр TelegramBotsApi и регистрирует бота медицинской диагностики.
     * </p>
     *
     * @param args аргументы командной строки (не используются)
     *
     * <p>Логика работы:
     * <ol>
     *     <li>Создает экземпляр TelegramBotsApi с DefaultBotSession</li>
     *     <li>Регистрирует экземпляр MedicalDiagnosisBot</li>
     *     <li>Выводит сообщение об успешном запуске в консоль</li>
     *     <li>Обрабатывает возможные исключения TelegramApiException</li>
     * </ol>
     *
     * <p>Пример использования:
     * <pre>{@code
     * // Запуск приложения
     * java -jar MedicalDiagnosisBot.jar
     * }</pre>
     *
     * @see MedicalDiagnosisBot
     * @see TelegramBotsApi
     * @see DefaultBotSession
     */
    public static void main(String[] args) {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new MedicalDiagnosisBot());
            System.out.println("Бот успешно запущен!");
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}