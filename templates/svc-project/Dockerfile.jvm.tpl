FROM eclipse-temurin:21-jre as runner
WORKDIR /work/
COPY target/quarkus-app/ /work/
ENTRYPOINT ["java","-jar","/work/quarkus-run.jar"]
