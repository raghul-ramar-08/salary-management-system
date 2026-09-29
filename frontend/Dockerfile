FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:1.27-alpine
ENV BACKEND_URL=http://backend:8080
ENV NGINX_ENVSUBST_FILTER=BACKEND_URL
COPY nginx.conf /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist/salary-management-frontend/browser /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
