# K8s 部署手册 — chenxu_ai2026

> **前提**: Docker Desktop 已安装并运行，VM 中间件 (192.168.249.129) 在线

---

## 一、环境准备

### 1.1 安装 kind + kubectl

```bash
# kind (Kubernetes in Docker)
curl -Lo kind-windows-amd64.exe https://kind.sigs.k8s.io/dl/v0.27.0/kind-windows-amd64
mv kind-windows-amd64.exe /usr/local/bin/kind

# kubectl
curl -LO "https://dl.k8s.io/release/v1.32.0/bin/windows/amd64/kubectl.exe"
```

### 1.2 验证

```bash
docker version      # Docker Desktop 运行中
kind version        # kind v0.27.0+
kubectl version     # v1.32.0+
```

---

## 二、创建 kind 集群

```bash
cd chenxu_ai2026/chenxu_ai2026

# 创建集群（映射 NodePort 到宿主机）
kind create cluster --config k8s/kind-config.yaml

# 验证
kubectl get nodes
# NAME                           STATUS   ROLES           AGE   VERSION
# chenxu-cluster-control-plane   Ready    control-plane   1m    v1.32.0

# 查看集群信息
kubectl cluster-info
```

---

## 三、构建镜像

### 3.1 构建 Java 服务镜像

```bash
# chenxu-demo
docker build -f Dockerfile.demo -t chenxu-demo:latest .

# chenxu-gateway
docker build -f Dockerfile.gateway -t chenxu-gateway:latest .
```

### 3.2 构建 Vue 前端镜像

```bash
docker build -f Dockerfile.vue -t chenxu-vue:latest .
```

### 3.3 加载镜像到 kind 节点

> kind 不会自动使用宿主机 Docker 镜像，需要手动 `load`。

```bash
kind load docker-image chenxu-demo:latest    --name chenxu-cluster
kind load docker-image chenxu-gateway:latest --name chenxu-cluster
kind load docker-image chenxu-vue:latest     --name chenxu-cluster
```

---

## 四、部署到 K8s

```bash
# 1. 创建命名空间
kubectl apply -f k8s/namespace.yaml

# 2. 注入配置和密钥
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secret.yaml

# 3. 部署服务
kubectl apply -f k8s/demo-deployment.yaml
kubectl apply -f k8s/gateway-deployment.yaml
kubectl apply -f k8s/vue-deployment.yaml
```

---

## 五、查看部署状态

```bash
# 所有资源
kubectl -n chenxu get all

# Pod 状态（等待 Running）
kubectl -n chenxu get pods -w

# 日志排查
kubectl -n chenxu logs -l app=chenxu-demo --tail=50
kubectl -n chenxu logs -l app=chenxu-gateway --tail=50

# 查看某个 Pod 的启动事件
kubectl -n chenxu describe pod -l app=chenxu-demo
```

---

## 六、验证

### 6.1 端口转发（调试用）

```bash
# 直接访问 demo（不经过 gateway）
kubectl -n chenxu port-forward svc/chenxu-demo 8081:8081
# 另开终端:
curl http://localhost:8081/actuator/health
curl http://localhost:8081/actuator/prometheus | head
```

### 6.2 NodePort 访问

| 服务 | NodePort | 访问地址 | 验证命令 |
|------|----------|----------|----------|
| **Gateway** | 30080 | `http://localhost:30080` | `curl localhost:30080/actuator/health` |
| **Vue 前端** | 31080 | `http://localhost:31080` | 浏览器打开 |

```bash
# Gateway 健康检查
curl http://localhost:30080/actuator/health

# Gateway 代理到 demo 的健康检查
curl http://localhost:30080/actuator/health

# Prometheus 指标
curl http://localhost:30080/actuator/prometheus | head

# 登录测试
curl -X POST http://localhost:30080/auth/login \
  -d 'username=admin&password=123456'
```

### 6.3 Nacos 注册验证

浏览器打开 `http://192.168.249.129:8848/nacos` → 服务管理 → 服务列表：
- `chenxu-demo` — 应有 2 个实例（Pod IP:8081）
- `chenxu-gateway` — 应有 1 个实例（Pod IP:8080）

### 6.4 前端验证

浏览器打开 `http://localhost:31080`：
1. 显示登录页面
2. 注册/登录成功
3. 各功能模块正常（电表/Kafka/Redis/Sentinel/交易链路）

---

## 七、常用运维命令

```bash
# 查看日志
kubectl -n chenxu logs deploy/chenxu-demo --tail=100 -f
kubectl -n chenxu logs deploy/chenxu-gateway --tail=100 -f

# 进入容器
kubectl -n chenxu exec -it deploy/chenxu-demo -- sh
kubectl -n chenxu exec -it deploy/chenxu-gateway -- sh

# 重启
kubectl -n chenxu rollout restart deploy/chenxu-demo
kubectl -n chenxu rollout restart deploy/chenxu-gateway

# 扩缩容
kubectl -n chenxu scale deploy/chenxu-demo --replicas=3
kubectl -n chenxu scale deploy/chenxu-demo --replicas=1

# 更新 ConfigMap 后重启（ConfigMap 修改不会自动重启 Pod）
kubectl -n chenxu rollout restart deploy/chenxu-demo
kubectl -n chenxu rollout restart deploy/chenxu-gateway

# 删除整个 namespace
kubectl delete namespace chenxu
```

---

## 八、更新镜像

修改代码后重新部署：

```bash
# 1. 重新构建
docker build -f Dockerfile.demo -t chenxu-demo:latest .

# 2. 加载到 kind
kind load docker-image chenxu-demo:latest --name chenxu-cluster

# 3. 滚动更新
kubectl -n chenxu rollout restart deploy/chenxu-demo

# 4. 观察更新过程
kubectl -n chenxu rollout status deploy/chenxu-demo
```

---

## 九、清理

```bash
# 删除集群
kind delete cluster --name chenxu-cluster

# 删除镜像
docker rmi chenxu-demo:latest chenxu-gateway:latest chenxu-vue:latest
```

---

## 十、故障排查

### Pod 一直 Pending

```bash
kubectl -n chenxu describe pod <pod-name>
# 常见原因: 资源不足 / 镜像拉取失败
```

### Pod CrashLoopBackOff

```bash
kubectl -n chenxu logs <pod-name> --previous
# 常见原因: Nacos 连不上 / MySQL 连不上 / 配置错误
```

### kind 容器无法访问 192.168.249.129

```bash
# 进入 kind 节点测试网络
docker exec -it chenxu-cluster-control-plane sh
ping 192.168.249.129
# 如果不通，检查 Docker Desktop 网络模式
```

### NodePort 无法访问

```bash
# 确认 kind 端口映射
docker port chenxu-cluster-control-plane
# 应看到 30080/TCP → 0.0.0.0:30080, 31080/TCP → 0.0.0.0:31080
```
