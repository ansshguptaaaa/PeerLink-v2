# 🚀 PeerLink - Secure File Sharing Platform

PeerLink is a secure full-stack file sharing platform that enables users to upload, share, and download files using invite codes. The platform provides authentication, transfer history tracking, persistent storage, and a modern responsive dashboard.

Built with **Java**, **Next.js**, **PostgreSQL**, and **Docker**, PeerLink focuses on secure and seamless peer-to-peer style file sharing.

---

## ✨ Features

### 🔐 Authentication
- User Signup & Login
- JWT-based Authentication
- Access & Refresh Token Management
- Protected APIs and Secure Sessions
- BCrypt Password Hashing

### 📁 File Sharing
- Upload Files Securely
- Generate Share/Invite Codes
- Download Files Using Share Codes
- Sent & Received File Tracking
- Persistent Transfer History

### 📊 Dashboard
- Total Shared Files
- Total Transfers
- Recent Activity Tracking
- Shared & Received History
- Responsive Modern UI

### ☁️ Deployment & DevOps
- Dockerized Frontend & Backend
- Cloud Deployment Support
- Environment-based Configuration

---

# 🏗 System Architecture

```text
┌─────────────────────┐
│     Next.js UI      │
│      Frontend       │
└──────────┬──────────┘
           │ REST APIs + JWT
           ▼
┌─────────────────────┐
│    Java Backend     │
│   HttpServer APIs   │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│    PostgreSQL DB    │
│                     │
│ • Users             │
│ • File Metadata     │
│ • File Transfers    │
│ • Refresh Tokens    │
└─────────────────────┘
```

---

# 🛠 Tech Stack

## Frontend
- Next.js 14
- TypeScript
- Tailwind CSS
- Axios
- Framer Motion

## Backend
- Java 17
- Maven
- Java HttpServer
- JWT
- BCrypt
- HikariCP

## Database
- PostgreSQL

## DevOps & Deployment
- Docker
- Docker Compose
- Render
- Vercel
- Git & GitHub

---

# 📂 Project Structure

```text
PeerLink
│
├── src/                        # Java Backend
│   ├── auth
│   ├── controller
│   ├── db
│   ├── model
│   ├── repository
│   └── service
│
├── ui/                         # Next.js Frontend
│   ├── src/app
│   ├── src/components
│   └── src/utils
│
├── Dockerfile.backend
├── Dockerfile.frontend
├── docker-compose.yml
├── pom.xml
└── README.md
```

---

# 🗄 Database Schema

## Users Table

| Column | Type |
|---------|------|
| id | BIGSERIAL |
| email | VARCHAR |
| password_hash | TEXT |
| created_at | TIMESTAMP |

---

## Refresh Tokens Table

| Column | Type |
|---------|------|
| id | BIGSERIAL |
| user_email | VARCHAR |
| token | TEXT |
| expiry_date | TIMESTAMP |

---

## File Metadata Table

| Column | Type |
|---------|------|
| id | BIGSERIAL |
| file_name | VARCHAR |
| owner_email | VARCHAR |
| share_code | VARCHAR |
| file_size | BIGINT |
| created_at | TIMESTAMP |

---

## File Transfers Table

| Column | Type |
|---------|------|
| id | BIGSERIAL |
| file_name | VARCHAR |
| sender_email | VARCHAR |
| receiver_email | VARCHAR |
| file_size | BIGINT |
| downloaded_at | TIMESTAMP |

---

# 🔗 API Endpoints

## Authentication APIs

```http
POST /signup
POST /login
POST /refresh
```

---

## File APIs

```http
POST /upload
GET  /files
GET  /stats
GET  /transfers
GET  /download/{shareCode}
```

---

# ⚙ Environment Variables

## Backend (.env)

```env
DB_URL=
DB_USERNAME=
DB_PASSWORD=

JWT_SECRET=
```

---

## Frontend (.env.local)

```env
NEXT_PUBLIC_API_URL=
```

---

# 🚀 Running Locally

## Clone Repository

```bash
git clone https://github.com/ansshguptaaaa/PeerLink.git
cd PeerLink
```

---

## Backend Setup

```bash
mvn clean package
java -jar target/p2p-1.0-SNAPSHOT.jar
```

Backend:

```text
http://localhost:9090
```

---

## Frontend Setup

```bash
cd ui
npm install
npm run dev
```

Frontend:

```text
http://localhost:3000
```

---

# 🐳 Docker Setup

Run the complete application:

```bash
docker compose up --build
```

---

# 📸 Main Functionalities

✅ User Authentication

✅ Secure File Upload

✅ Invite Code Based Sharing

✅ Download Using Share Codes

✅ Transfer History Tracking

✅ Dashboard Analytics

✅ PostgreSQL Persistence

✅ Dockerized Deployment

---

# 📈 Future Improvements

- One-to-Many File Sharing
- Redis Caching
- CI/CD using GitHub Actions
- Kubernetes Deployment
- Real-time Notifications
- Expiring Share Links
- File Encryption at Rest

---

# 📄 Resume Description

> Developed PeerLink, a secure full-stack file sharing platform using Java, Next.js, PostgreSQL, and Docker. Implemented JWT authentication, invite-code based file sharing, persistent transfer history, and cloud deployment using Render and Vercel.

---

# 🎯 Key Learnings

- REST API Development in Java
- JWT Authentication & Authorization
- PostgreSQL Database Design
- Docker Containerization
- Full-Stack Deployment Workflow
- Cloud-based Application Hosting

---

# 🌐 Deployment

### Frontend
https://peer-link-v2.vercel.app

### Backend
https://peerlink-backend-k7va.onrender.com

---

# 👨‍💻 Author

**Ansh Gupta**

- GitHub: https://github.com/ansshguptaaaa
  

---

# ⭐ Support

If you like this project, please consider giving it a **Star ⭐** on GitHub.
