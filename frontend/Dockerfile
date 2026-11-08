# Stage 1: Build
FROM node:20-alpine AS build
WORKDIR /app

# Copy package files trước để cache dependencies
COPY package.json package-lock.json ./
RUN npm ci

# Copy source code và build
COPY . .
# stripe (mặc định) | mock (demo/test khi chưa có khóa Stripe)
ARG VITE_PAYMENT_MODE=stripe
ENV VITE_PAYMENT_MODE=$VITE_PAYMENT_MODE
RUN npm run build

# Stage 2: Serve với Nginx
FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]