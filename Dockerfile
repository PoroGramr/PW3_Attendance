# attendance-deploy/Dockerfile
FROM eclipse-temurin:17-jdk-jammy

# 워크스페이스에서 복사해 온 app.jar 을 이미지에 복사
COPY app.jar /app.jar

ENTRYPOINT ["java","-jar","/app.jar"]