#!/bin/bash
# Script de deployment para backend en EC2

set -e

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}🚀 Deploy Backend a EC2${NC}"

# Variables
EC2_HOST="${EC2_HOST:-}"
EC2_USER="${EC2_USER:-ec2-user}"
SSH_KEY="${SSH_KEY:-~/.ssh/cadetex-backend-key}"
REMOTE_DIR="/opt/cadetex-backend"

# Validar variables
if [ -z "$EC2_HOST" ]; then
  echo -e "${RED}❌ Error: EC2_HOST no configurado${NC}"
  echo "Ejecuta: export EC2_HOST=tu_ip_o_dominio"
  exit 1
fi

if [ ! -f "$SSH_KEY" ]; then
  echo -e "${RED}❌ Error: SSH key no encontrada en $SSH_KEY${NC}"
  exit 1
fi

# Build
echo -e "${YELLOW}📦 Compilando...${NC}"
./gradlew clean shadowJar

JAR_FILE="build/libs/cadetex-backend-v2-all.jar"

if [ ! -f "$JAR_FILE" ]; then
  echo -e "${RED}❌ Error: JAR no encontrado${NC}"
  exit 1
fi

echo -e "${GREEN}✅ Build completado${NC}"

# Deploy
echo -e "${YELLOW}📤 Deployando...${NC}"
scp -i "$SSH_KEY" "$JAR_FILE" "${EC2_USER}@${EC2_HOST}:${REMOTE_DIR}/"

echo -e "${YELLOW}🔄 Reiniciando servicio...${NC}"
ssh -i "$SSH_KEY" "${EC2_USER}@${EC2_HOST}" << 'ENDSSH'
  sudo systemctl restart cadetex-backend
  sleep 3
  sudo systemctl status cadetex-backend --no-pager
ENDSSH

echo -e "${GREEN}✅ Deploy completado!${NC}"
echo -e "${GREEN}Backend disponible en: http://${EC2_HOST}:8080${NC}"

