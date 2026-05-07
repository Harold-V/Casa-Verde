package com.casaverde.backend.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@Configuration
public class DataSourceConfig {

    private static final String DB_DIR = "./DataBase";
    private static final String DB_PATH = DB_DIR + "/casaverde.db";

    @Bean
    DataSource dataSource() {
        try {
            Path dir = Paths.get(DB_DIR);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
        } catch (IOException e) {
            throw new RuntimeException("No se pudo crear el directorio de base de datos: " + DB_DIR, e);
        }

        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("org.sqlite.JDBC");
        ds.setUrl("jdbc:sqlite:" + DB_PATH);
        return ds;
    }
}
