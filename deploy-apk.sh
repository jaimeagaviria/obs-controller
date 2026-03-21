#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# deploy-apk.sh  —  Construye el APK release y lo publica en el servidor
# Uso:  ./deploy-apk.sh
# ─────────────────────────────────────────────────────────────────────────────
set -e

SERVER="100.51.43.233"
SERVER_USER="ubuntu"
SERVER_KEY="OBS-Controller.pem"
APK_NAME="obs-remote-camera.apk"

# El volumen Docker "downloads" se monta en el contenedor frontend en /downloads.
# Para subir directamente al volumen usamos el path canónico de Docker en el host.
REMOTE_DIR="/var/lib/docker/volumes/obs-controller_downloads/_data"

APK_SRC="obs-remote-camera/app/build/outputs/apk/release/app-release.apk"

# ── 1. Extraer versión del build.gradle.kts ───────────────────────────────────
VERSION=$(grep 'versionName' obs-remote-camera/app/build.gradle.kts \
          | head -1 | sed 's/.*"\(.*\)".*/\1/')
BUILD_DATE=$(date +"%Y-%m-%d")

echo "┌─────────────────────────────────────────────"
echo "│  OBS Remote Camera — Deploy APK"
echo "│  Versión : $VERSION"
echo "│  Fecha   : $BUILD_DATE"
echo "└─────────────────────────────────────────────"

# ── 2. Construir APK release ──────────────────────────────────────────────────
echo ""
echo "▶ Compilando APK release..."
cd obs-remote-camera
bash ./gradlew assembleRelease --quiet
cd ..
echo "  ✓ APK compilado"

# ── 3. Calcular tamaño ────────────────────────────────────────────────────────
APK_SIZE=$(du -sh "$APK_SRC" | cut -f1)

# ── 4. Crear version.json ─────────────────────────────────────────────────────
TMP_JSON=$(mktemp)
cat > "$TMP_JSON" << EOF
{
  "version": "$VERSION",
  "date": "$BUILD_DATE",
  "size": "${APK_SIZE}B",
  "filename": "$APK_NAME"
}
EOF
echo "  ✓ version.json generado"

# ── 5. Subir al servidor ──────────────────────────────────────────────────────
echo ""
echo "▶ Subiendo al servidor $SERVER..."

SSH_OPTS="-i $SERVER_KEY -o StrictHostKeyChecking=no"

# Crear directorio si no existe
ssh $SSH_OPTS "$SERVER_USER@$SERVER" "sudo mkdir -p $REMOTE_DIR && sudo chmod 755 $REMOTE_DIR"

# Subir APK
scp $SSH_OPTS "$APK_SRC" "$SERVER_USER@$SERVER:/tmp/$APK_NAME"
ssh $SSH_OPTS "$SERVER_USER@$SERVER" "sudo mv /tmp/$APK_NAME $REMOTE_DIR/$APK_NAME && sudo chmod 644 $REMOTE_DIR/$APK_NAME"
echo "  ✓ APK subido"

# Subir version.json
scp $SSH_OPTS "$TMP_JSON" "$SERVER_USER@$SERVER:/tmp/version.json"
ssh $SSH_OPTS "$SERVER_USER@$SERVER" "sudo mv /tmp/version.json $REMOTE_DIR/version.json && sudo chmod 644 $REMOTE_DIR/version.json"
echo "  ✓ version.json subido"

rm -f "$TMP_JSON"

# ── 6. Resumen ────────────────────────────────────────────────────────────────
echo ""
echo "✅ Publicado correctamente"
echo ""
echo "   Página de descarga : http://$SERVER:5180/download"
echo "   Descarga directa   : http://$SERVER:5180/downloads/$APK_NAME"
echo ""
echo "   Comparte este enlace por WhatsApp:"
echo "   http://$SERVER:5180/download"
