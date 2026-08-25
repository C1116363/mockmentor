# Build the static bundle, then serve it with nginx.
#
# Only needed if your host runs containers rather than static files. For
# Vercel, Netlify, Cloudflare Pages or GitHub Pages, skip this entirely -
# `npm run build` and upload dist/.

# ---------- build ----------
FROM node:22-alpine AS build
WORKDIR /build

# Lockfile first so the install layer caches independently of source changes.
COPY package.json package-lock.json* ./
RUN npm ci

COPY . .

# The API URL is baked into the bundle at BUILD time - it is not read from the
# environment when the container starts. So it has to be a build argument, and
# changing it means rebuilding the image, not restarting it. This is the single
# most surprising thing about deploying a Vite app.
ARG VITE_API_URL=http://localhost:8080/api
ENV VITE_API_URL=$VITE_API_URL
RUN npm run build

# ---------- serve ----------
FROM nginx:1.27-alpine AS runtime

COPY --from=build /build/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 8080 rather than 80 so the container needs no privileged port, which lets it
# run as a non-root user on hosts that require it.
EXPOSE 8080
