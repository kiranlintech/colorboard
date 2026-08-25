# 🌈 ColorBoard

Simple colourful 3-tier Java application for DevOps practice.

## Architecture

Browser → NGINX → Spring Boot → MySQL

Only NGINX exposes port 80. MySQL and Java stay on the Docker network.

## Run with Docker Compose

```bash
docker compose up -d --build
```

Open:

http://localhost

## Useful commands

```bash
docker compose ps
docker compose logs -f backend
docker compose logs -f nginx
docker compose logs -f mysql
docker compose down
```

## API

```text
GET    /api/tasks
POST   /api/tasks
PUT    /api/tasks/{id}
DELETE /api/tasks/{id}
GET    /api/tasks/health
```

## Next DevOps exercises

1. Push to GitHub
2. Create Jenkins pipeline
3. Add Maven tests
4. Add SonarQube
5. Add Trivy image scanning
6. Push image to Docker Hub
7. Deploy to VPS
8. Add HTTPS with NGINX
9. Convert Jenkinsfile to GitHub Actions
10. Deploy the same application to Kubernetes
