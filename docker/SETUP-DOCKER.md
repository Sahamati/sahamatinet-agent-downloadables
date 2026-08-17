# SahamatiNet Agent - Docker Deployment Guide

Deploying the SahamatiNet Agent (SNA) with Docker Compose on a single host, for
environments without Kubernetes. If you run Kubernetes, use the Helm chart and
[SETUP.md](../SETUP.md) instead.

The agent is a single stateless-front / local-store service. It does not require
a cluster, an orchestrator, or an external database.

---

## 1. Requirements

**Host**

| | |
|---|---|
| OS | Any Linux with Docker Engine 20.10+ (Amazon Linux 2023, Ubuntu 22.04+ tested patterns) |
| Docker Compose | v2 (`docker compose version`). Ships with current Docker Engine. |
| Architecture | `amd64` or `arm64` - the image is published for both, so Graviton instances work |
| vCPU | 2 minimum, 4 recommended |
| RAM | 4 GB minimum, 8 GB recommended |
| Disk | 20 GB. The datastore itself is small, but BadgerDB pre-allocates a 2 GB sparse value log, so do not provision a tiny volume. |

An EC2 `t3.large` / `t4g.large` or larger is a reasonable starting point.

**Network egress** - the agent must be able to reach, outbound over HTTPS (443):

- The Sahamati IAM endpoint (`token_generation_base_url`) - to obtain a token
- The Sahamati SLA ingest endpoint (`sla_api_url`) - to push SLA inputs

If your environment restricts outbound traffic, both hosts must be allowlisted in
the security group / NAT / proxy configuration. **The agent will start
successfully but silently fail to deliver SLA inputs if this egress is blocked** -
verify it explicitly (see section 7).

**Inbound** - only your own AA application needs to reach port 4044. Nothing in
the SahamatiNet ecosystem initiates connections to the agent.

---

## 2. What you need from Sahamati

Obtain these during onboarding before you begin:

1. **Entity ID** and **Entity Secret** for your entity
2. The **SLA API URL** and **token generation base URL** for your target
   environment (the values shipped in `config/config.yaml` point at the
   development environment)

---

## 3. Install

### 3.1 Get the files

```bash
git clone https://github.com/Sahamati/sahamatinet-agent-downloadables.git
cd sahamatinet-agent-downloadables/docker
```

### 3.2 Create the credentials file

```bash
cp secrets/credentials.json.example secrets/credentials.json
```

Edit `secrets/credentials.json` with the values issued to you:

```json
{
  "entityId": "your-entity-id",
  "entitySecret": "your-entity-secret"
}
```

Then restrict access to it:

```bash
chmod 600 secrets/credentials.json
```

> **When your entity secret is rotated, update this file and restart the agent:**
>
> ```bash
> docker compose restart sna
> ```
>
> The credentials are read once at startup, so updating the file alone has no
> effect on a running agent. This file is excluded from version control - never
> commit it.

### 3.3 Configure the agent

Two configuration templates ship with the agent:

| Template | Environment |
|----------|-------------|
| `config/config-prod.yaml` | Production |
| `config/config-uat.yaml` | UAT |

The agent reads `config/config.yaml` and nothing else. Copy the template for
your environment over it:

```bash
# Production
cp config/config-prod.yaml config/config.yaml

# UAT
cp config/config-uat.yaml config/config.yaml
```

Then edit `config/config.yaml`, not the template.

The templates differ in three places: the SLA endpoints, `LOG_LEVEL`, and
`capture_txn`. Everything else is identical between them.

Confirm the SLA endpoints in your copy match what Sahamati issued you during
onboarding:

```yaml
  sla:
    sla_api_url: "<provided by Sahamati>"
    token_generation_base_url: "<provided by Sahamati>"
```

Every other value has a working default. Review the TLS section (3.4) before
going to production.

Three logging settings are worth knowing about, all documented inline in
`config/config.yaml`:

| Setting | Default | Description |
|---------|---------|-------------|
| `LOG_LEVEL` | `INFO` | Log verbosity - `DEBUG`, `INFO`, `WARN`, `ERROR` or `FATAL`. Any other value falls back to `INFO` |
| `processConfig.log_transactions` | `true` | Master switch for transaction logging. Writes one pipe-separated record per transaction, customer ID masked |
| `processConfig.developer_config.capture_txn` | `false` | Debug add-on to `log_transactions`; no effect unless that is `true`. Additionally dumps the full request body with the customer ID **unmasked**. Keep off outside local debugging |

### 3.4 Decide where TLS terminates

The shipped configuration runs the agent over **HTTP**, which is the
recommendation when the agent and your AA application are on the same host or in
the same private subnet. Terminate TLS at your load balancer or reverse proxy.

To terminate TLS at the agent instead:

1. Place your certificate and key at `certs/tls.crt` and `certs/tls.key`
2. Uncomment the `./certs:/etc/tls:ro` mount in `docker-compose.yml`
3. Switch the healthcheck to the HTTPS variant noted in the same file
4. Set `tls.https_enabled: true` in `config/config.yaml`

### 3.5 Start

```bash
docker compose up -d
```

Two containers appear. `sna-init` runs once, sets ownership on the datastore
volume, and exits - this is expected. `sahamatinet-agent` is the long-running
service.

Confirm it is healthy (allow ~30 seconds):

```bash
docker compose ps
```

```
NAME                STATUS
sahamatinet-agent   Up 45 seconds (healthy)
```

---

## 4. Verify

```bash
curl http://localhost:4044/sna/v1/ping
# {"status":"up"}

curl http://localhost:4044/sna/v1/version
# {"agentVersion":"1.0.0","name":"SahamatiNet Agent (SNA)"}
```

Then check the logs for successful token acquisition and SLA push cycles:

```bash
docker compose logs -f sna
```

API request/response formats are documented in
[SETUP.md](../SETUP.md#api-endpoints) - they are identical regardless of how the
agent is deployed.

---

## 5. Connecting your AA application

**Same host, your app also in Docker** - attach your application container to
the `sna-net` network and call the agent by name. Remove the `ports:` mapping
from `docker-compose.yml` entirely so the agent is not exposed to the host at
all.

```
http://sahamatinet-agent:4044/sna/v1/aa
```

**Same host, your app running natively** - keep the default loopback binding.

```
http://127.0.0.1:4044/sna/v1/aa
```

**Different host in your VPC** - change the port mapping to the instance's
private IP and restrict the security group to your application's host only:

```yaml
ports:
  - "10.0.1.25:4044:4044"
```

> Do not bind the agent to `0.0.0.0` on a public subnet. The API has no
> authentication of its own - it assumes it sits inside your trust boundary.

---

## 6. Operations

### Constraint: exactly one instance

The agent uses BadgerDB, a single-writer embedded store. Only one container may
use the datastore at a time, and a second instance will fail to acquire the lock.
Do not run `docker compose up --scale sna=2`, and do not point a second host at a
shared copy of the datastore. This mirrors the Helm chart, where autoscaling is
disabled for the same reason.

### Routine commands

```bash
docker compose ps                 # status
docker compose logs -f sna        # follow logs
docker compose restart sna        # restart, datastore preserved
docker compose down               # stop and remove containers, datastore preserved
docker compose down -v            # DESTRUCTIVE: also deletes the datastore volume
```

The agent restarts automatically if it crashes or the host reboots
(`restart: unless-stopped`).

### Upgrading

```bash
# 1. Back up the datastore first (see below)
# 2. Update the image tag in docker-compose.yml (both services)
# 3. Apply
docker compose pull
docker compose up -d
```

The `sna-datastore` volume is untouched by upgrades. To roll back, set the
previous tag and re-run the same two commands.

### Backing up the datastore

In Kubernetes this was handled by the storage layer. On a single Docker host it
is **your responsibility** - the datastore lives in a Docker volume on the
instance's disk, and nothing snapshots it unless you arrange it. If the instance
is terminated or the disk is lost, any state the agent had not yet pushed is lost
with it.

Two options:

**EBS snapshots** (simplest) - schedule snapshots of the instance's volume
through AWS Data Lifecycle Manager. Covers the datastore along with everything
else on the host.

**Volume-level backup** - stop the agent briefly for a consistent copy:

```bash
docker compose stop sna
docker run --rm \
  -v sna-datastore:/data:ro \
  -v "$(pwd)":/backup \
  alpine tar czf /backup/sna-datastore-$(date +%F).tar.gz -C /data .
docker compose start sna
```

Restore by extracting the archive back into a fresh `sna-datastore` volume with
the agent stopped, then running `docker compose up -d` (the init container will
correct ownership).

### Logs

Container logs rotate at 50 MB × 5 files. To ship them to CloudWatch instead,
replace the `logging:` block in `docker-compose.yml` with the `awslogs` driver.

---

## 7. Troubleshooting

**Container is `unhealthy` or restarting**

```bash
docker compose logs sna --tail 100
```

**`permission denied` writing to the datastore**

The `sna-init` container did not run or did not complete. Confirm it exited
cleanly, then recreate the stack:

```bash
docker compose down && docker compose up -d
```

**`Cannot acquire directory lock` / `Another process is using this Badger database`**

Two containers are pointed at the same datastore. Confirm only one is running:

```bash
docker ps --filter volume=sna-datastore
```

**Agent is healthy but SLA inputs are not arriving at Sahamati**

`/sna/v1/ping` only proves the HTTP server is up - it does not test outbound
connectivity. Verify egress from inside the container:

```bash
# Use the token_generation_base_url from your config.yaml
docker exec sahamatinet-agent curl -sS -o /dev/null -w '%{http_code}\n' \
  https://api.sahamati.org.in/iam/v1
```

A timeout or DNS failure means the security group, NAT, or proxy is blocking
egress. Also confirm `secrets/credentials.json` holds a current, unrotated
secret - an expired secret produces token failures in the logs.

**`range of CPUs is from 0.01 to N`**

A `cpus:` limit in `docker-compose.yml` exceeds the instance's vCPU count. The
limits are commented out by default; if you enabled them, lower the value.

---

## 8. Security notes

- `secrets/credentials.json` contains your entity secret. Keep it at mode `600`,
  never commit it, and never include it in a support bundle or log excerpt.
- The agent runs as a non-root user (uid 1000) inside the container. Do not
  override this.
- The container also starts an internal **debug server on port 9090**. The
  supplied `docker-compose.yml` does not publish it, and it must not be exposed.
  Do not add a port mapping for 9090.
- Payloads passing through the agent contain regulated financial data. Ensure
  container logs are handled under the same retention and access controls as the
  rest of your AA infrastructure.

---

## 9. Support

For issues or questions, contact the Sahamati team. When reporting a problem,
include the output of `docker compose ps` and `docker compose logs sna --tail 100`,
**with any credentials, tokens, and customer identifiers redacted**.
