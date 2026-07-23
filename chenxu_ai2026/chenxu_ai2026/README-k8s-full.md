# K8s + Docker Desktop 完整操作手册

> **目标**: 从零开始，用 Docker Desktop + kind 管理 chenxu_ai2026 项目
> **当前状态**: Docker Desktop 4.81.0 ✅ | kind v0.27.0 ✅ | kubectl v1.32.0 ✅ | 集群已创建 ✅

---

## 零、环境要求

- Windows 10/11 Pro
- WSL2（已装 ✅）
- 至少 8GB 空闲内存

---

## 一、安装 Docker Desktop

### 1.1 安装

```powershell
# 方式一：winget（推荐）
winget install Docker.DockerDesktop --accept-source-agreements --accept-package-agreements

# 方式二：官网下载
# https://www.docker.com/products/docker-desktop/
```

安装后重启电脑，Docker Desktop 自动启动。右下角看到 Docker 鲸鱼图标变白即就绪。

### 1.2 配置镜像加速（重要！）

编辑 `C:\Users\<用户名>\.docker\daemon.json`：

```json
{
  "builder": {
    "gc": {
      "defaultKeepStorage": "20GB",
      "enabled": true
    }
  },
  "experimental": false,
  "registry-mirrors": [
    "https://docker.m.daocloud.io",
    "https://dockerhub.timeweb.cloud",
    "https://registry.cn-hangzhou.aliyuncs.com"
  ]
}
```

修改后重启 Docker Desktop：

```bash
# 杀进程
taskkill /F /IM "Docker Desktop.exe"

# 重新启动
start "" "C:\Program Files\Docker\Docker\Docker Desktop.exe"

# 等待就绪后验证镜像是否生效
docker info | grep -A5 "Registry Mirrors"
```

### 1.3 汉化（可选）

1. 打开 https://github.com/asxez/DockerDesktop-CN/releases
2. 下载对应版本的 `app-Windows-x86.zip`（当前需要 v4.81.0，113MB）
3. 关闭 Docker Desktop（托盘也要退出）
4. 备份原文件：`C:\Program Files\Docker\Docker\frontend\resources\app.asar` → `app.asar.bak`
5. 解压下载的 zip，将里面的 `app.asar` 放到上述目录
6. 重启 Docker Desktop

### 1.4 验证

```bash
docker version     # 应显示 Client 和 Server 版本
docker ps          # 应正常返回（可能为空）
docker run hello-world   # 测试拉取和运行
```

---

## 二、安装 kind + kubectl

### 2.1 下载

```bash
# 创建工具目录
mkdir -p ~/bin

# kind（Kubernetes in Docker）
curl -Lo ~/bin/kind.exe "https://kind.sigs.k8s.io/dl/v0.27.0/kind-windows-amd64"

# kubectl
curl -Lo ~/bin/kubectl.exe "https://dl.k8s.io/release/v1.32.0/bin/windows/amd64/kubectl.exe"
```

### 2.2 配置 PATH

编辑 `~/.bashrc`，追加：

```bash
export PATH="$HOME/bin:/c/Program Files/Docker/Docker/resources/bin:$PATH"
alias kubectl="kubectl.exe"
alias kind="kind.exe"
```

新开终端生效，或 `source ~/.bashrc`。

### 2.3 验证

```bash
kind version        # v0.27.0
kubectl version --client   # v1.32.0
```

---

## 三、创建 kind 集群

### 3.1 配置文件

项目 `k8s/kind-config.yaml`：

```yaml
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
name: chenxu-cluster
nodes:
  - role: control-plane
    extraPortMappings:
      - containerPort: 30080   # gateway NodePort
        hostPort: 30080
        protocol: TCP
      - containerPort: 31080   # vue NodePort
        hostPort: 31080
        protocol: TCP
```

### 3.2 创建集群

```bash
# git bash里边执行
cd chenxu_ai2026/chenxu_ai2026
kind create cluster --config k8s/kind-config.yaml

# 验证
kubectl get nodes
# 输出: chenxu-cluster-control-plane   Ready   control-plane   xxm   v1.32.2
```

### 3.3 常用 kind 命令

```bash
kind get clusters                          # 列出所有集群
kind delete cluster --name chenxu-cluster  # 删除集群
docker ps                                  # kind 本质是 Docker 容器
```

---

## 四、构建 Docker 镜像

### 4.1 项目 Dockerfile 说明

| 文件 | 构建内容 | 端口 | 基础镜像 |
|------|----------|------|----------|
| `Dockerfile.demo` | chenxu-demo 业务服务 | 8081 | eclipse-temurin:21-jre-alpine |
| `Dockerfile.gateway` | chenxu-gateway 网关 | 8080 | eclipse-temurin:21-jre-alpine |
| `Dockerfile.vue` | Vue3 前端 (nginx) | 80 | nginx:alpine |

> 都是多阶段构建：Stage 1 用 Maven/Node 编译，Stage 2 用瘦身镜像运行。

### 4.2 构建步骤

```bash
cd chenxu_ai2026/chenxu_ai2026

# 构建 chenxu-demo（首次较慢，需下载 Maven 依赖 ~5-10min）
docker build -f Dockerfile.demo -t chenxu-demo:latest .

# 构建 chenxu-gateway
docker build -f Dockerfile.gateway -t chenxu-gateway:latest .

# 构建 Vue 前端
docker build -f Dockerfile.vue -t chenxu-vue:latest .
```

### 4.3 加载镜像到 kind

```bash
# 必须执行！kind 不会自动使用 Docker 宿主机镜像
kind load docker-image chenxu-demo:latest    --name chenxu-cluster
kind load docker-image chenxu-gateway:latest --name chenxu-cluster
kind load docker-image chenxu-vue:latest     --name chenxu-cluster
```

> 每次重新构建镜像后，需要再次 `kind load`。

---

## 五、部署到 K8s

### 5.1 文件结构

```
k8s/
├── kind-config.yaml         # kind 集群配置
├── namespace.yaml           # 命名空间 chenxu
├── configmap.yaml           # 公共配置（中间件地址）
├── secret.yaml              # 密码/密钥
├── demo-deployment.yaml     # chenxu-demo: Deployment + ClusterIP Service
├── gateway-deployment.yaml  # chenxu-gateway: Deployment + NodePort Service
├── vue-deployment.yaml      # chenxu-vue: Deployment + NodePort Service
└── nginx-default.conf       # Vue 的 nginx 反向代理配置
```

### 5.2 部署命令（一键）

```bash
# 1. 基础资源
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secret.yaml

# 2. 应用服务
kubectl apply -f k8s/demo-deployment.yaml
kubectl apply -f k8s/gateway-deployment.yaml
kubectl apply -f k8s/vue-deployment.yaml

# 3. 查看状态
kubectl -n chenxu get all
kubectl -n chenxu get pods -w    # 等待所有 Pod Running
```

### 5.3 重要说明

- **中间件（MySQL/Nacos/Kafka/Redis）仍在 VM `192.168.249.129`**，不在 K8s 中
- **Nacos 注册发现仍正常**：K8s Pod 向 VM 的 Nacos 注册 Pod IP，gateway 通过 `lb://chenxu-demo` 路由
- **NodePort 映射**：
  - `localhost:30080` → Gateway
  - `localhost:31080` → Vue 前端

---

## 六、验证部署

### 6.1 基础检查

```bash
# Pods 状态
kubectl -n chenxu get pods
# 预期: 4 个 Running（demo×2 + gateway×1 + vue×1）

# 日志
kubectl -n chenxu logs deploy/chenxu-demo --tail=50
```

### 6.2 端口转发调试

```bash
# 转发 demo 到本地
kubectl -n chenxu port-forward svc/chenxu-demo 8081:8081

# 另开终端测试
curl http://localhost:8081/actuator/health
curl http://localhost:8081/actuator/prometheus | head
```

### 6.3 NodePort 直连

```bash
# Gateway 健康检查
curl http://localhost:30080/actuator/health

# 登录测试
curl -X POST http://localhost:30080/auth/login \
  -d 'username=admin&password=123456'

# 浏览器打开前端
# http://localhost:31080
```

### 6.4 Nacos 注册验证

浏览器打开 `http://192.168.249.129:8848/nacos` (nacos/nacos) → 服务管理 → 服务列表：
- `chenxu-demo` 应有 2 个实例
- `chenxu-gateway` 应有 1 个实例

---

## 七、日常运维

### 更新代码后重新部署

```bash
# 1. 重新构建
docker build -f Dockerfile.demo -t chenxu-demo:latest .

# 2. 加载到 kind
kind load docker-image chenxu-demo:latest --name chenxu-cluster

# 3. 滚动重启
kubectl -n chenxu rollout restart deploy/chenxu-demo

# 4. 观察
kubectl -n chenxu rollout status deploy/chenxu-demo
```

### 常用命令速查

```bash
# --- 查看 ---
kubectl -n chenxu get pods -o wide           # Pod 列表 + IP
kubectl -n chenxu get svc                     # Service 列表
kubectl -n chenxu describe pod <pod-name>    # Pod 详情
kubectl -n chenxu logs -f <pod-name>         # 实时日志

# --- 操作 ---
kubectl -n chenxu exec -it <pod-name> -- sh  # 进入容器
kubectl -n chenxu scale deploy/chenxu-demo --replicas=3  # 扩缩容
kubectl -n chenxu rollout restart deploy/chenxu-demo     # 重启
kubectl -n chenxu delete pod <pod-name>      # 删 Pod（自动重建）

# --- 配置热更新 ---
kubectl -n chenxu edit configmap app-config  # 编辑配置
kubectl -n chenxu rollout restart deploy/chenxu-demo  # 重启使配置生效
```

---

## 八、故障排查

| 现象 | 排查命令 | 常见原因 |
|------|----------|----------|
| Pod Pending | `kubectl -n chenxu describe pod <name>` | 资源不足 / 镜像拉取失败 |
| CrashLoopBackOff | `kubectl -n chenxu logs <pod-name> --previous` | Nacos/MySQL 连不上 |
| 镜像拉取失败 | `kubectl -n chenxu describe pod <name> \| grep -A5 Events` | 忘了 `kind load` |
| NodePort 不通 | `docker port chenxu-cluster-control-plane` | 端口映射没生效 |
| Pod 连不上 VM | `kubectl -n chenxu exec -it <pod> -- ping 192.168.249.129` | 网络不通 |

### 彻底重置

```bash
# 删集群
kind delete cluster --name chenxu-cluster

# 删镜像
docker rmi chenxu-demo:latest chenxu-gateway:latest chenxu-vue:latest

# 重建
kind create cluster --config k8s/kind-config.yaml
# ... 重新构建 + 加载 + 部署
```

---

## 九、架构速览

```
Docker Desktop (WSL2)
  └── kind 集群 (chenxu-cluster)
       ├── chenxu-gateway  (NodePort:30080) → 对外 HTTP 入口
       ├── chenxu-demo ×2  (ClusterIP)      → 业务逻辑
       └── chenxu-vue      (NodePort:31080) → 前端页面
            │
            │  所有服务通过 192.168.249.129 访问中间件
            ▼
  VM 192.168.249.129
       ├── MySQL:3306   (austin 库)
       ├── Nacos:8848    (注册 + 配置)
       ├── Kafka:9092    (消息)
       └── Redis:6379    (缓存)
```
