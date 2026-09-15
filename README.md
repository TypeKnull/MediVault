# MediVault

AI-powered personal health records with OCR and Gemini.

MediVault is a health-record management backend built with Spring Boot. It allows patients to upload medical reports, extract information using OCR, store structured report data in MongoDB, and generate AI-powered summaries using Google Gemini.

## Features

- JWT-based authentication
- Patient and Doctor roles
- Patient profile management
- Doctor profile management
- Doctor search by specialization
- Medical report upload
- OCR-based text extraction
- Medical document classification
- Structured medical field extraction
- MongoDB report storage
- AI-powered report summaries using Gemini
- AI health assistant
- Patient-controlled doctor permissions
- Patient-doctor chat
- WebSocket-based messaging
- Refresh-token authentication

---

# Tech Stack

| Component | Technology |
|---|---|
| Backend | Spring Boot |
| Language | Java |
| Build Tool | Maven |
| Database | MongoDB |
| Authentication | JWT |
| Password Hashing | BCrypt |
| OCR | OCR.space |
| AI | Google Gemini |
| API | REST |
| Real-time Chat | WebSocket / STOMP |

---

# Requirements

Install the following before running the project:

- Java 17 or later
- MongoDB
- Git
- Internet connection
- OCR.space API key
- Google Gemini API key

You can verify Java and Maven with:

```bash
java -version
./mvnw -version

On Windows:

mvnw.cmd -version
1. Clone the Repository
git clone <YOUR_GITHUB_REPOSITORY_URL>
cd MediVault
2. Configure MongoDB

MediVault uses MongoDB.

The default local database configuration is:

mongodb://localhost:27017/medivault

Make sure MongoDB is running before starting the backend.

You can also use MongoDB Atlas by providing your own MongoDB connection string.

3. Configure Environment Variables

Do not put API keys, JWT secrets, or credentials directly into the source code.

Set the following environment variables.

Required
MONGODB_URI=mongodb://localhost:27017/medivault

JWT_SECRET=your-long-random-secret

OCR_SPACE_API_KEY=your-ocr-space-api-key

AI_API_KEY=your-gemini-api-key
Optional
PORT=8080

JWT_ACCESS_EXPIRATION_MS=900000

JWT_REFRESH_EXPIRATION_MS=1209600000

MEDIVAULT_UPLOAD_DIR=uploads/reports

CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173

OCR_SPACE_API_URL=https://api.ocr.space/parse/image

AI_API_URL=https://generativelanguage.googleapis.com/v1beta

AI_MODEL=gemini-3.6-flash

CV_SERVICE_URL=

FCM_CREDENTIALS_PATH=

The application already provides development defaults for values that do not require secrets.

4. OCR Configuration

MediVault uses OCR.space for extracting text from uploaded medical reports.

Set:

OCR_SPACE_API_KEY=your-ocr-space-api-key

The backend sends the report image to OCR.space and receives the extracted text.

The OCR pipeline then performs:

Medical Report
      ↓
OCR
      ↓
Extracted Text
      ↓
Document Classification
      ↓
Structured Field Extraction
      ↓
MongoDB
5. Gemini AI Configuration

MediVault uses Google Gemini for AI-powered functionality.

Set:

AI_API_KEY=your-gemini-api-key

Default API configuration:

AI_API_URL=https://generativelanguage.googleapis.com/v1beta
AI_MODEL=gemini-3.6-flash

The AI integration is used for:

Medical report summaries
AI assistant conversations
Processing report information for user-friendly explanations

Do not commit your Gemini API key to GitHub.

6. Run the Backend
Windows

From the project root:

mvnw.cmd spring-boot:run
Linux / macOS / WSL
./mvnw spring-boot:run

The backend runs on:

http://localhost:8080
7. Build the Project

Windows:

mvnw.cmd clean package

Linux / macOS / WSL:

./mvnw clean package

Run the generated JAR with:

java -jar target/*.jar
API Endpoints
Authentication
Register
POST /api/auth/register
Login
POST /api/auth/login
Refresh Token
POST /api/auth/refresh-token

Authenticated endpoints require:

Authorization: Bearer <ACCESS_TOKEN>
Patient APIs
Get Current Patient
GET /api/patients/me
Update Patient
PUT /api/patients/me
Doctor APIs
Get Current Doctor
GET /api/doctors/me
Update Doctor
PUT /api/doctors/me
Search Doctors
GET /api/doctors
Search by Specialization
GET /api/doctors?specialization=Cardiologist
Medical Reports
Upload Report
POST /api/reports

Use:

multipart/form-data

with the field:

file

Example using curl:

curl -X POST http://localhost:8080/api/reports \
  -H "Authorization: Bearer <ACCESS_TOKEN>" \
  -F "file=@report.jpg"

The upload workflow performs:

Upload
   ↓
OCR
   ↓
Document Classification
   ↓
Field Extraction
   ↓
MongoDB Storage
Get My Reports
GET /api/reports/my
Get Report
GET /api/reports/{reportId}
Update Report
PUT /api/reports/{reportId}
AI APIs
AI Chat
POST /api/ai/chat
Generate Report Summary
GET /api/ai/reports/{reportId}/summary

The report summary is generated from the stored report information.

Doctor Permission APIs

Patients control access to their reports.

Request Permission
POST /api/permissions/request

Request body:

{
  "patientId": "<PATIENT_ID>"
}
View Patient Permission Requests
GET /api/permissions/patient
Approve Request
POST /api/permissions/{requestId}/approve
Reject Request
POST /api/permissions/{requestId}/reject

Doctors cannot access protected patient reports until the required permission has been approved.

Chat APIs
Get Chat
GET /api/chat/{participantId}
Send Message
POST /api/chat/messages

Real-time messaging uses:

/chat.send

via WebSocket/STOMP.

Patient approval is required before protected doctor-patient communication is allowed.

Device Token APIs
Register Device Token
POST /api/device-tokens
Delete Device Token
DELETE /api/device-tokens

These endpoints are used for push notification device registration.

Project Structure
MediVault/
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/medivault/
│   │   │       ├── controller/
│   │   │       ├── service/
│   │   │       ├── repository/
│   │   │       ├── model/
│   │   │       ├── dto/
│   │   │       ├── client/
│   │   │       ├── config/
│   │   │       └── security/
│   │   │
│   │   └── resources/
│   │       └── application.yml
│   │
│   └── test/
│
├── frontend-examples/
│
├── .gitignore
├── mvnw
├── mvnw.cmd
├── pom.xml
└── README.md
Application Architecture
                    React Native
                         │
                         │ HTTP / REST
                         ▼
                 ┌─────────────────┐
                 │  Spring Boot    │
                 │     Backend     │
                 └────────┬────────┘
                          │
             ┌────────────┼─────────────┐
             │            │             │
             ▼            ▼             ▼
         MongoDB       OCR.space     Gemini AI
             │            │             │
             │            │             │
             └────────────┼─────────────┘
                          │
                    Processed Data
Medical Report Processing

A typical report-processing request follows:

Patient uploads report
        ↓
Spring Boot API
        ↓
OCR.space
        ↓
Extracted text
        ↓
Document classification
        ↓
Structured field extraction
        ↓
Report stored in MongoDB
        ↓
AI summary generated when requested
Security

The project uses JWT authentication.

Protected API requests require:

Authorization: Bearer <ACCESS_TOKEN>

Passwords are hashed using BCrypt.

Do not commit:

API keys
JWT secrets
MongoDB credentials
Firebase credentials
.env files
uploaded medical reports
personal health information

Make sure the following are ignored by Git:

.env
.env.*
!.env.example
uploads/
Important Privacy Note

Medical reports may contain sensitive personal and health information.

For development:

Use synthetic/test medical reports whenever possible.
Do not commit real patient reports to GitHub.
Do not share API keys publicly.
Do not expose MongoDB credentials.
Remove sensitive test data before distributing the repository.
Troubleshooting
Port 8080 already in use

Change the port:

PORT=8081

Then access the API at:

http://localhost:8081
MongoDB connection error

Check that MongoDB is running and verify:

MONGODB_URI
OCR not working

Check:

OCR_SPACE_API_KEY

and verify that the OCR.space service is reachable.

Gemini not working

Check:

AI_API_KEY
AI_API_URL
AI_MODEL
Windows / WSL

If running from WSL, the Windows project can be accessed using:

cd /mnt/c/Users/Akshay/Pictures/MediVault
Hackathon Workflow

MediVault is designed to support repeatable AI-assisted workflows.

Planned workflows include:

Play 1 — Scan → Understand → Record
Medical Report
      ↓
Upload
      ↓
OCR
      ↓
Classification
      ↓
Structured Extraction
      ↓
MongoDB
      ↓
Verified Report
Play 2 — Record → Analyze → Summarize
Stored Report
      ↓
Retrieve
      ↓
Gemini
      ↓
AI Analysis
      ↓
Medical Summary

These workflows can be executed through the project's backend APIs and integrated with the Modiqo/Rote workflow layer.

Development

Run tests with:

Windows
mvnw.cmd test
Linux / macOS / WSL
./mvnw test

Before pushing changes:

git status
git diff

Make sure no secrets or uploaded medical files are included.
