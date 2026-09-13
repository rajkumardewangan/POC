# Men's Salon Management Web Application

A Spring Boot web application for managing salon customers, barbers, services, and appointments.

## Prerequisites

- Java 17 or later (JDK)
- Maven (or use the included Maven Wrapper if present)

## Running the Server

### Option 1: Using Maven

From the project root directory, run:

```powershell
mvn spring-boot:run
```

### Option 2: Build and run the JAR

```powershell
mvn clean package
java -jar target/mens-salon-1.0.0.jar
```

## Accessing the Application

Once the server starts, it will be available at:

- Application: [http://localhost:8080](http://localhost:8080)
- H2 Database Console: [http://localhost:8080/h2-console](http://localhost:8080/h2-console)
  - JDBC URL: `jdbc:h2:file:./data/salondb`
  - Username: `sa`
  - Password: *(leave blank)*

## Notes

- The app uses a file-based H2 database stored in the `data/` folder, so data persists across restarts.
- Server port defaults to `8080`. To change it, edit `server.port` in [src/main/resources/application.properties](src/main/resources/application.properties).
- Thymeleaf template caching is disabled for easier development.
