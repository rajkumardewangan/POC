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
- Sign-in is allowed only for mobile numbers with an existing profile. Configure `SALON_BOOTSTRAP_ADMIN_NAME` and `SALON_BOOTSTRAP_ADMIN_PHONE` before the first run to create the initial admin; after that, admins manage profiles and assign roles from **Manage Users**. Sign-in currently checks the registered mobile number but does not verify ownership with an OTP.

## WhatsApp Assistant (Meta Cloud API)

The assistant uses the Meta WhatsApp Cloud API webhook at `/webhooks/whatsapp`. The Spring Boot server must be reachable from Meta over HTTPS. Set the following environment variables before starting the app; keep the access token and app secret out of source control:

- `WHATSAPP_VERIFY_TOKEN`: a private value you choose and enter in Meta webhook configuration.
- `WHATSAPP_APP_SECRET`: the Meta app secret used to verify webhook signatures.
- `WHATSAPP_ACCESS_TOKEN`: Meta Graph API access token for sending replies.
- `WHATSAPP_PHONE_NUMBER_ID`: the WhatsApp Business phone number ID.
- `WHATSAPP_OWNER_NUMBERS`: comma-separated owner phone numbers in international format.
- `WHATSAPP_GRAPH_API_VERSION`: optional Graph API version; defaults to `v22.0`.

Subscribe the Meta webhook to the `messages` field and use `https://<public-host>/webhooks/whatsapp` as the callback URL. Staff phone numbers must be saved on their barber records, preferably in international format. The bot does not reply to unrecognized numbers.

Staff commands:

- `START <appointment ID>` starts an appointment assigned to that staff member and marks it in progress.
- `WORKING <service or task>` records an unlinked activity, which also appears on the dashboard.
- `DONE [appointment ID]` completes the active work entry or the specified assigned appointment.
- `HELP` lists the commands.

The owner can ask supported dashboard questions about today's appointments, revenue, staff attendance, payroll, active work, customer count, and services. This first version answers locally from the salon database and does not require or send data to an AI provider. Natural-language AI responses require a separately configured provider and API key.
