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

### 3. Create TLS Secret (Optional - Only if HTTPS is enabled)

If you want to enable HTTPS for the service, create a Kubernetes Secret with your TLS certificate and key:

```bash
kubectl create secret tls sahamatinet-agent-tls \
  --cert=path/to/cert.pem \
  --key=path/to/key.pem \
  -n sahamatinet-agent
```

**Note**: 
- `path/to/cert.pem` and `path/to/key.pem` are **local file paths** on your machine where the certificate and key files are located (e.g., `/home/user/certs/cert.pem` or `./certs/cert.pem`)
- Replace these paths with the actual paths to your certificate and key files on your local filesystem
- Replace `sahamatinet-agent-tls` with your desired secret name (must match `tls.secretName` in values.yaml)
- Skip this step if you don't need HTTPS (set `data.tls.https_enabled: false` in values.yaml)

**After creating the secret, update `helmchart/values.yaml`** to reference the secret name:

```yaml
tls:
  secretName: "sahamatinet-agent-tls"  # Must match the secret name created above
```

**Important**: If you created the secret with a different name, make sure `tls.secretName` in values.yaml matches exactly. The StatefulSet will automatically mount the TLS files from this secret.

### 4. Configure Required Paths in values.yaml

**MANDATORY**: Before deploying, you must set the following required paths in `helmchart/values.yaml`:

1. **Set `datastore_path`** (always required):
   ```yaml
   data:
     store:
       db:
         datastore_path: "/app/datastore"  # Set your desired path (directory will be created automatically)
   ```

2. **If HTTPS is enabled, set TLS certificate and key paths**:
   ```yaml
   data:
     tls:
       https_enabled: true
       cert_file: "/etc/tls/tls.crt"  # Path where TLS cert will be mounted
       key_file: "/etc/tls/tls.key"   # Path where TLS key will be mounted
   ```

**Important Notes**:
- The `datastore_path` can be any directory path you prefer (e.g., `/app/datastore`, `/data/db`, `/var/lib/sna`)
- The directory will be **automatically created** by Kubernetes when the volume is mounted
- If `datastore_path` is empty, the pod will **fail to start** with a validation error
- If `cert_file` or `key_file` are empty when HTTPS is enabled, the pod will **fail to start**
- You can use different paths, but ensure `cert_file` and `key_file` match where the TLS secret is mounted (default: `/etc/tls/`)

### 5. Deploy with Helm

```bash
cd helmchart
helm install sahamatinet-agent . --namespace sahamatinet-agent
```

**Or** from the repository root:

```bash
helm install sahamatinet-agent ./helmchart --namespace sahamatinet-agent
```

**Note**: Wait for approximately 1 minute after installation for the pod to become ready. The readiness probe performs health checks that may take some time to pass.

**Alternative**: If you prefer to set the TLS secret name via command line instead of editing values.yaml:
```bash
helm install sahamatinet-agent . --namespace sahamatinet-agent \
  --set tls.secretName=sahamatinet-agent-tls
```

### 6. Verify Deployment

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
| `store.db.datastore_path` | `""` | **REQUIRED** - Path for SQLite database storage (must be a directory path, e.g., `/app/datastore`) |
| `tls.https_enabled` | `true` | Enable/disable HTTPS |
| `tls.cert_file` | `""` | **REQUIRED if HTTPS enabled** - Path to TLS certificate file (mounted from secret, e.g., `/etc/tls/tls.crt`) |
| `tls.key_file` | `""` | **REQUIRED if HTTPS enabled** - Path to TLS key file (mounted from secret, e.g., `/etc/tls/tls.key`) |

**Important**: 
- These values are stored in a `config.yaml` file in the ConfigMap and mounted to the container.
- **`datastore_path` is MANDATORY** - The pod will fail to start if this is not set.
- **`cert_file` and `key_file` are MANDATORY** if `tls.https_enabled: true` - The pod will fail if these are not set when HTTPS is enabled.
- You must provide these paths in your `values.yaml` before deploying.

### SQLite Datastore Configuration

**IMPORTANT**: The `datastore_path` is **MANDATORY** and must be set in `values.yaml`. The pod will fail to start if this path is empty.

The SQLite database requires a directory path where the database file (`sna.db`) will be created. You have two options:

#### Option 1: Persistent Storage (Recommended for Production)

**Data persists across pod restarts** - Uses PersistentVolumeClaim (PVC)

1. **Enable persistence in values.yaml**:
   ```yaml
   persistence:
     enabled: true
     storageClass: ""  # Use default storage class, or specify your storage class
     accessMode: ReadWriteOnce
     size: 1Gi
   
   data:
     store:
       db:
         datastore_path: "/app/datastore"  # Path where volume will be mounted
   ```

2. **Deploy** - The PVC will be automatically created and mounted at `/app/datastore`

#### Option 2: Temporary Storage (For Testing Only)

**Data is lost on pod restart** - Uses emptyDir volume

1. **Disable persistence in values.yaml**:
   ```yaml
   persistence:
     enabled: false
   
   data:
     store:
       db:
         datastore_path: "/app/datastore"
   ```

2. **Manually create the directory** (if needed for testing):
   ```bash
   # After pod is running, exec into the pod
   kubectl exec -n sahamatinet-agent -it statefulset/sahamatinet-agent -- sh
   
   # Create the directory
   mkdir -p /app/datastore
   
   # Verify it exists
   ls -la /app/datastore
   ```

**Important Notes**:
- The `datastore_path` must be a **directory path**, not a file path
- The database file `sna.db` will be automatically created inside this directory
- For production, always use `persistence.enabled: true` to prevent data loss
- The directory is automatically created when the volume is mounted (no manual creation needed)

### TLS Configuration

**IMPORTANT**: If `tls.https_enabled: true`, both `cert_file` and `key_file` are **MANDATORY** and must be set in `values.yaml`. The pod will fail if these paths are empty when HTTPS is enabled.

If you want to enable HTTPS for the service, you need to:

1. **Create a Kubernetes Secret** with your TLS certificate and key:
   ```bash
   kubectl create secret tls sahamatinet-agent-tls \
     --cert=path/to/cert.pem \
     --key=path/to/key.pem \
     -n sahamatinet-agent
   ```
   **Note**: `path/to/cert.pem` and `path/to/key.pem` are local file paths on your machine where the certificate and key files are located.

2. **Update values.yaml** to reference the secret:
   ```yaml
   tls:
     secretName: sahamatinet-agent-tls  # Name of the secret created above
   
   data:
     tls:
       https_enabled: true
       cert_file: "/etc/tls/tls.crt"  # Path where cert will be mounted
       key_file: "/etc/tls/tls.key"   # Path where key will be mounted
   ```

3. **Deploy the chart** - The secret will be automatically mounted as files in the pod at `/etc/tls/`.

**Note**: 
- The secret must exist before deploying the chart
- If `tls.secretName` is empty, TLS will be disabled even if `https_enabled` is true
- The certificate and key files will be mounted at `/etc/tls/tls.crt` and `/etc/tls/tls.key` respectively
- **The paths in `tls.cert_file` and `tls.key_file` must match the mount paths** (typically `/etc/tls/tls.crt` and `/etc/tls/tls.key`)
- **Both `cert_file` and `key_file` are REQUIRED** - The pod will fail to start if these are empty when HTTPS is enabled

### Service Configuration

```yaml
service:
  type: ClusterIP      # Service type (ClusterIP, NodePort, or LoadBalancer)
  port: 4044          # Service port
  targetPort: 4044    # Container port
```

### Persistent Storage Configuration

```yaml
persistence:
  enabled: true        # Enable persistent volume (recommended for production)
  storageClass: ""     # Storage class name (empty = use default)
  accessMode: ReadWriteOnce
  size: 1Gi           # Storage size for SQLite database
```

**Note**: 
- Set `enabled: true` for production (data persists across pod restarts)
- Set `enabled: false` for testing (uses emptyDir, data lost on restart)
- The volume is automatically mounted at the path specified in `data.store.db.datastore_path`

### Security Context Configuration

The application runs as a non-root user (`sahamati`, UID 1000) for security. The security context is automatically configured:

```yaml
securityContext:
  runAsUser: 1000        # Run as user sahamati (matches Dockerfile)
  runAsGroup: 1000       # Run as group sahamati
  runAsNonRoot: true     # Security best practice
  fsGroup: 1000          # Ensures mounted volumes are writable by the user
```

**Important**: The `fsGroup: 1000` setting ensures that mounted volumes (especially the datastore PVC) have the correct permissions, allowing the non-root user to write to them. This is automatically configured and typically doesn't need to be changed.

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

### Option 4: Configure TLS/HTTPS

To enable HTTPS with TLS certificates:

1. **Create the TLS secret** (if not already created):
   ```bash
   kubectl create secret tls sahamatinet-agent-tls \
     --cert=path/to/cert.pem \
     --key=path/to/key.pem \
     -n sahamatinet-agent
   ```
   **Note**: `path/to/cert.pem` and `path/to/key.pem` are local file paths on your machine where the certificate and key files are located.

2. **Update values.yaml** or use command line:
   ```yaml
   tls:
     secretName: sahamatinet-agent-tls
   
   data:
     tls:
       https_enabled: true
       cert_file: "/etc/tls/tls.crt"
       key_file: "/etc/tls/tls.key"
   ```

   Or via command line:
   ```bash
   helm install sahamatinet-agent . --namespace sahamatinet-agent \
     --set tls.secretName=sahamatinet-agent-tls \
     --set data.tls.https_enabled=true
   ```

3. **Deploy** - The secret will be automatically mounted at `/etc/tls/` in the pod.

**Note**: The secret must exist before deployment. The certificate and key files from the secret will be mounted as `tls.crt` and `tls.key` respectively.

### Option 5: Configure SQLite Datastore and Persistent Storage

#### For Production (Data Persists Across Pod Restarts)

**Enable Persistent Volume Claim (PVC)**:

```yaml
persistence:
  enabled: true
  storageClass: ""  # Use default, or specify your storage class
  accessMode: ReadWriteOnce
  size: 1Gi

data:
  store:
    db:
      datastore_path: "/app/datastore"  # Path where volume will be mounted
```

**Deploy** - The PVC will be automatically created and the directory will be available at `/app/datastore`. The SQLite database file (`sna.db`) will be created automatically in this directory.

#### For Testing (Data Lost on Pod Restart)

**Use emptyDir volume**:

```yaml
persistence:
  enabled: false  # Uses emptyDir - data lost on pod restart

data:
  store:
    db:
      datastore_path: "/app/datastore"
```

**Manual Directory Creation (if needed)**:

After deployment, if the directory doesn't exist, you can create it manually:

```bash
# Exec into the pod
kubectl exec -n sahamatinet-agent -it statefulset/sahamatinet-agent -- sh

# Create the directory
mkdir -p /app/datastore

# Verify it exists
ls -la /app/datastore

# Exit
exit
```

**Important Notes**:
- When `persistence.enabled: true`, the directory is **automatically created** by Kubernetes when the volume is mounted
- When `persistence.enabled: false`, you may need to manually create the directory (as shown above)
- The `datastore_path` must be a **directory path** (e.g., `/app/datastore`), not a file path
- The database file `sna.db` will be automatically created by SQLite inside the specified directory
- For production, **always use `persistence.enabled: true`** to prevent data loss

## Upgrading Deployment

```bash
helm upgrade sahamatinet-agent . --namespace sahamatinet-agent
```

Or with custom values:

```bash
helm upgrade sahamatinet-agent . --namespace sahamatinet-agent -f custom-values.yaml
```

## Testing the APIs

Once deployed, you can test the four APIs:

### Method 1: Port Forward (Recommended)

```bash
# In one terminal - start port forwarding
kubectl port-forward -n sahamatinet-agent svc/sahamatinet-agent 4044:4044

# In another terminal - test the APIs
curl http://localhost:4044/sna/v1/ping
curl http://localhost:4044/sna/v1/version

# Entity Registration - Using Secret
curl -X POST http://localhost:4044/sna/v1/entity/register \
  -H "Content-Type: application/json" \
  -d '{
    "entity_id": "your-entity-id",
    "secret": "your-entity-secret"
  }'

# Entity Registration - Using Token
curl -X POST http://localhost:4044/sna/v1/entity/register \
  -H "Content-Type: application/json" \
  -d '{
    "entity_id": "your-entity-id",
    "token": "your-entity-token"
  }'

curl -X POST http://localhost:4044/sna/v1/aa \
  -H "Content-Type: application/json" \
  -d '{"callType":"requestIn",.......,"description":""}]}]}}}}'

# If calling from another pod
#given: sahamati-net-agent-pod-ip is the ip of the sahamatinet-agent pod

curl -X POST http://sahamati-net-agent-pod-ip:4044/sna/v1/aa \
  -H "Content-Type: application/json" \
  -d '{"callType":"requestIn",.......,"description":""}]}]}}}}'

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

# Entity Registration - Using Secret
curl -X POST https://your-domain.com/sna/v1/entity/register \
  -H "Content-Type: application/json" \
  -d '{
    "entity_id": "your-entity-id",
    "secret": "your-entity-secret"
  }'

# Entity Registration - Using Token
curl -X POST https://your-domain.com/sna/v1/entity/register \
  -H "Content-Type: application/json" \
  -d '{
    "entity_id": "your-entity-id",
    "token": "your-entity-token"
  }'

curl -X POST https://your-domain.com/sna/v1/aa \
  -H "Content-Type: application/json" \
  -d '{"callType":"requestIn",.......,"description":""}]}]}}}}'
```

**Note**: When using Kong/App Gateway ingress, the path prefix `/sna/v1/` is used for external access and routes to `/sna/v1/` internally.

## API Endpoints

The SahamatiNet Agent service provides four APIs:

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

### 3. Entity Registration API

**Endpoint**: `POST /sna/v1/entity/register`

**Description**: This API is used to register an entity with the SahamatiNet Agent (SNA). Entities can provide either a `secret` or a `token` directly. The SNA stores the secret in its database and uses it for token generation, which is required to push SLA input data to SahamatiNET.

**When to Call**:

**If using `secret`**:
- **Initial Setup**: Call this API when initializing your application/service to register your entity's secret with SNA
- **Secret Reset**: Call this API when you perform a secret reset to update the stored secret in SNA's database
- **Note**: Once the secret is registered, SNA will automatically generate tokens when the old token expires. You need to call this API again if your secret changes.

**If using `token`**:
- **Initial Setup**: Call this API when initializing your application/service to register your entity's token with SNA
- **Token Refresh**: Call this API **before your old token expires** (recommended: every 12 hours or as per your token expiration policy) to provide a new token
- **Note**: Since you are providing the token directly, you must proactively refresh it before expiration to ensure continuous service

**Request Body Options**:

**Option 1: Using Secret**
```json
{
  "entity_id": "your-entity-id",
  "secret": "your-entity-secret"
}
```

**Option 2: Using Token**
```json
{
  "entity_id": "your-entity-id",
  "token": "your-entity-token"
}
```

**Request Parameters**:

| Key | Type | Required | Description |
|-----|------|----------|-------------|
| `entity_id` | string | Yes | Unique identifier for your entity |
| `secret` | string | Conditional | Secret key for your entity (used for token generation). Either `secret` or `token` must be provided |
| `token` | string | Conditional | Authentication token for your entity. Either `secret` or `token` must be provided |

**Response**:
```json
{
  "message": "Entity registration received successfully",
  "entity_id": "your-entity-id",
  "status": "registered"
}
```

**Examples**:

**Using Secret**:
```bash
curl -X POST http://localhost:4044/sna/v1/entity/register \
  -H "Content-Type: application/json" \
  -d '{
    "entity_id": "your-entity-id",
    "secret": "your-entity-secret"
  }'
```

**Using Token**:
```bash
curl -X POST http://localhost:4044/sna/v1/entity/register \
  -H "Content-Type: application/json" \
  -d '{
    "entity_id": "your-entity-id",
    "token": "your-entity-token"
  }'
```

**Important Notes**:
- **Either `secret` OR `token` must be provided** - the request will fail if both are missing
- If both `secret` and `token` are provided, the `token` will be used
- **Using Secret**: The secret is securely stored in SNA's database and is used for generating authentication tokens. Tokens are automatically generated when the old token expires. You only need to call this API again if your secret changes.
- **Using Token**: You must call this API before your old token expires (recommended: every 12 hours or as per your token expiration policy) to ensure continuous service. SNA will use the provided token directly.
- Tokens are required for making SLA API calls
- You must call this API during your application/service initialization
- The `entity_id` is always required - the request will fail if it is missing or empty

### 4. Agent Request API

**Endpoint**: `POST /sna/v1/aa`

**Request Body**:
```json
{
  "callType": "requestIn",
  "route": "/FI/Notification",
  "peerId": "fip-xxx",
  "peerType": "FIP",
  "customerId": "customer_identifier@AA_identifier",
  "httpStatus": 0,
  "addlAttr": {},
  "body": {
    "ver": "2.0.0",
    "timestamp": "2023-06-26T11:39:57.153Z",
    "txnid": "0b811819-9044-4856-b0ee-8c88035f8858",
    "Notifier": {
      "type": "FIP",
      "id": "FIP-1"
    },
    "FIStatusNotification": {
      "sessionId": "XXXX0-XXXX-XXXX",
      "sessionStatus": "ACTIVE",
      "FIStatusResponse": [
        {
          "fipID": "FIP-1",
          "Accounts": [
            {
              "linkRefNumber": "XXXX-XXXX-XXXX",
              "FIStatus": "READY",
              "description": ""
            }
          ]
        }
      ]
    }
  }
}
```

## JSON Parameters

| Key | Type | Description |
|--------|----------|-------------|
| callType | string | type of transaction call |
| route | string | ReBit API endpoint route |
| peerId | string | entity id of the FIP |
| peerType | string | entity type of the peer - FIP |
| customerId | string | customer id related to the transaction |
| httpStatus | integer | the http status code, applicable only to responses |
| addlAttr | json | optional, reserved for future use |
| body| json | actual request or response body |


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
  -d '{"callType":"requestIn",.......,"description":""}]}]}}}}'
```

## API Summary Table

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/sna/v1/ping` | Health check endpoint |
| GET | `/sna/v1/version` | Get agent version information |
| POST | `/sna/v1/entity/register` | Register entity secret (call during initialization and secret reset) |
| POST | `/sna/v1/aa` | Handle agent requests |

## Troubleshooting

### Required paths not set

**Error**: Pod fails to start with validation errors or "mountPath cannot be empty"

**Cause**: The following paths are **MANDATORY** and must be set in `values.yaml`:
- `data.store.db.datastore_path` - **ALWAYS REQUIRED**
- `data.tls.cert_file` - **REQUIRED if `tls.https_enabled: true`**
- `data.tls.key_file` - **REQUIRED if `tls.https_enabled: true`**

**Solution**:
1. **Set `datastore_path`** (always required):
   ```yaml
   data:
     store:
       db:
         datastore_path: "/app/datastore"  # Must not be empty - pod will fail if empty
   ```

2. **If HTTPS is enabled, set TLS paths**:
   ```yaml
   data:
     tls:
       https_enabled: true
       cert_file: "/etc/tls/tls.crt"  # Must not be empty - pod will fail if empty
       key_file: "/etc/tls/tls.key"   # Must not be empty - pod will fail if empty
   ```

3. **Verify all required paths are set** before deploying:
   ```bash
   # Check values.yaml
   grep -A 3 "datastore_path:" helmchart/values.yaml
   grep -A 3 "cert_file:" helmchart/values.yaml
   grep -A 3 "key_file:" helmchart/values.yaml
   ```

### SQLite datastore path issues

**Error**: `data store path empty` or `db is not writable`

**Solution**:
1. **Check the datastore_path is set** in values.yaml:
   ```yaml
   data:
     store:
       db:
         datastore_path: "/app/datastore"  # Must not be empty
   ```

2. **If using emptyDir (testing)**, manually create the directory:
   ```bash
   kubectl exec -n sahamatinet-agent -it statefulset/sahamatinet-agent -- mkdir -p /app/datastore
   ```

3. **If using PVC**, verify the volume is mounted:
   ```bash
   kubectl describe pod -n sahamatinet-agent -l app.kubernetes.io/name=sahamatinet-agent | grep -A 5 "Mounts:"
   ```

4. **Check PVC status** (if persistence is enabled):
   ```bash
   kubectl get pvc -n sahamatinet-agent
   kubectl describe pvc -n sahamatinet-agent datastore-sahamatinet-agent-0
   ```

5. **Verify directory exists in pod**:
   ```bash
   kubectl exec -n sahamatinet-agent -it statefulset/sahamatinet-agent -- ls -la /app/datastore
   ```

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
