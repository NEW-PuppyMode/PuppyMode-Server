FROM eclipse-temurin:17-jdk
ARG JAR_FILE
COPY ${JAR_FILE} app.jar
VOLUME /tmp
ENV JAVA_OPTS="-Xms200m -Xmx400m -XX:+UseG1GC"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app.jar"]