# ============================================
# chenxu-vue 多阶段构建
# 阶段1: Node 22 编译 Vue3 + Vite
# 阶段2: nginx alpine 托管静态文件
# ============================================

# ---- Stage 1: 编译 ----
FROM node:22-alpine AS build
WORKDIR /app

# 分层缓存: 先装依赖
COPY chenxu-vue/package*.json ./
RUN npm ci

# 复制源码并构建
COPY chenxu-vue/ ./
RUN npm run build

# ---- Stage 2: nginx 托管 ----
FROM nginx:alpine

# 替换默认 nginx 配置（含 API 代理到 gateway）
COPY k8s/nginx-default.conf /etc/nginx/conf.d/default.conf

# 复制构建产物
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s \
  CMD wget -qO- http://localhost/ || exit 1
