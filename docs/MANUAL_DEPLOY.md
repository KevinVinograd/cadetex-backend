# 📤 Deploy Manual - Cadetex Backend

## 1) Conectarse por SSH (Windows)

Si ves "REMOTE HOST IDENTIFICATION HAS CHANGED", ejecuta antes:
```powershell
ssh-keygen -R 52.3.208.72
ssh-keygen -R ec2-52-3-208-72.compute-1.amazonaws.com
```

Luego conecta:
```powershell
ssh -i $env:USERPROFILE\.ssh\cadetex-backend-key ec2-user@52.3.208.72
```

## 2) Preparar directorio y Java (en EC2)

```bash
sudo mkdir -p /opt/cadetex-backend
sudo chown ec2-user:ec2-user /opt/cadetex-backend

# Instalar Java (Amazon Linux 2023)
sudo dnf update -y
sudo dnf install -y java-17-amazon-corretto || sudo dnf install -y java-21-amazon-corretto
java -version
```

## 3) Subir el JAR (desde tu PC)

```powershell
scp -i $env:USERPROFILE\.ssh\cadetex-backend-key `
  C:\Users\kevin\vexa\cadetex-backend\build\libs\cadetex-backend-v2-all.jar `
  ec2-user@52.3.208.72:/opt/cadetex-backend/
```

Si falla por permisos, súbelo al HOME y muévelo en EC2:
```powershell
scp -i $env:USERPROFILE\.ssh\cadetex-backend-key `
  C:\Users\kevin\vexa\cadetex-backend\build\libs\cadetex-backend-v2-all.jar `
  ec2-user@52.3.208.72:~
```
```bash
sudo mv ~/cadetex-backend-v2-all.jar /opt/cadetex-backend/
sudo chown ec2-user:ec2-user /opt/cadetex-backend/cadetex-backend-v2-all.jar
```

## 4) Crear application.conf (en EC2)

```bash
sudo tee /opt/cadetex-backend/application.conf > /dev/null << 'EOF'
ktor {
  development = false
  deployment { port = 8080 }
  application { modules = [ com.cadetex.ApplicationKt.module ] }
}

database {
  host = "cadetex-stack-prod-databaseb269d8bb-kmdxsh7s3lnk.cijeswmk2d8w.us-east-1.rds.amazonaws.com"
  port = 5432
  name = "cadetex"
  user = "postgres"
  password = "c14zr9ZbdNNGnH26ehdt-Vw2NI1qJ2"
  maxPoolSize = 10
}
EOF
```

## 5) Crear service systemd (en EC2)

```bash
sudo tee /etc/systemd/system/cadetex-backend.service > /dev/null << 'EOF'
[Unit]
Description=Cadetex Backend
After=network.target

[Service]
Type=simple
User=ec2-user
WorkingDirectory=/opt/cadetex-backend
ExecStart=/usr/bin/java -Dconfig.file=/opt/cadetex-backend/application.conf -jar /opt/cadetex-backend/cadetex-backend-v2-all.jar
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
sudo systemctl enable cadetex-backend
sudo systemctl restart cadetex-backend
sudo systemctl status cadetex-backend --no-pager || sudo journalctl -u cadetex-backend -n 100 --no-pager
```

## 6) Ver logs (en EC2)

```bash
sudo journalctl -u cadetex-backend -n 100 --no-pager
sudo journalctl -u cadetex-backend -f
```

## 7) Probar desde tu PC

```powershell
curl http://52.3.208.72:8080/swagger
```

---

## Info útil

- EC2 IP: 52.3.208.72
- JAR: `cadetex-backend/build/libs/cadetex-backend-v2-all.jar`
- DB: RDS Postgres (endpoint en outputs)

