# Policy Pulse API

Policy Pulse API is a Spring Boot REST backend for managing insurance policy records and policy documents.

It supports policy CRUD operations, pagination, status-based search, AWS S3 document upload/download, and Kafka event publishing after a document is uploaded.

## Features

- Create, read, update, and delete policy records
- Paginated policy listing
- Search policies by status
- Upload policy documents to AWS S3
- Download policy documents from AWS S3
- Store only the S3 document key in the database
- Publish a Kafka event when a policy document is uploaded
- Validate request data using Jakarta Bean Validation

## Tech Stack

- Java
- Spring Boot
- Spring Web
- Spring Data JPA
- PostgreSQL
- AWS S3 SDK
- Apache Kafka
- Maven
- Flyway

## Project Structure

src/main/java/com/policypulse/api

- Policy.java
- PolicyController.java
- PolicyRepository.java
- PolicyService.java
- S3Config.java
- S3Service.java
- PolicyDocumentUploadedEvent.java
- PolicyKafkaProducer.java

## Main Concepts

### Policy Entity

Policy.java represents the policy table in the database.

One Policy object represents one row in the policy table.

Important fields:

- id
- policyNumber
- holderName
- status
- premium
- documentKey
- createdAt
- updatedAt

### Document Storage

The actual document file is stored in AWS S3.

The database does not store the full document file. It stores only the S3 object key in the document_key column.

Example:

S3 bucket stores:

1714450000000_policy.pdf

Database stores:

document_key = 1714450000000_policy.pdf

### Kafka Event

After a policy document is uploaded, the application can publish a Kafka event.

Example event:

{
  "eventType": "POLICY_DOCUMENT_UPLOADED",
  "policyId": 10,
  "policyNumber": "POL101",
  "documentKey": "1714450000000_policy.pdf",
  "uploadedAt": "2026-05-10T10:30:00Z"
}

This event can later be consumed by another service, such as an audit service, notification service, or reporting service.

## API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| GET | /api/policies?page=0&size=10 | Get paginated policies |
| POST | /api/policies | Create a new policy |
| GET | /api/policies/{id} | Get policy by ID |
| PUT | /api/policies/{id} | Update policy by ID |
| DELETE | /api/policies/{id} | Delete policy by ID |
| GET | /api/policies/search?status=ACTIVE&page=0&size=10 | Search policies by status |
| POST | /api/policies/{id}/document | Upload document for a policy |
| GET | /api/policies/{id}/document | Download document for a policy |

## Sample Create Policy Request

POST /api/policies

Request body:

{
  "policyNumber": "POL101",
  "holderName": "John Doe",
  "status": "ACTIVE",
  "premium": 500.00
}

## Sample Upload Document Request

POST /api/policies/1/document

Content-Type:

multipart/form-data

Form data:

file = policy-document.pdf

## Sample Download Document Request

GET /api/policies/1/document

The API returns the file as binary data with a download header.

## Configuration

Add the required configuration in application.yml.

server:
  port: 8080

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/policy_pulse
    username: your_db_username
    password: your_db_password

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: true

  flyway:
    enabled: true

aws:
  s3:
    bucket-name: your-s3-bucket-name

kafka:
  bootstrap-servers: localhost:9092
  topic:
    document-uploaded: policy-document-uploaded

## AWS S3 Setup

Create an S3 bucket in the AWS Console and update the bucket name in application.yml.

The application uses S3Client to upload and download files from the configured bucket.

## Kafka Setup

Run Kafka locally or provide a reachable Kafka bootstrap server.

Example:

kafka:
  bootstrap-servers: localhost:9092

The producer sends document upload events to the configured topic.

## Run the Application

mvn spring-boot:run

The backend runs on:

http://localhost:8080

## Frontend CORS

The controller allows requests from:

http://localhost:5173

This is useful when running a Vite/React frontend locally.

## Example Flow

### Create Policy

Frontend -> PolicyController -> PolicyService -> PolicyRepository -> PostgreSQL

### Upload Document

Frontend uploads file -> PolicyController -> PolicyService -> S3Service -> AWS S3 stores the file -> PostgreSQL stores the document key -> Kafka event is published

### Download Document

Frontend clicks download -> PolicyController -> PolicyService gets document key from DB -> S3Service downloads file bytes from S3 -> Controller returns byte[] as downloadable file

## Notes

- The actual document is stored in S3.
- The database stores only the S3 object key.
- Kafka is used to publish an event after a successful document upload.
- Validation is handled using annotations like NotBlank, NotNull, Size, and DecimalMin.

## Future Improvements

- Add global exception handling
- Add unit and integration tests
- Add CloudWatch logging
- Add authentication and authorization
- Add Docker Compose for PostgreSQL and Kafka
- Add API documentation with Swagger/OpenAPI
