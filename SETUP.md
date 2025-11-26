# SahamatiNet Agent - Deployment Guide

Guide to deploy SahamatiNet Agent using Helm chart in Kubernetes.

## Prerequisites

- Kubernetes cluster (v1.20 or higher)
- Helm 3.x installed
- `kubectl` configured to access your cluster

## Docker Image

**Image**: `sahamatidevsecops/sahamatinet-agent:1.0.0`

## Quick Start

### 1. Clone Repository

```bash
git clone https://github.com/Sahamati/sahamatinet-agent-downloadables.git
cd sahamatinet-agent-downloadables
```

### 2. Create Namespace

```bash
kubectl create namespace sahamatinet-agent
```

### 3. Deploy with Helm

```bash
cd helmchart
helm install sahamatinet-agent . --namespace sahamatinet-agent
```

**Or** from the repository root:

```bash
helm install sahamatinet-agent ./helmchart --namespace sahamatinet-agent
```

**Note**: Wait for approximately 1 minute after installation for the pod to become ready. The readiness probe performs health checks that may take some time to pass.

### 4. Verify Deployment

```bash
kubectl get pods -n sahamatinet-agent
kubectl get statefulset -n sahamatinet-agent
kubectl get svc -n sahamatinet-agent
```

## Configuration Variables

The Helm chart uses the following configuration variables (defined in `values.yaml`). All variables have defaults and are optional:

### Image Configuration

```yaml
image:
  repository: sahamatidevsecops/sahamatinet-agent
  pullPolicy: IfNotPresent
  tag: "1.0.0"
```

### Configuration Variables

These are set via the `data` section in `values.yaml` and passed to the container as a `config.yaml` file:

| Variable | Default | Description |
|----------|---------|-------------|
| `port` | `"4044"` | Port on which the service runs |
| `routes_prefix` | `"sna"` | API route prefix (e.g., `/sna/v1/`) |
| `api_response_version` | `"1.0.0"` | API response version |
| `max_payload_size_in_kb` | `4096` | Maximum payload size in KB |
| `gomaxprocs` | `0` | GOMAXPROCS value (0 = use all available CPUs) |
| `error_code` | `""` | Error code configuration (empty by default) |
| `read_buffer_size` | `4096` | Read buffer size in bytes |
| `env` | `"production"` | Environment (development/production) |

**Note**: These values are stored in a `config.yaml` file in the ConfigMap and mounted to the container.

### Service Configuration

```yaml
service:
  type: ClusterIP      # Service type (ClusterIP, NodePort, or LoadBalancer)
  port: 4044          # Service port
  targetPort: 4044    # Container port
```

### Resource Limits

```yaml
resources:
  limits:
    cpu: 100m
    memory: 256Mi
  requests:
    cpu: 50m
    memory: 256Mi
```

### Replica Count

```yaml
replicaCount: 1  # Number of pod replicas
```

### Horizontal Pod Autoscaler (HPA)

```yaml
hpa:
  enabled: true        # Enable/disable HPA
  minReplicas: 1       # Minimum number of replicas
  maxReplicas: 3       # Maximum number of replicas
  cpuTarget: 80        # CPU utilization target (%)
  scaleDownWindow: 300  # Scale down stabilization window (seconds)
```

### Ingress Configuration (Optional)

Ingress is **disabled by default**. Only enable if you have an ingress controller and need external access.

**For Kong Ingress:**
```yaml
global:
  enable_nginx_kong: true
  kong_ingress_class: kong

ingress:
  api_host: "your-domain.com"
  api_secret_name: "your-tls-secret"
```

**For Azure Application Gateway:**
```yaml
global:
  enable_app_gateway: true
  app_gateway_ingress_class: azure/application-gateway

ingress:
  api_host: "your-domain.com"
  api_secret_name: "your-tls-secret"
```

## Customizing Configuration

### Option 1: Edit values.yaml

Edit `helmchart/values.yaml` and then install:

```bash
helm install sahamatinet-agent . --namespace sahamatinet-agent
```

### Option 2: Use custom values file

Create a `custom-values.yaml`:

```yaml
replicaCount: 2
data:
  env: "production"
  max_payload_size_in_kb: 8192
hpa:
  maxReplicas: 5
```

Then install:

```bash
helm install sahamatinet-agent . --namespace sahamatinet-agent -f custom-values.yaml
```

### Option 3: Override values on command line

```bash
helm install sahamatinet-agent . --namespace sahamatinet-agent \
  --set replicaCount=2 \
  --set data.env=production \
  --set data.max_payload_size_in_kb=8192
```

## Upgrading Deployment

```bash
helm upgrade sahamatinet-agent . --namespace sahamatinet-agent
```

Or with custom values:

```bash
helm upgrade sahamatinet-agent . --namespace sahamatinet-agent -f custom-values.yaml
```

## Testing the APIs

Once deployed, you can test the three APIs:

### Method 1: Port Forward (Recommended)

```bash
# In one terminal - start port forwarding
kubectl port-forward -n sahamatinet-agent svc/sahamatinet-agent 4044:4044

# In another terminal - test the APIs
curl http://localhost:4044/sna/v1/ping
curl http://localhost:4044/sna/v1/version
curl -X POST http://localhost:4044/sna/v1/aa \
  -H "Content-Type: application/json" \
  -d '{"test": "data"}'

# If calling from another pod
#given: sahamati-net-agent-pod-ip is the ip of the sahamatinet-agent pod

curl -X POST http://sahamati-net-agent-pod-ip:4044/sna/v1/aa \
  -H "Content-Type: application/json" \
  -d '{"test": "data"}'

#Note: payload and response handling will be updated when the working agent image is given.
```

### Method 2: kubectl exec

```bash
kubectl exec -n sahamatinet-agent -it statefulset/sahamatinet-agent -- \
  curl http://localhost:4044/sna/v1/ping
```

### Method 3: Using Ingress (if configured)

```bash
curl https://your-domain.com/sna/v1/ping
curl https://your-domain.com/sna/v1/version
curl -X POST https://your-domain.com/sna/v1/aa \
  -H "Content-Type: application/json" \
  -d '{"test": "data"}'
```

**Note**: When using Kong/App Gateway ingress, the path prefix `/sna/v1/` is used for external access and routes to `/sna/v1/` internally.

## API Endpoints

The SahamatiNet Agent service provides three APIs:

### 1. Health Check API

**Endpoint**: `GET /sna/v1/ping`

**Response**:
```json
{
  "status": "up"
}
```

**Example**:
```bash
curl http://localhost:4044/sna/v1/ping
```

### 2. Version API

**Endpoint**: `GET /sna/v1/version`

**Response**:
```json
{
  "agentVersion": "1.0.0",
  "name": "SahamatiNet Agent (SNA)"
}
```

**Example**:
```bash
curl http://localhost:4044/sna/v1/version
```

### 3. Agent Request API

**Endpoint**: `POST /sna/v1/aa`

**Request Body**: JSON (any data)

**Response**:
```json
{
  "message": "accepted"
}
```

**Example**:
```bash
curl -X POST http://localhost:4044/sna/v1/aa \
  -H "Content-Type: application/json" \
  -d '{"test": "data"}'
```

## API Summary Table

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/sna/v1/ping` | Health check endpoint |
| GET | `/sna/v1/version` | Get agent version information |
| POST | `/sna/v1/aa` | Handle agent requests |

## Troubleshooting

### Pods not starting

```bash
# Check pod status
kubectl get pods -n sahamatinet-agent

# Describe pod for details
kubectl describe pod -n sahamatinet-agent -l app.kubernetes.io/name=sahamatinet-agent

# Check logs
kubectl logs -n sahamatinet-agent -l app.kubernetes.io/name=sahamatinet-agent
```

### Service not accessible

```bash
# Check service
kubectl get svc -n sahamatinet-agent

# Check endpoints
kubectl get endpoints -n sahamatinet-agent

# Describe service
kubectl describe svc -n sahamatinet-agent sahamatinet-agent
```

### Image pull issues

```bash
# Verify image name
kubectl describe pod -n sahamatinet-agent | grep -i image

# Test image pull manually
docker pull sahamatidevsecops/sahamatinet-agent:1.0.0
```

## Uninstallation

```bash
helm uninstall sahamatinet-agent --namespace sahamatinet-agent
```

To also remove the namespace:

```bash
kubectl delete namespace sahamatinet-agent
```

## Additional Information

- **Repository**: `https://github.com/Sahamati/sahamatinet-agent-downloadables.git`
- **Helm Chart Location**: `helmchart/`
- **Docker Image**: `sahamatidevsecops/sahamatinet-agent:1.0.0`
- **Service Port**: `4044`
- **Default Namespace**: `sahamatinet-agent`

## Support

For issues or questions, please contact the Sahamati team.
