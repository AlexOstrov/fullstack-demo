# Dockerfile для frontend (Sprint 7)
# Сборка production-бандла: nginx служит статикой и проксирует /api → game-service

# --- Этап 1: сборка React-приложения ---
FROM node:22-alpine AS builder

WORKDIR /app

# package-lock.json включён в контекст → воспроизводимая установка через npm ci
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# --- Этап 2: nginx со статикой ---
FROM nginx:stable-alpine

COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
