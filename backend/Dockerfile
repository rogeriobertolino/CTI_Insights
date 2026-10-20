FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build
COPY backend/pom.xml .
COPY backend/src src
RUN mvn -q -DskipTests package

# Python executa ETL com openpyxl e estatística com SciPy.
FROM eclipse-temurin:25-jre-noble
RUN apt-get update && apt-get install -y --no-install-recommends python3 python3-pip && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
COPY python /app/python
COPY analytics-python /app/analytics-python
RUN pip3 install --break-system-packages --no-cache-dir -r /app/python/requirements.txt
ENTRYPOINT ["java","-jar","/app/app.jar"]
