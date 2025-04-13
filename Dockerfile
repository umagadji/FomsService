# Используем минимальный образ с OpenJDK 17
FROM openjdk:17-jdk-slim

# Устанавливаем необходимые зависимости для работы с графикой и шрифты
RUN apt-get update && apt-get install -y \
    libfreetype6 \
    fontconfig \
    fonts-dejavu \
    && rm -rf /var/lib/apt/lists/*

# Устанавливаем рабочий каталог в контейнере
WORKDIR /app

# Копируем JAR файл в контейнер
COPY FomsService-0.0.1-SNAPSHOT.jar app.jar

# Открываем порт 8089
EXPOSE 8089

# Команда для запуска приложения в headless-режиме
ENTRYPOINT ["java", "-Djava.awt.headless=true", "-Dsun.java2d.headless=true", "-jar", "/app/app.jar"]