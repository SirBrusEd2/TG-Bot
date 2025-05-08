package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class TestConfigLoader {
    private static final String CONFIG_FILE = "/tests_config.json";

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
