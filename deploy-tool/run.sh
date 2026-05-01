#!/usr/bin/env bash
# =============================================================================
# GCP Deploy Tool
# Deploys any containerised application to Google Cloud (GKE + Cloud SQL)
# Usage: ./run.sh
# =============================================================================
set -euo pipefail

# ── Colours ───────────────────────────────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
BLUE='\033[0;34m'; CYAN='\033[0;36m'; BOLD='\033[1m'; NC='\033[0m'

# ── Helpers ───────────────────────────────────────────────────────────────────
banner()  { echo -e "\n${BOLD}${BLUE}══════════════════════════════════════${NC}"; \
            echo -e "${BOLD}${BLUE}  $1${NC}"; \
            echo -e "${BOLD}${BLUE}══════════════════════════════════════${NC}\n"; }
step()    { echo -e "\n${CYAN}▶  $1${NC}"; }
ok()      { echo -e "${GREEN}✔  $1${NC}"; }
warn()    { echo -e "${YELLOW}⚠  $1${NC}"; }
die()     { echo -e "${RED}✘  $1${NC}"; exit 1; }
ask()     { echo -e "${YELLOW}?  $1${NC}"; }

confirm() {
    ask "$1 (y/n)"
    read -r answer
    [[ "$answer" =~ ^[Yy]$ ]]
}

# ── Load config ───────────────────────────────────────────────────────────────
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="$SCRIPT_DIR/config.env"

if [[ ! -f "$CONFIG_FILE" ]]; then
    warn "config.env not found."
    echo -e "  Copying example config — fill it in then re-run this script.\n"
    cp "$SCRIPT_DIR/config.env.example" "$CONFIG_FILE"
    echo -e "  ${BOLD}Edit this file:${NC} $CONFIG_FILE\n"
    exit 0
fi

# shellcheck source=/dev/null
source "$CONFIG_FILE"

# ── Validate required fields ──────────────────────────────────────────────────
check_required() {
    local missing=()
    for var in APP_NAME GCP_PROJECT_ID GCP_REGION K8S_CLUSTER_NAME \
               K8S_NAMESPACE DB_INSTANCE_NAME DB_NAME DB_USER DB_PASS; do
        [[ -z "${!var:-}" ]] && missing+=("$var")
    done
    if [[ ${#missing[@]} -gt 0 ]]; then
        die "Missing required config values: ${missing[*]}\n  Edit $CONFIG_FILE and try again."
    fi
}

# ── Dependency check ──────────────────────────────────────────────────────────
check_deps() {
    step "Checking required tools are installed"
    local missing=()
    for cmd in gcloud docker helm kubectl; do
        if command -v "$cmd" &>/dev/null; then
            ok "$cmd found"
        else
            missing+=("$cmd")
        fi
    done

    if [[ ${#missing[@]} -gt 0 ]]; then
        echo ""
        warn "Missing tools: ${missing[*]}"
        echo "  Install them with:"
        [[ " ${missing[*]} " =~ " gcloud " ]]  && echo "    brew install --cask google-cloud-sdk"
        [[ " ${missing[*]} " =~ " docker " ]]  && echo "    brew install --cask docker"
        [[ " ${missing[*]} " =~ " helm " ]]    && echo "    brew install helm"
        [[ " ${missing[*]} " =~ " kubectl " ]] && echo "    brew install kubectl"
        exit 1
    fi
}

# ── Main menu ─────────────────────────────────────────────────────────────────
show_menu() {
    banner "GCP Deploy Tool — ${APP_NAME}"
    echo -e "  Project : ${BOLD}${GCP_PROJECT_ID}${NC}"
    echo -e "  Region  : ${BOLD}${GCP_REGION}${NC}"
    echo -e "  App     : ${BOLD}${APP_NAME}${NC}"
    echo ""
    echo -e "  ${BOLD}What would you like to do?${NC}"
    echo ""
    echo -e "  ${CYAN}1)${NC} Full setup        — First time? Run this. Sets up everything on GCP."
    echo -e "  ${CYAN}2)${NC} Deploy            — Build image and deploy latest code."
    echo -e "  ${CYAN}3)${NC} Status            — Check if the app is running."
    echo -e "  ${CYAN}4)${NC} Logs              — View live application logs."
    echo -e "  ${CYAN}5)${NC} Rollback          — Undo the last deployment."
    echo -e "  ${CYAN}6)${NC} Teardown          — Delete everything from GCP."
    echo -e "  ${CYAN}q)${NC} Quit"
    echo ""
    ask "Enter your choice"
    read -r choice
    echo ""

    case "$choice" in
        1) run_full_setup ;;
        2) run_deploy ;;
        3) run_status ;;
        4) run_logs ;;
        5) run_rollback ;;
        6) run_teardown ;;
        q|Q) echo "Bye!"; exit 0 ;;
        *) warn "Invalid choice."; show_menu ;;
    esac
}

# ── 1. Full setup ─────────────────────────────────────────────────────────────
run_full_setup() {
    banner "Full Setup"
    echo "  This will create the following resources in Google Cloud:"
    echo "  • Artifact Registry (Docker image storage)"
    echo "  • GKE Cluster (Kubernetes)"
    echo "  • Cloud SQL PostgreSQL instance"
    echo "  • Service accounts and permissions"
    echo ""

    confirm "Ready to proceed?" || { show_menu; return; }

    setup_gcloud
    setup_artifact_registry
    setup_gke
    setup_cloud_sql
    setup_secrets
    setup_helm_deploy

    banner "Setup Complete!"
    ok "Your application is deploying."
    echo ""
    echo "  Run option 3 (Status) to check when it's ready."
    echo "  Run option 4 (Logs) to watch the startup logs."
    echo ""
}

setup_gcloud() {
    step "Configuring Google Cloud project"
    gcloud config set project "$GCP_PROJECT_ID" --quiet
    gcloud services enable \
        sqladmin.googleapis.com \
        container.googleapis.com \
        artifactregistry.googleapis.com \
        --quiet
    ok "APIs enabled"
}

setup_artifact_registry() {
    step "Creating Artifact Registry for Docker images"
    if gcloud artifacts repositories describe "$APP_NAME" \
        --location="$GCP_REGION" --quiet &>/dev/null; then
        ok "Artifact Registry already exists — skipping"
    else
        gcloud artifacts repositories create "$APP_NAME" \
            --repository-format=docker \
            --location="$GCP_REGION" \
            --description="${APP_NAME} Docker images" \
            --quiet
        ok "Artifact Registry created"
    fi
}

setup_gke() {
    step "Creating Kubernetes cluster (this takes 3-5 minutes)"
    if gcloud container clusters describe "$K8S_CLUSTER_NAME" \
        --region="$GCP_REGION" --quiet &>/dev/null; then
        ok "Cluster already exists — skipping"
    else
        gcloud container clusters create "$K8S_CLUSTER_NAME" \
            --region="$GCP_REGION" \
            --num-nodes=1 \
            --machine-type="${K8S_NODE_TYPE:-e2-small}" \
            --enable-ip-alias \
            --quiet
        ok "Cluster created"
    fi

    step "Connecting to cluster"
    gcloud container clusters get-credentials "$K8S_CLUSTER_NAME" \
        --region="$GCP_REGION" --quiet
    kubectl create namespace "$K8S_NAMESPACE" --dry-run=client -o yaml | kubectl apply -f - --quiet 2>/dev/null || true
    ok "Connected to cluster — namespace: $K8S_NAMESPACE"
}

setup_cloud_sql() {
    step "Creating Cloud SQL PostgreSQL instance (this takes 5-8 minutes)"
    if gcloud sql instances describe "$DB_INSTANCE_NAME" --quiet &>/dev/null; then
        ok "Database instance already exists — skipping"
    else
        gcloud sql instances create "$DB_INSTANCE_NAME" \
            --database-version=POSTGRES_16 \
            --tier="${DB_TIER:-db-f1-micro}" \
            --region="$GCP_REGION" \
            --storage-type=SSD \
            --storage-size=10GB \
            --quiet
        ok "Cloud SQL instance created"
    fi

    step "Creating database and user"
    gcloud sql databases create "$DB_NAME" \
        --instance="$DB_INSTANCE_NAME" --quiet 2>/dev/null || ok "Database already exists"
    gcloud sql users create "$DB_USER" \
        --instance="$DB_INSTANCE_NAME" \
        --password="$DB_PASS" --quiet 2>/dev/null || ok "User already exists"
    ok "Database and user ready"

    step "Setting up Cloud SQL service account"
    local sa="${APP_NAME}-sa@${GCP_PROJECT_ID}.iam.gserviceaccount.com"
    gcloud iam service-accounts create "${APP_NAME}-sa" \
        --display-name="${APP_NAME} service account" --quiet 2>/dev/null || true
    gcloud projects add-iam-policy-binding "$GCP_PROJECT_ID" \
        --member="serviceAccount:${sa}" \
        --role="roles/cloudsql.client" --quiet &>/dev/null
    ok "Service account configured"
}

setup_secrets() {
    step "Storing application secrets in Kubernetes"
    local connection_name
    connection_name=$(gcloud sql instances describe "$DB_INSTANCE_NAME" \
        --format="value(connectionName)")

    # DB credentials — app connects via Cloud SQL proxy on 127.0.0.1
    kubectl create secret generic "${APP_NAME}-db" \
        --from-literal=DB_URL="jdbc:postgresql://127.0.0.1:5432/${DB_NAME}" \
        --from-literal=DB_USER="$DB_USER" \
        --from-literal=DB_PASS="$DB_PASS" \
        --namespace="$K8S_NAMESPACE" \
        --dry-run=client -o yaml | kubectl apply -f - --quiet
    ok "Database credentials stored"

    # App environment variables from config
    if [[ -n "${APP_ENV_VARS:-}" ]]; then
        local secret_args=()
        while IFS= read -r line; do
            [[ -z "$line" || "$line" =~ ^# ]] && continue
            secret_args+=("--from-literal=${line}")
        done <<< "$APP_ENV_VARS"
        if [[ ${#secret_args[@]} -gt 0 ]]; then
            kubectl create secret generic "${APP_NAME}-env" \
                "${secret_args[@]}" \
                --namespace="$K8S_NAMESPACE" \
                --dry-run=client -o yaml | kubectl apply -f - --quiet
            ok "App environment secrets stored"
        fi
    fi

    # Save connection name for deploy step
    echo "$connection_name" > "$SCRIPT_DIR/.connection_name"
}

setup_helm_deploy() {
    step "Building and deploying application"
    run_deploy_internal
}

# ── 2. Deploy ─────────────────────────────────────────────────────────────────
run_deploy() {
    banner "Deploy"
    run_deploy_internal
}

run_deploy_internal() {
    local image_repo="${GCP_REGION}-docker.pkg.dev/${GCP_PROJECT_ID}/${APP_NAME}/${APP_NAME}"
    local image_tag
    image_tag=$(date +%Y%m%d%H%M%S)
    local connection_name=""
    [[ -f "$SCRIPT_DIR/.connection_name" ]] && connection_name=$(cat "$SCRIPT_DIR/.connection_name")

    step "Building Docker image"
    local dockerfile_path="${DOCKER_FILE_PATH:-../Dockerfile}"
    local context_dir
    context_dir=$(cd "$SCRIPT_DIR" && cd "$(dirname "$dockerfile_path")" && pwd)
    docker build -t "${image_repo}:${image_tag}" -t "${image_repo}:latest" \
        -f "${SCRIPT_DIR}/${dockerfile_path}" "$context_dir"
    ok "Image built: ${image_repo}:${image_tag}"

    step "Pushing image to Artifact Registry"
    gcloud auth configure-docker "${GCP_REGION}-docker.pkg.dev" --quiet
    docker push "${image_repo}:${image_tag}" --quiet
    docker push "${image_repo}:latest" --quiet
    ok "Image pushed"

    step "Connecting to cluster"
    gcloud container clusters get-credentials "$K8S_CLUSTER_NAME" \
        --region="$GCP_REGION" --quiet

    step "Deploying with Helm"
    # Generate a minimal Helm release on the fly — no chart files needed
    helm upgrade --install "$APP_NAME" "$SCRIPT_DIR/chart" \
        --namespace "$K8S_NAMESPACE" \
        --create-namespace \
        --set image.repository="$image_repo" \
        --set image.tag="$image_tag" \
        --set appName="$APP_NAME" \
        --set cloudSql.connectionName="$connection_name" \
        --set dbSecretName="${APP_NAME}-db" \
        --set envSecretName="${APP_NAME}-env" \
        --wait --timeout 5m
    ok "Deployment complete — tag: $image_tag"

    echo ""
    echo -e "  ${BOLD}Run option 3 to check status.${NC}"
}

# ── 3. Status ─────────────────────────────────────────────────────────────────
run_status() {
    banner "Status — ${APP_NAME}"
    gcloud container clusters get-credentials "$K8S_CLUSTER_NAME" \
        --region="$GCP_REGION" --quiet 2>/dev/null

    echo -e "${BOLD}Pods:${NC}"
    kubectl get pods -n "$K8S_NAMESPACE" 2>/dev/null || warn "No pods found"

    echo -e "\n${BOLD}Services:${NC}"
    kubectl get svc -n "$K8S_NAMESPACE" 2>/dev/null || true

    echo -e "\n${BOLD}Recent events:${NC}"
    kubectl get events -n "$K8S_NAMESPACE" \
        --sort-by='.lastTimestamp' 2>/dev/null | tail -5 || true

    echo ""
    ask "Press Enter to return to menu"
    read -r
    show_menu
}

# ── 4. Logs ───────────────────────────────────────────────────────────────────
run_logs() {
    banner "Logs — ${APP_NAME}"
    gcloud container clusters get-credentials "$K8S_CLUSTER_NAME" \
        --region="$GCP_REGION" --quiet 2>/dev/null
    echo "  Showing live logs (Ctrl+C to stop)..."
    echo ""
    kubectl logs -n "$K8S_NAMESPACE" \
        -l "app=$APP_NAME" \
        -c app \
        --follow \
        --tail=50 2>/dev/null || warn "No running pods found yet."
    show_menu
}

# ── 5. Rollback ───────────────────────────────────────────────────────────────
run_rollback() {
    banner "Rollback — ${APP_NAME}"
    gcloud container clusters get-credentials "$K8S_CLUSTER_NAME" \
        --region="$GCP_REGION" --quiet 2>/dev/null

    echo -e "${BOLD}Deployment history:${NC}"
    helm history "$APP_NAME" -n "$K8S_NAMESPACE" 2>/dev/null || \
        { warn "No deployment history found."; show_menu; return; }

    echo ""
    confirm "Roll back to the previous version?" || { show_menu; return; }
    helm rollback "$APP_NAME" -n "$K8S_NAMESPACE" --wait
    ok "Rolled back successfully"
    echo ""
    ask "Press Enter to return to menu"
    read -r
    show_menu
}

# ── 6. Teardown ───────────────────────────────────────────────────────────────
run_teardown() {
    banner "Teardown"
    warn "This will permanently delete all GCP resources for ${APP_NAME}."
    echo "  • GKE cluster: $K8S_CLUSTER_NAME"
    echo "  • Cloud SQL:   $DB_INSTANCE_NAME"
    echo "  • Registry:    $APP_NAME"
    echo ""
    warn "All data will be lost. This cannot be undone."
    echo ""
    confirm "Type 'yes' to confirm" || { show_menu; return; }

    step "Removing Helm release"
    helm uninstall "$APP_NAME" -n "$K8S_NAMESPACE" --wait 2>/dev/null || true
    ok "Helm release removed"

    step "Deleting GKE cluster"
    gcloud container clusters delete "$K8S_CLUSTER_NAME" \
        --region="$GCP_REGION" --quiet && ok "Cluster deleted" || true

    step "Deleting Cloud SQL instance"
    gcloud sql instances delete "$DB_INSTANCE_NAME" --quiet && ok "Database deleted" || true

    step "Deleting Artifact Registry"
    gcloud artifacts repositories delete "$APP_NAME" \
        --location="$GCP_REGION" --quiet && ok "Registry deleted" || true

    ok "Teardown complete"
    exit 0
}

# ── Run ───────────────────────────────────────────────────────────────────────
check_required
check_deps
show_menu
