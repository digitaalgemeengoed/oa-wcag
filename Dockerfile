# oa-wcag: de toegankelijkheidscontrole van de Office Assistent.
# Bouw: docker build -t oa/wcag .
# Start: docker run -e WCAG_DIENSTSLEUTEL=... -p 4400:4400 oa/wcag

FROM maven:3.9-eclipse-temurin-21 AS bouw
WORKDIR /bron
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package

FROM eclipse-temurin:21-jre-alpine
# Lettertypen: POI gebruikt hier en daar java.awt (kleuren, maten van tekst); zonder lettertypen kan dat op Alpine vastlopen.
RUN apk add --no-cache fontconfig ttf-dejavu \
 && addgroup -S wcag && adduser -S wcag -G wcag
WORKDIR /app
COPY --from=bouw /bron/target/oa-wcag.jar ./oa-wcag.jar
USER wcag
EXPOSE 4400
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -Djava.awt.headless=true"
HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
  CMD wget -qO- http://127.0.0.1:4400/gezondheid || exit 1
CMD ["java", "-jar", "oa-wcag.jar"]
