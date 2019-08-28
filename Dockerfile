FROM openjdk:11-jdk-slim as builder
WORKDIR /etc/premium-synchronizer
COPY ./ ./
USER root
RUN chmod +x ./gradlew
RUN ./gradlew build --stacktrace

FROM openjdk:11-jre-slim
COPY --from=builder ./etc/premium-synchronizer/build/libs/ .
ENTRYPOINT java \
	-XX:+UseG1GC \
	-XX:+UseStringDeduplication \
	-Xmx$HEAP_SIZE \
	-jar \
	./premium-synchronizer.jar