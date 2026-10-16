package com.example.papeleriacampus;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConnection {
    public static Connection getConnection() throws SQLException {
        Properties props = new Properties();
        java.nio.file.Path path = java.nio.file.Path.of(System.getProperty("campus.config", "database.properties"));
        if (java.nio.file.Files.exists(path)) {
            try (InputStream input = java.nio.file.Files.newInputStream(path)) {
                props.load(input);
            } catch (IOException e) {
                throw new SQLException("No se pudo leer la configuración local.", "08001", e);
            }
        }
        Properties credentials = new Properties();
        credentials.setProperty("user", value(props, "db.user", "CAMPUS_DB_USER", "postgres"));
        credentials.setProperty("password", value(props, "db.password", "CAMPUS_DB_PASSWORD", ""));
        credentials.setProperty("connectTimeout", "5");
        credentials.setProperty("socketTimeout", "10");
        return DriverManager.getConnection(value(props, "db.url", "CAMPUS_DB_URL",
                "jdbc:postgresql://localhost:5432/papeleria_campus"), credentials);
    }

    private static String value(Properties p, String key, String env, String fallback) {
        String value = System.getenv(env);
        return value != null ? value : p.getProperty(key, fallback);
    }
}
