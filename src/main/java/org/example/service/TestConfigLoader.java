package org.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.example.model.DiagnosticTest;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Класс для загрузки конфигурации диагностических тестов из JSON-файла.
 * <p>
 * Использует библиотеку Jackson для десериализации JSON-данных в объекты Java.
 * </p>
 */
public class TestConfigLoader {
    /**
     * Путь к файлу конфигурации в ресурсах
     */
    private static final String CONFIG_FILE = "/tests_config.json";

    /**
     * Загружает список диагностических тестов из JSON-файла конфигурации.
     *
     * @return список объектов DiagnosticTest
     * @throws IOException если возникла ошибка чтения файла или файл не найден
     *
     * <p>Логика работы:
     * <ol>
     *     <li>Открывает поток для чтения файла из ресурсов</li>
     *     <li>Проверяет существование файла</li>
     *     <li>Десериализует JSON в список объектов DiagnosticTest</li>
     *     <li>Автоматически закрывает поток после чтения</li>
     * </ol>
     *
     * <p>Требования к файлу конфигурации:
     * <ul>
     *     <li>Должен находиться в директории ресурсов</li>
     *     <li>Должен содержать валидный JSON-массив тестов</li>
     *     <li>Структура должна соответствовать классу DiagnosticTest</li>
     * </ul>
     *
     * @see DiagnosticTest
     * @see ObjectMapper
     */
    public static List<DiagnosticTest> loadTests() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream is = TestConfigLoader.class.getResourceAsStream(CONFIG_FILE)) {
            if (is == null) {
                throw new IOException("Конфигурационный файл не найден: " + CONFIG_FILE);
            }
            return mapper.readValue(is, new TypeReference<List<DiagnosticTest>>() {});
        }
    }
}