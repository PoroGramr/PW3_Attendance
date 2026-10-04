# Playwright 버전은 build.gradle의 Java 의존성과 반드시 일치해야 한다.
FROM mcr.microsoft.com/playwright/java:v1.63.0-noble

# 워크스페이스에서 복사해 온 app.jar 을 이미지에 복사
COPY app.jar app.jar

ENTRYPOINT ["java","-jar","app.jar"]
