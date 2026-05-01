# Deployment Guide — Simple Hearing Backend

Helm-based deployment to Google Kubernetes Engine (GKE) with Cloud SQL (PostgreSQL).

---

## Architecture Overview

```
GitHub Actions
    │
    ├─ Build multi-stage Docker image
    ├─ Push to Artifact Registry (asia-south1)
    └─ helm upgrade --install → GKE cluster
                                    │
                                    └─ Pod
                                         ├─ API container (Spring Boot)
                                         └─ Cloud SQL Auth Proxy (sidecar)
                                                    │
                                                    └─ Cloud SQL (PostgreSQL 16)
```

---

## Prerequisites

```bash
# Install tools
brew install --cask google-cloud-sdk
brew install helm kubectl

# Login
gcloud auth login
gcloud auth application-default login
gcloud config set project YOUR_PROJECT_ID
```

---

## Phase 1 — One-Time GCP Infrastructure Setup

### 1. Enable required APIs

```bash
gcloud services enable \
  sqladmin.googleapis.com \
  container.googleapis.com \
  artifactregistry.googleapis.com \
  secretmanager.googleapis.com
```

### 2. Create Artifact Registry

```bash
gcloud artifacts repositories create simple-hearing \
  --repository-format=docker \
  --location=asia-south1 \
  --description="Simple Hearing API images"
```

### 3. Create GKE cluster

```bash
gcloud container clusters create simple-hearing-cluster \
  --region=asia-south1 \
  --num-nodes=1 \
  --machine-type=e2-small \
  --enable-ip-alias \
  --workload-pool=YOUR_PROJECT_ID.svc.id.goog
```

### 4. Create Cloud SQL instance

```bash
gcloud sql instances create simple-hearing-db \
  --database-version=POSTGRES_16 \
  --tier=db-f1-micro \
  --region=asia-south1 \
  --storage-type=SSD \
  --storage-size=10GB \
  --no-assign-ip \
  --network=default

# Create database and user
gcloud sql databases create simplehearing --instance=simple-hearing-db
gcloud sql users create simplehearing \
  --instance=simple-hearing-db \
  --password=YOUR_STRONG_PASSWORD
```

### 5. Create GCP Service Account for the app

```bash
# Create the SA
gcloud iam service-accounts create simple-hearing-api \
  --display-name="Simple Hearing API"

# Allow it to connect to Cloud SQL
gcloud projects add-iam-policy-binding YOUR_PROJECT_ID \
  --member="serviceAccount:simple-hearing-api@YOUR_PROJECT_ID.iam.gserviceaccount.com" \
  --role="roles/cloudsql.client"

# Bind Kubernetes SA to GCP SA (Workload Identity)
gcloud iam service-accounts add-iam-policy-binding \
  simple-hearing-api@YOUR_PROJECT_ID.iam.gserviceaccount.com \
  --role="roles/iam.workloadIdentityUser" \
  --member="serviceAccount:YOUR_PROJECT_ID.svc.id.goog[simple-hearing/simple-hearing-api]"
```

### 6. Create Kubernetes secrets

```bash
# Connect to cluster
gcloud container clusters get-credentials simple-hearing-cluster --region=asia-south1

# Create namespace
kubectl create namespace simple-hearing

# DB credentials
# DB_URL points to 127.0.0.1 because Cloud SQL Proxy runs as a sidecar on the same pod
kubectl create secret generic simple-hearing-db-credentials \
  --from-literal=DB_URL='jdbc:postgresql://127.0.0.1:5432/simplehearing' \
  --from-literal=DB_USER='simplehearing' \
  --from-literal=DB_PASS='YOUR_STRONG_PASSWORD' \
  --namespace=simple-hearing

# JWT secret
kubectl create secret generic simple-hearing-jwt \
  --from-literal=JWT_SECRET='YOUR_JWT_SECRET' \
  --namespace=simple-hearing
```

### 7. Set up Workload Identity for GitHub Actions

```bash
# Create CI/CD service account
gcloud iam service-accounts create github-actions \
  --display-name="GitHub Actions deployer"

# Grant required roles
gcloud projects add-iam-policy-binding YOUR_PROJECT_ID \
  --member="serviceAccount:github-actions@YOUR_PROJECT_ID.iam.gserviceaccount.com" \
  --role="roles/artifactregistry.writer"

gcloud projects add-iam-policy-binding YOUR_PROJECT_ID \
  --member="serviceAccount:github-actions@YOUR_PROJECT_ID.iam.gserviceaccount.com" \
  --role="roles/container.developer"

# Create Workload Identity pool
gcloud iam workload-identity-pools create github-pool \
  --location=global \
  --display-name="GitHub Actions pool"

gcloud iam workload-identity-pools providers create-oidc github-provider \
  --location=global \
  --workload-identity-pool=github-pool \
  --display-name="GitHub provider" \
  --issuer-uri="https://token.actions.githubusercontent.com" \
  --attribute-mapping="google.subject=assertion.sub,attribute.repository=assertion.repository"

# Allow your repo to impersonate the SA
gcloud iam service-accounts add-iam-policy-binding \
  github-actions@YOUR_PROJECT_ID.iam.gserviceaccount.com \
  --role="roles/iam.workloadIdentityUser" \
  --member="principalSet://iam.googleapis.com/projects/YOUR_PROJECT_NUMBER/locations/global/workloadIdentityPools/github-pool/attribute.repository/YOUR_GITHUB_ORG/YOUR_REPO"
```

---

## Phase 2 — GitHub Repository Secrets

Go to **Settings → Secrets and variables → Actions** and add:

| Secret | Value |
|--------|-------|
| `GCP_PROJECT_ID` | Your GCP project ID |
| `GCP_WORKLOAD_IDENTITY_PROVIDER` | `projects/PROJECT_NUMBER/locations/global/workloadIdentityPools/github-pool/providers/github-provider` |
| `GCP_SERVICE_ACCOUNT` | `github-actions@YOUR_PROJECT_ID.iam.gserviceaccount.com` |

---

## Phase 3 — Configure Helm Values

Update `helm/simple-hearing/values.yaml` — replace the three placeholders:

```yaml
image:
  repository: asia-south1-docker.pkg.dev/YOUR_PROJECT_ID/simple-hearing/api

cloudSql:
  connectionName: "YOUR_PROJECT_ID:asia-south1:simple-hearing-db"

serviceAccount:
  gcpServiceAccount: simple-hearing-api@YOUR_PROJECT_ID.iam.gserviceaccount.com
```

---

## Phase 4 — Deploy

### Automatic (CI/CD)

Push to `main`. The GitHub Actions workflow (`.github/workflows/deploy.yml`) runs automatically:

```
push to main
    │
    ├─ Build Docker image (multi-stage)
    ├─ Push to Artifact Registry (tagged with git SHA)
    ├─ helm upgrade --install
    │     ├─ Deployment (API + Cloud SQL proxy sidecar)
    │     ├─ Service
    │     ├─ Ingress
    │     └─ HPA (enabled in values-production.yaml)
    └─ Liquibase applies any pending migrations on pod startup
```

### Manual (first deploy or hotfix)

```bash
# Build and push image
docker build -t asia-south1-docker.pkg.dev/YOUR_PROJECT_ID/simple-hearing/api:latest .
docker push asia-south1-docker.pkg.dev/YOUR_PROJECT_ID/simple-hearing/api:latest

# Connect to cluster
gcloud container clusters get-credentials simple-hearing-cluster --region=asia-south1

# Deploy
helm upgrade --install simple-hearing helm/simple-hearing \
  --namespace simple-hearing \
  --create-namespace \
  -f helm/simple-hearing/values.yaml \
  --wait \
  --timeout 5m
```

### Production deploy

```bash
helm upgrade --install simple-hearing helm/simple-hearing \
  --namespace simple-hearing \
  -f helm/simple-hearing/values.yaml \
  -f helm/simple-hearing/values-production.yaml \
  --set image.tag=YOUR_GIT_SHA \
  --wait
```

---

## Verify Deployment

```bash
# Check pods are running
kubectl get pods -n simple-hearing

# Check logs
kubectl logs -n simple-hearing -l app.kubernetes.io/name=simple-hearing -c api

# Check Cloud SQL proxy sidecar
kubectl logs -n simple-hearing -l app.kubernetes.io/name=simple-hearing -c cloud-sql-proxy

# Hit the health endpoint
kubectl port-forward -n simple-hearing svc/simple-hearing 8080:80
curl http://localhost:8080/actuator/health
```

---

## Rollback

```bash
# List releases
helm history simple-hearing -n simple-hearing

# Rollback to previous release
helm rollback simple-hearing -n simple-hearing
```

---

## File Structure

```
backend/
├── Dockerfile                          # Multi-stage build
├── DEPLOYMENT.md                       # This file
├── helm/
│   └── simple-hearing/
│       ├── Chart.yaml
│       ├── values.yaml                 # Base config (all environments)
│       ├── values-production.yaml      # Production overrides (2 replicas, HPA)
│       └── templates/
│           ├── _helpers.tpl
│           ├── deployment.yaml         # App + Cloud SQL proxy sidecar
│           ├── service.yaml
│           ├── ingress.yaml
│           ├── hpa.yaml
│           ├── serviceaccount.yaml     # Workload Identity annotation
│           └── secret.yaml             # Secret creation instructions
└── .github/workflows/
    └── deploy.yml                      # Build → push → helm deploy pipeline
```

---

## Cost Estimate (Mumbai — asia-south1)

| Resource | Config | ~Monthly |
|----------|--------|----------|
| GKE cluster | 1 × e2-small node | ~$15 USD |
| Cloud SQL | db-f1-micro, 10GB SSD | ~$9 USD |
| Artifact Registry | < 1GB storage | ~$0.10 USD |
| **Total (dev/staging)** | | **~$24 USD** |
| GKE cluster (prod) | 2 × e2-medium nodes | ~$50 USD |
| Cloud SQL (prod) | db-g1-small, 20GB SSD | ~$27 USD |
| **Total (production)** | | **~$80 USD** |
