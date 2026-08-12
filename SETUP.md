# SahamatiNet Agent - Deployment Guide

Guide to deploy SahamatiNet Agent using Helm chart in Kubernetes.

## Prerequisites

- Kubernetes cluster (v1.20 or higher)
- Helm 3.x installed
- `kubectl` configured to access your cluster

## Docker Image

**Image**: `sahamatidevsecops/sna:v2.1.5`

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

### 4. Create the credentials file with entityId and secret.

This is required for SNA to generate a token for sending the SLA input to Sahamati API.

**Important**: 
This file has to be updated everytime the secret is renewed.

```bash
kubectl create secret generic sahamatinet-agent-credentials -n sahamatinet-agent 
--from-literal=credentials.json='{"entityId":"<your-entity-id>","entitySecret":"<your-entity-secret>"}' 
--dry-run=client -o yaml | kubectl apply -n sahamatinet-agent -f -
```

### 5. Configure Required Paths in values.yaml

**MANDATORY**: Before deploying, you must set the following required paths in `helmchart/values.yaml`:

1. **Set `datastore_path`** (always required):
   ```yaml
   data:
     store:
       db:
         datastore_path: "/app/datastore"  # Set your desired path (directory will be created automatically)
   ```

2. **Configure the credentials secret name**
   ```yaml
   # Mount credentials.json from a Secret (for entityConfig.credentials)
   credentialsSecret:
     secretName: "sahamatinet-agent-credentials"   # e.g. sahamatinet-agent-credentials
     secretKey: "credentials.json"  # key in the Secret that holds the file content
   ```

3. **If HTTPS is enabled, set TLS certificate and key paths**:
   ```yaml
   data:
     tls:
       https_enabled: true
       cert_file: "/etc/tls/tls.crt"  # Path where TLS cert will be mounted
       key_file: "/etc/tls/tls.key"   # Path where TLS key will be mounted
   ```
   Note: If both app and sna are running within the same cluster, we recommend to run sna in http mode.

4. **Configure the SLA API backend URL**:
   ```yaml
   data:
     sla:
      sla_api_url: "" #e.g. https://api.dev.sahamati.org.in/sla-inputs/aa/v1/push"
   ```

5. **Configure the Token fetch URL**:
   ```yaml
   data:
     sla:
      token_generation_base_url: "" # e.g. https://api.dev.sahamati.org.in/iam/v1"
   ```

**Important Notes**:
- The `datastore_path` can be any directory path (e.g., `/app/datastore`, `/data/db`, `/var/lib/sna`)
- It is preferred and recommended that it be a path in the persistent volume storage, to ensure that no data is lost on restarts.
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
| `store.db.datastore_path` | `""` | **REQUIRED** - Path for disk-based database storage (must be a directory path, e.g., `/app/datastore`) - A persistent volume |
| `tls.https_enabled` | `true` | Enable/disable HTTPS |
| `tls.cert_file` | `""` | **REQUIRED if HTTPS enabled** - Path to TLS certificate file (mounted from secret, e.g., `/etc/tls/tls.crt`) |
| `tls.key_file` | `""` | **REQUIRED if HTTPS enabled** - Path to TLS key file (mounted from secret, e.g., `/etc/tls/tls.key`) |
|`data.sla.sla_api_url` | `""` | **REQUIRED** - Sahamati's SLA API to which the sla inputs are pushed |
|`data.sla.token_generation_base_url` | `""` | **REQUIRED** - Sahamati's Entity token generation baseURL |

**Important**: 
- These values are stored in a `config.yaml` file in the ConfigMap and mounted to the container.
- **`datastore_path` is MANDATORY** - The pod will fail to start if this is not set.
- **`cert_file` and `key_file` are MANDATORY** if `tls.https_enabled: true` - The pod will fail if these are not set when HTTPS is enabled.
- You must provide these paths in your `values.yaml` before deploying.

### Sequencer Configuration (Optional)

The **sequencer** is an optional, opt-in component inside SNA that sits in front of the normal
request-handling pipeline and re-orders events that can arrive out of order, before forwarding
them unchanged. It runs in two stages:

- **Stage A (TxnSequencer)** — pairs a request leg with its response leg by `txnCorId`, for every
  route, so a leg that arrives before its pair isn't dropped.
- **Stage B (SessionSequencer)** — orders `FIR -> FIN -> FIF` events within a session
  (`fipID` + `sessionID`), releasing them downstream in that order once a session's events are
  complete, or force-releasing whatever is held once a session goes stale (via a periodic
  collector sweep).

Both stages are sticky-routed (hashed by `txnCorId` / `fipID+sessionID`) across a configurable
number of worker goroutines, so ordering decisions for a given transaction/session are always
made by the same worker. **When disabled (the default), the sequencer has no effect on request
handling.**

It is configured in `config.yaml` under `process_config`:

```yaml
process_config:
  modes:
    sequencer_enabled: true   # opt-in, off by default
  sequencer_config:
    txn_pair_ttl: "1m"            # Stage A: max wait for a lone leg's pair before it's dropped
    session_max_age: "1m"         # Stage B: max time an incomplete session is held before force-release
    collector_interval: "1m"      # how often the stale-session sweep runs
    txn_queue_size: 20000         # Stage A per-worker buffered channel size
    session_queue_size: 20000     # Stage B per-worker buffered channel size
    txn_worker_count: 1           # Stage A worker goroutines
    session_worker_count: 1       # Stage B worker goroutines
    queue_enqueue_timeout: "100ms" # non-blocking enqueue timeout before a drop is logged
```

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `process_config.modes.sequencer_enabled` | bool | `false` | Enables the sequencer. Off by default. |
| `sequencer_config.txn_pair_ttl` | duration | `1m` | Stage A: max time to wait for a lone req/resp leg's pair before dropping it |
| `sequencer_config.session_max_age` | duration | `1m` | Stage B: max time a session may sit incomplete before the collector force-releases it |
| `sequencer_config.collector_interval` | duration | `1m` | How often the stale-session sweep runs |
| `sequencer_config.txn_queue_size` | int | `20000` | Stage A per-worker buffered channel size |
| `sequencer_config.session_queue_size` | int | `20000` | Stage B per-worker buffered channel size |
| `sequencer_config.txn_worker_count` | int | `1` | Stage A worker goroutines |
| `sequencer_config.session_worker_count` | int | `1` | Stage B worker goroutines |
| `sequencer_config.queue_enqueue_timeout` | duration | `100ms` | Non-blocking enqueue timeout before a drop is logged |

All `sequencer_config` fields are optional and fall back to the defaults above if omitted or
invalid. Keep `collector_interval` at or below `session_max_age` — otherwise stale sessions are
force-released later than intended (SNA logs a startup warning if this isn't the case).

**Note (Helm deployments)**: The current `helmchart/templates/configmap.yaml` does not yet expose
`process_config`/`sequencer_config` as `values.yaml` fields. To enable the sequencer in a
Helm-based deployment today, add the `process_config` block above directly under `config:` in
`helmchart/templates/configmap.yaml` (or a values-driven equivalent) before installing/upgrading
the chart. For Docker Compose deployments, edit `config.yaml` directly — see
`docker/SETUP-DOCKER.md`.

### BadgerDB Datastore Configuration

**IMPORTANT**: The `datastore_path` is **MANDATORY** and must be set in `values.yaml`. The pod will fail to start if this path is empty.

The BadgerDb database requires a directory path where the database file (`sna.db`) will be created. 

You have two options:

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
  size: 1Gi           # Storage size for BadgerDB database
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
    cpu: "5"
    memory: 6Gi
  requests:
    cpu: 200m
    memory: 4Gi
```

### Replica Count

```yaml
replicaCount: 1  # Number of pod replicas
```

### Horizontal Pod Autoscaler (HPA)

Disable Autoscaling at present. Only one instance should be running.

```yaml
hpa:
  enabled: false       # Disable Enable/disable HPA
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

### Option 5: Configure BadgerDB Datastore and Persistent Storage

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

**Deploy** - The PVC will be automatically created and the directory will be available at `/app/datastore`. The Badger database file will be created automatically in this directory.

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
- The database file will be created by SNA inside the specified directory
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

### Method 1: Port Forward

```bash
# In one terminal - start port forwarding
kubectl port-forward -n sahamatinet-agent svc/sahamatinet-agent 4044:4044

# In another terminal - test the APIs
curl http://localhost:4044/sna/v1/ping
curl http://localhost:4044/sna/v1/version

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

### 3. Agent Request API

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
  "txnCorId": "txn-uuid-1234",
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
| txnCorId | string | uuid - txn correlation id mapping a request to a response |
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

### BadgerDB datastore path issues

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
