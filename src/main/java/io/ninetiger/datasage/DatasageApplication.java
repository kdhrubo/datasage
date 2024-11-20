package io.ninetiger.datasage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;

@SpringBootApplication
public class DatasageApplication {

    public static void main(String[] args) {
        SpringApplication.run(DatasageApplication.class, args);
    }

}
