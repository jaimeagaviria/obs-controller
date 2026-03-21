#!/bin/bash
# =============================================================================
# deploy.sh — Despliega servicios de obs-controller en el servidor remoto
# =============================================================================
# Uso:
#   ./deploy.sh                          # despliega todos los servicios
#   ./deploy.sh backend                  # solo backend
#   ./deploy.sh frontend                 # solo frontend
#   ./deploy.sh backend frontend         # backend y frontend
#
# Servicios disponibles: backend, frontend, mediamtx, tailscale
# =============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PEM_KEY="$SCRIPT_DIR/OBS-Controller.pem"
CONTEXT_NAME="obs-controller-server"

# Cargar .env
[ -f "$SCRIPT_DIR/.env" ] && export $(grep -v '^#' "$SCRIPT_DIR/.env" | xargs)

SERVICES="$@"

# ---------------------------------------------------------------------------
# Validaciones
# ---------------------------------------------------------------------------
if [ -z "$DEPLOY_HOST" ]; then
  echo "ERROR: DEPLOY_HOST no está definido en .env"
  exit 1
fi

if [ ! -f "$PEM_KEY" ]; then
  echo "ERROR: No se encontró la clave: $PEM_KEY"
  exit 1
fi

if [ -n "$SERVICES" ]; then
  echo "🚀 Desplegando [$SERVICES] → $DEPLOY_HOST"
else
  echo "🚀 Desplegando todos los servicios → $DEPLOY_HOST"
fi

# ---------------------------------------------------------------------------
# SSH agent
# ---------------------------------------------------------------------------
chmod 600 "$PEM_KEY"
eval "$(ssh-agent -s)" > /dev/null
trap "ssh-agent -k > /dev/null" EXIT
ssh-add "$PEM_KEY"

SERVER_IP="${DEPLOY_HOST##*@}"
if ! ssh-keygen -F "$SERVER_IP" &>/dev/null; then
  echo "📋 Registrando host key de $SERVER_IP..."
  ssh-keyscan -H "$SERVER_IP" >> ~/.ssh/known_hosts 2>/dev/null
fi

# ---------------------------------------------------------------------------
# Docker context
# ---------------------------------------------------------------------------
if ! docker context inspect "$CONTEXT_NAME" &>/dev/null; then
  echo "📡 Creando Docker context '$CONTEXT_NAME'..."
  docker context create "$CONTEXT_NAME" --docker "host=ssh://$DEPLOY_HOST"
fi

# ---------------------------------------------------------------------------
# Subir mediamtx.yml al servidor si se despliega mediamtx o todo
# ---------------------------------------------------------------------------
DEPLOY_MEDIAMTX_CFG=false
if [ -z "$SERVICES" ] || echo "$SERVICES" | grep -qw "mediamtx"; then
  DEPLOY_MEDIAMTX_CFG=true
fi

if [ "$DEPLOY_MEDIAMTX_CFG" = "true" ] && [ -f "$SCRIPT_DIR/mediamtx/mediamtx.yml" ]; then
  REMOTE_MEDIAMTX_DIR="${MEDIAMTX_CONFIG_PATH%/*}"
  echo "📤 Subiendo mediamtx.yml → $DEPLOY_HOST:$MEDIAMTX_CONFIG_PATH"
  ssh -i "$PEM_KEY" -o StrictHostKeyChecking=no "${DEPLOY_HOST}" "mkdir -p $REMOTE_MEDIAMTX_DIR"
  scp -i "$PEM_KEY" -o StrictHostKeyChecking=no \
    "$SCRIPT_DIR/mediamtx/mediamtx.yml" \
    "${DEPLOY_HOST}:${MEDIAMTX_CONFIG_PATH}"
  # Forzar restart para que mediamtx cargue el nuevo config
  # (docker compose up no reinicia si la imagen no cambia)
  echo "🔄 Reiniciando mediamtx para cargar nuevo config..."
  ssh -i "$PEM_KEY" -o StrictHostKeyChecking=no "${DEPLOY_HOST}" "docker restart mediamtx 2>/dev/null || true"
fi

# ---------------------------------------------------------------------------
# Desplegar
# ---------------------------------------------------------------------------
echo ""
echo "🔨 Construyendo y desplegando..."
docker --context "$CONTEXT_NAME" compose up --build -d --no-deps $SERVICES

echo ""
echo "✅ Listo."
if [ -z "$SERVICES" ]; then
  echo "   Frontend  → http://$SERVER_IP:5180"
  echo "   Backend   → http://$SERVER_IP:3010"
fi
echo ""
echo "Logs:  docker --context $CONTEXT_NAME compose logs -f $SERVICES"
