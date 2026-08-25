# The built frontend, served by nginx. Works on any platform that runs a
# container - and gives you an option besides Vercel or Netlify.

# ---------- build ----------
FROM node:22-alpine AS build
WORKDIR /build

# package files first so the dependency install caches in its own layer and
# does not repeat on every source change.
COPY package*.json ./
# ci, not install: it installs exactly what package-lock.json pins, so the
# image is reproducible and cannot quietly pick up a newer minor version.
RUN npm ci

COPY . .

# The API URL is substituted INTO THE BUNDLE at build time, so it has to be
# known now - there is nothing in dist/ to edit afterwards. That is a property
# of Vite, not a choice: import.meta.env is replaced by the compiler.
#
#   docker build --build-arg VITE_API_URL=https://api.example.com/api .
#
# Left empty it falls back to localhost, which is right for a dev build and
# useless in a deployed one.
ARG VITE_API_URL
ENV VITE_API_URL=$VITE_API_URL
RUN npm run build

# ---------- serve ----------
FROM nginx:1.27-alpine AS runtime

COPY --from=build /build/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
