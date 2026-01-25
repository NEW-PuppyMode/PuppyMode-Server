FROM eclipse-temurin:17-jdk
COPY build/libs/*.jar app.jar
ENV JAVA_OPTS="-Xms256m -Xmx768m -XX:+UseG1GC"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app.jar --spring.config.location=file:/app/application.yml"]