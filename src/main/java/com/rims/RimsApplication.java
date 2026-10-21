package com.rims;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@SpringBootApplication
public class RimsApplication {

    public static void main(String[] args) {
        loadDotEnv();
        SpringApplication.run(RimsApplication.class, args);
        System.out.println("=========================================================");
        System.out.println(" RIMS - Rental Item Management System is RUNNING!");
        System.out.println(" Local Access:    http://localhost:8080");
        System.out.println(" Dedicated Portals:");
        System.out.println("  - Public Catalog:   http://localhost:8080/items");
        System.out.println("  - Customer Portal:  http://localhost:8080/login/buyer");
        System.out.println("  - Seller Portal:    http://localhost:8080/login/seller");
        System.out.println("  - SUDO Admin:       http://localhost:8080/login/admin");
        System.out.println("=========================================================");
    }

    private static void loadDotEnv() {
        Path envPath = Paths.get(".env");
        if (Files.exists(envPath)) {
            try {
                List<String> lines = Files.readAllLines(envPath);
                for (String line : lines) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    int idx = line.indexOf('=');
                    if (idx > 0) {
                        String key = line.substring(0, idx).trim();
                        String val = line.substring(idx + 1).trim();
                        if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'"))) {
                            val = val.substring(1, val.length() - 1);
                        }
                        if (System.getProperty(key) == null && System.getenv(key) == null) {
                            System.setProperty(key, val);
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Notice: Could not parse local .env file: " + e.getMessage());
            }
        }
    }
}
