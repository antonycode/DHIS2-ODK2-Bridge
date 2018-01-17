FROM maven:3 as compiler

COPY . /dhis2-sync

WORKDIR /dhis2-sync

RUN mvn validate package && \
    mv dhis2-sync-cli/target/*-jar-with-dependencies.jar /dhis2-sync.jar


FROM openjdk:8-jre-slim

COPY --from=compiler /dhis2-sync.jar /dhis2-sync.jar

ENTRYPOINT java $JAVA_OPTS -jar /dhis2-sync.jar