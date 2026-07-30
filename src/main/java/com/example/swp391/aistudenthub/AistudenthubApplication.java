package com.example.swp391.aistudenthub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class AistudenthubApplication {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(AistudenthubApplication.class);
		java.util.Map<String, Object> envProperties = loadEnvMap();
		if (!envProperties.isEmpty()) {
			app.setDefaultProperties(envProperties);
		}
		app.run(args);
	}

	private static java.util.Map<String, Object> loadEnvMap() {
		java.util.Map<String, Object> props = new java.util.HashMap<>();
		File envFile = new File(".env");
		if (envFile.exists()) {
			try (BufferedReader reader = new BufferedReader(new FileReader(envFile))) {
				String line;
				while ((line = reader.readLine()) != null) {
					line = line.trim();
					if (line.isEmpty() || line.startsWith("#")) {
						continue;
					}
					int eqIdx = line.indexOf('=');
					if (eqIdx > 0) {
						String key = line.substring(0, eqIdx).trim();
						String value = line.substring(eqIdx + 1).trim();
						// Remove quotes if present
						if (value.startsWith("\"") && value.endsWith("\"")) {
							value = value.substring(1, value.length() - 1);
						} else if (value.startsWith("'") && value.endsWith("'")) {
							value = value.substring(1, value.length() - 1);
						}
						props.put(key, value);
					}
				}
			} catch (IOException e) {
				System.err.println("Warning: Failed to load .env file: " + e.getMessage());
			}
		}
		return props;
	}

}
