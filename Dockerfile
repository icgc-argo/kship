FROM openjdk:11-jdk as builder
WORKDIR /usr/src/app
# we add only what we need
# we add it in steps to utilize docker cache
ADD pom.xml .
ADD .mvn ./.mvn
ADD mvnw .
RUN ./mvnw verify clean --fail-never
ADD src ./src
RUN ./mvnw clean package

FROM openjdk:11-jre-slim
COPY --from=builder /usr/src/app/target/kship-*.jar /usr/bin/kship.jar
CMD ["java", "-ea", "-jar", "/usr/bin/kship.jar"]
EXPOSE 3518