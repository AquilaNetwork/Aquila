FROM maven:3-openjdk-11 as builder

WORKDIR /work
COPY ./ /work/
RUN mvn clean package

###
FROM openjdk:11

RUN useradd -r -u 1000 -g users aquila && \
    mkdir /usr/local/aquila /aquila && \
    chown 1000:100 /aquila

COPY --from=builder /work/log4j2.properties /usr/local/aquila/
COPY --from=builder /work/target/aquila*.jar /usr/local/aquila/aquila.jar

USER 1000:100

EXPOSE 12391 12392
HEALTHCHECK --start-period=5m CMD curl -sf http://127.0.0.1:12391/admin/info || exit 1

WORKDIR /aquila
VOLUME /aquila

ENTRYPOINT ["java"]
CMD ["-Djava.net.preferIPv4Stack=false", "-jar", "/usr/local/aquila/aquila.jar"]
