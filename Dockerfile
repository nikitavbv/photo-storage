FROM node:alpine
COPY frontend /frontend
RUN cd /frontend && npm install && npm run-script build-prod

FROM gradle:jdk11
COPY . /app
RUN cd /app && gradle jar

FROM openjdk:11
COPY --from=0 /frontend/dist /frontend/dist
COPY --from=1 /app/build/libs/photo-storage-0.1.jar /app.jar
EXPOSE 8080
CMD ["java", "-jar", "/app.jar"]