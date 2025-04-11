# Используем официальный образ Java как базовый
FROM openjdk:17-jdk-slim

# Устанавливаем рабочий каталог в контейнере
WORKDIR /app

# Копируем JAR файл в контейнер
COPY build/libs/FomsService-0.0.1-SNAPSHOT.jar app.jar

# Открываем порт (если необходимо)
EXPOSE 8089

# Команда для запуска JAR файла
ENTRYPOINT ["java", "-jar", "/app/app.jar"]