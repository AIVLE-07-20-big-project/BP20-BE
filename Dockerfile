# 빌드 환경 (Gradle 포함) 설정
FROM gradle:8.5-jdk21 AS build

# 작업 디렉토리 설정
WORKDIR /app

# 의존성 캐싱을 위한 Gradle 파일 먼저 복사
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle

# Gradle 실행 권한 부여
RUN chmod +x gradlew

# 의존성 미리 다운 (캐시 최적화)
RUN ./gradlew dependencies --no-daemon || true

# 프로젝트의 모든 파일을 컨테이너 내부로 복사
COPY . .

RUN chmod +x gradlew

# 테스트는 제외하고 빌드
RUN ./gradlew clean bootJar -x test --no-daemon

# 실행 환경 (경량 JDK) 설정
FROM eclipse-temurin:21-jre

WORKDIR /app

# 빌드된 JAR 복사 (와일드카드로 복사)
# Default JAR 이름: {rootProject.name}-{build.gradle.version}.jar
# build.gradle에 archiveFileName을 작성하면, 그대로 들어감
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

# Spring Boot 실행
CMD ["java", "-jar", "app.jar"]
