FROM maven:3 as compiler

COPY . /dhis2-odk2-bridge

WORKDIR /dhis2-odk2-bridge

RUN mvn clean validate package && \
    mv dhis2-odk2-bridge-cli/target/*-jar-with-dependencies.jar /dhis2-odk2-bridge.jar


FROM openjdk:8-jre-slim

COPY --from=compiler /dhis2-odk2-bridge.jar /dhis2-odk2-bridge.jar

ENTRYPOINT java $JAVA_OPTS -jar /dhis2-odk2-bridge.jar