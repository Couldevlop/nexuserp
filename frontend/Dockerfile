# NexusERP — Frontend Angular 18
# Build optimisé : cache npm + dist minimal

# ─── Étape 1 : Build Angular ─────────────────────────────────────────────────
FROM node:20-alpine AS builder
WORKDIR /app

# Copier manifestes seuls pour maximiser le cache layer
COPY package.json package-lock.json ./

# Installer avec cache BuildKit. npm ci (strict) si le lockfile est synchro,
# sinon fallback npm install (le temps de régénérer package-lock.json).
RUN --mount=type=cache,target=/root/.npm \
    sh -c "npm ci --legacy-peer-deps --prefer-offline || npm install --legacy-peer-deps"

# Copier sources et builder
COPY . .
RUN npm run build -- --configuration production --output-hashing=all

# ─── Étape 2 : Serveur Nginx léger ───────────────────────────────────────────
FROM nginx:1.27-alpine AS runtime

# Supprimer la config nginx par défaut
RUN rm -rf /usr/share/nginx/html/*

# Copier le build Angular
COPY --from=builder /app/dist/nexuserp/browser /usr/share/nginx/html

# Config Nginx : SPA routing + compression + cache assets
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Utilisateur non-root
RUN chown -R nginx:nginx /usr/share/nginx/html && chmod -R 755 /usr/share/nginx/html

EXPOSE 80
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD wget -qO- http://localhost/health || exit 1

CMD ["nginx", "-g", "daemon off;"]
