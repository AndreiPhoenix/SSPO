package com.ordermanagement.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI orderManagementOpenAPI() {
        // Настройка серверов (для разработки и продакшена)
        Server devServer = new Server();
        devServer.setUrl("http://localhost:8080");
        devServer.setDescription("Локальный сервер разработки");

        Server prodServer = new Server();
        prodServer.setUrl("https://api.ordermanagement.com");
        prodServer.setDescription("Продакшен сервер");

        // Контактная информация
        Contact contact = new Contact();
        contact.setName("Отдел разработки Order Management System");
        contact.setEmail("support@ordermanagement.com");
        contact.setUrl("https://ordermanagement.com");

        // Лицензия
        License license = new License()
                .name("MIT License")
                .url("https://opensource.org/licenses/MIT");

        // Основная информация об API
        Info info = new Info()
                .title("Order Management System API")
                .version("1.0.0")
                .description("""
                        ## REST API для системы управления заказами
                        
                        ### Описание системы
                        Система автоматизации процессов приёма, обработки и оплаты заказов для интернет-магазина.
                        
                        ### Основные функции:
                        - Управление покупателями (CRUD операции)
                        - Управление товарами и складскими остатками
                        - Создание и обработка заказов
                        - Резервирование товаров
                        - Обработка платежей
                        
                        ### Технологии:
                        - **Spring Boot 4.0.0**
                        - **Java 17**
                        - **PostgreSQL**
                        - **Spring Data JPA**
                        
                        ### Авторизация
                        Для работы с API требуется авторизация через Bearer токен.
                        """)
                .termsOfService("https://ordermanagement.com/terms")
                .contact(contact)
                .license(license);

        return new OpenAPI()
                .info(info)
                .servers(List.of(devServer, prodServer));
    }
}