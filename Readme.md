# FinSplit — Backend API

A Spring Boot REST API for personal expense tracking and group expense splitting.

## Tech Stack
- Java 17, Spring Boot 3.2
- Spring Security + JWT
- Spring Data JPA + PostgreSQL
- Maven

## Features
- JWT authentication (register / login)
- Personal expense tracking with categories
- Monthly budget limits with alerts
- Group expense splitting (equal & custom)
- Debt simplification algorithm (minimum transactions)
- Receipt image upload
- CSV export

## Getting Started

### Prerequisites
- Java 17+
- PostgreSQL running locally
- Maven

### Setup

1. Clone the repo
2. Create a PostgreSQL database:
```sql
   CREATE DATABASE finsplit_db;
```
3. Copy the example config:
```
   cp src/main/resources/application.properties.example \
      src/main/resources/application.properties
```
4. Fill in your values in `application.properties`
5. Run:
```bash
   ./mvnw spring-boot:run
```

Server starts at `http://localhost:8080`

### Health check
```
GET http://localhost:8080/api/health
```

## API Endpoints

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | /api/auth/register | Register | No |
| POST | /api/auth/login | Login | No |
| GET | /api/users/me | Get profile | Yes |
| GET | /api/categories | List categories | No |
| POST | /api/expenses | Add expense | Yes |
| GET | /api/expenses | List expenses | Yes |
| PUT | /api/expenses/{id} | Edit expense | Yes |
| DELETE | /api/expenses/{id} | Delete expense | Yes |
| GET | /api/expenses/summary | Monthly summary | Yes |
| POST | /api/expenses/{id}/receipt | Upload receipt | Yes |
| GET | /api/budgets | Get budgets | Yes |
| POST | /api/budgets | Set budget | Yes |
| GET | /api/budgets/analytics | Analytics | Yes |
| GET | /api/groups | My groups | Yes |
| POST | /api/groups | Create group | Yes |
| POST | /api/groups/{id}/members | Add member | Yes |
| POST | /api/groups/{id}/expenses | Add group expense | Yes |
| GET | /api/groups/{id}/balances | View balances | Yes |
| GET | /api/export/expenses | Download CSV | Yes |