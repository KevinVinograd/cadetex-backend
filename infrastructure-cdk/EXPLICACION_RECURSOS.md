# Recursos AWS que se Crean con CDK

Explicación detallada de cada recurso que se crea al hacer `cdk deploy`.

## 🏗️ Arquitectura Completa

```
┌──────────────────────────────────────────────────────────┐
│                         AWS                               │
│                                                           │
│  ┌─────────────┐    HTTPS     ┌────────────────┐        │
│  │ CloudFront  │─────────────►│  S3 Bucket     │        │
│  │  (CDN)      │              │  (Frontend)    │        │
│  └─────────────┘              └────────────────┘       │
│         │                                                 │
│         │ HTTPS API Call                                     │
│         ▼                                                 │
│  ┌──────────────────────┐                               │
│  │      EC2 Instance     │                               │
│  │   (Backend Kotlin)   │                               │
│  │  IP: 54.x.x.x:8080   │                               │
│  │                      │                               │
│  │  - Java 17           │                               │
│  │  - PostgreSQL client │                               │
│  │  - Service systemd   │                               │
│  └──────────────────────┘                               │
│         │                                                 │
│         │ PostgreSQL (5432)                               │
│         ▼                                                 │
│  ┌──────────────────────┐                               │
│  │   RDS PostgreSQL     │                               │
│  │   (Database)         │                               │
│  │  - db.t3.micro       │                               │
│  │  - 20GB storage      │                               │
│  │  - Auto-scaling 100GB│                               │
│  └──────────────────────┘                               │
│                                                           │
└──────────────────────────────────────────────────────────┘
```

## 📋 Recursos Creados (Línea por Línea)

### 1. VPC (Virtual Private Cloud) - Línea 25

```typescript
const vpc = new ec2.Vpc(this, 'Vpc', {
  maxAzs: 2,
  natGateways: 0,
  subnetConfiguration: [/* ... */]
});
```

**Qué es**: Red privada aislada en AWS  
**Para qué**: Aislar tus recursos (EC2, RDS) del resto de Internet  
**Detalles**:
- CIDR: `10.0.0.0/16` (65,536 IPs)
- 2 Availability Zones (2 regiones para redundancia)
- 0 NAT Gateways (ahorra costo, Free Tier no lo cubre)

**Subnets creadas**:
- **2 Public Subnets** (`10.0.0.0/24` y `10.0.1.0/24`): Para EC2
- **2 Private Subnets** (`10.0.2.0/24` y `10.0.3.0/24`): Para RDS

---

### 2. Security Group para EC2 - Línea 44

```typescript
const ec2SecurityGroup = new ec2.SecurityGroup(this, 'EC2SecurityGroup', {
  vpc,
  description: 'Security group for EC2 backend server',
  allowAllOutbound: true,
});
```

**Qué es**: Firewall para EC2  
**Para qué**: Controlar quién puede conectarse al servidor  
**Reglas de Entrada**:
- ✅ Puerto 22 (SSH): Conexión remota para deploy
- ✅ Puerto 8080 (Backend API): Acceso a tu API

**Reglas de Salida**:
- ✅ Todo permitido (para descargas, etc.)

---

### 3. Security Group para RDS - Línea 63

```typescript
const rdsSecurityGroup = new ec2.SecurityGroup(this, 'RDSSecurityGroup', {
  vpc,
  description: 'Security group for RDS PostgreSQL',
});
```

**Qué es**: Firewall para RDS  
**Para qué**: Proteger la base de datos  
**Regla de Entrada**:
- ✅ Puerto 5432 (PostgreSQL): Solo desde el Security Group de EC2

**IMPORTANTE**: La DB solo acepta conexiones desde EC2, no desde Internet.

---

### 4. EC2 Instance (Backend) - Línea 76

```typescript
const backendInstance = new ec2.Instance(this, 'Backend', {
  vpc,
  instanceType: ec2.InstanceType.of(ec2.InstanceClass.T3, ec2.InstanceSize.MICRO),
  machineImage: ec2.MachineImage.latestAmazonLinux2023(),
  securityGroup: ec2SecurityGroup,
  userData: ec2.UserData.custom(`/* ... */`),
});
```

**Qué es**: Servidor virtual donde corre tu backend  
**Tipo**: t3.micro (Free Tier eligible, 1 vCPU, 1GB RAM)  
**OS**: Amazon Linux 2023  
**Ubicación**: Public Subnet (tiene IP pública)  

**Software Instalado** (via UserData):
- Java 17 (para correr tu app Kotlin)
- PostgreSQL client (para conectarse a RDS)
- Directorio `/opt/cadetex-backend` creado
- Servicio systemd configurado (`cadetex-backend.service`)

**El servicio**:
```bash
systemctl start cadetex-backend  # Inicia tu app
systemctl status cadetex-backend # Ver estado
systemctl logs cadetex-backend   # Ver logs
```

---

### 5. Elastic IP - Línea 112

```typescript
const eip = new ec2.CfnEIP(this, 'BackendEIP', {
  instanceId: backendInstance.instanceId,
  domain: 'vpc',
});
```

**Qué es**: IP pública fija  
**Para qué**: Tu backend siempre tendrá la misma IP (no cambia aunque reinicies el servidor)  
**Ejemplo**: `http://54.123.45.67:8080`

---

### 6. DB Subnet Group - Línea 118

```typescript
const dbSubnetGroup = new rds.SubnetGroup(this, 'DBSubnetGroup', {
  vpc,
  subnetGroupName: `${projectName}-db-subnet-group`,
  vpcSubnets: { subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS },
});
```

**Qué es**: Conjunto de subnets donde se puede crear RDS  
**Para qué**: RDS necesita estar en subnets privadas  
**Subnets**: Private Subnets 1 y 2

---

### 7. RDS PostgreSQL - Línea 128

```typescript
const database = new rds.DatabaseInstance(this, 'Database', {
  engine: rds.DatabaseInstanceEngine.postgres({
    version: rds.PostgresEngineVersion.VER_16_5,
  }),
  instanceType: ec2.InstanceType.of(ec2.InstanceClass.T3, ec2.InstanceSize.MICRO),
  allocatedStorage: 20,
  maxAllocatedStorage: 100,
});
```

**Qué es**: Base de datos PostgreSQL gestionada  
**Tipo**: db.t3.micro (Free Tier eligible)  
**Storage**: 
- Inicial: 20 GB
- Auto-scaling: Hasta 100 GB (crece automáticamente si necesitas más)
- Tipo: gp3 (SSD de alta performance)

**Ubicación**: Private Subnet (no accesible desde Internet)  
**Backups**: 7 días de retención automáticos  
**Público**: NO (solo accesible desde EC2 en la misma VPC)

---

### 8. S3 Bucket (Frontend) - Línea 149

```typescript
const frontendBucket = new s3.Bucket(this, 'FrontendBucket', {
  bucketName: `${projectName}-frontend-${environment}`,
  publicReadAccess: true,
  websiteIndexDocument: 'index.html',
  websiteErrorDocument: 'index.html',
});
```

**Qué es**: Almacenamiento de objetos (archivos estáticos)  
**Nombre**: `cadetex-frontend-prod`  
**Hosting Estático**: Habilitado (puede servir HTML, CSS, JS)  
**Público**: Sí (lectura pública para que los usuarios accedan)  

**Cómo se usa**:
```bash
aws s3 sync dist/ s3://cadetex-frontend-prod/ --delete
```

Sube los archivos compilados de React.

---

### 9. CloudFront Distribution - Línea 165

```typescript
const distribution = new cloudfront.Distribution(this, 'FrontendDistribution', {
  defaultBehavior: {
    origin: new origins.S3Origin(frontendBucket),
    viewerProtocolPolicy: cloudfront.ViewerProtocolPolicy.REDIRECT_TO_HTTPS,
    compress: true,
    cachePolicy: cloudfront.CachePolicy.CACHING_OPTIMIZED,
  },
});
```

**Qué es**: CDN (Content Delivery Network)  
**Para qué**: Acelera el frontend distribuyéndolo por el mundo  
**Características**:
- HTTPS gratuito (certificado SSL)
- Cache inteligente (archivos estáticos cacheados)
- Compresión automática
- 404 → /index.html (SPA routing)

**URL**: `https://d1234abcd.cloudfront.net`

---

## 🔄 Flujo de Datos

### Usuario visita tu app:

1. Usuario escribe: `https://d1234abcd.cloudfront.net`
2. CloudFront: Verifica cache
   - Cache hit: Retorna archivo cached
   - Cache miss: Va a S3 por el archivo
3. S3: Entrega archivos estáticos (HTML, CSS, JS)
4. React App: Carga en el navegador
5. Usuario hace Login: React hace POST a `http://54.123.45.67:8080/auth/login`
6. EC2: Procesa la request
7. EC2 conecta a RDS: `SELECT * FROM users WHERE email='...'`
8. RDS: Retorna datos
9. EC2: Genera JWT token
10. EC2: Retorna al browser
11. React: Guarda token y muestra Dashboard

---

## 💰 Costo de Recursos

| Recurso | Free Tier | Después Free Tier |
|---------|-----------|-------------------|
| VPC | Gratis | Gratis |
| Security Groups | Gratis | Gratis |
| EC2 t3.micro | 750 hrs/mes = **Gratis** | ~$7/mes |
| Elastic IP | Gratis (si en uso) | ~$3.6/mes |
| RDS db.t3.micro | 750 hrs/mes = **Gratis** | ~$15/mes |
| S3 20 GB | 5 GB gratis | ~$0.50/mes |
| CloudFront 50 GB | 50 GB transfer gratis | ~$5/mes (primeros 50GB) |
| **TOTAL FREE TIER** | **$0/mes** | ~$31/mes |

---

## 🎯 Outputs (Lo que necesitas después)

```typescript
BackendUrl: http://54.123.45.67:8080    // URL del backend
FrontendUrl: https://d1234abcd.cloudfront.net  // URL del frontend
RdsEndpoint: cadetex.cf9dt3qu.us-east-1.rds.amazonaws.com  // Para conectar DB
Ec2PublicIp: 54.123.45.67                // IP del servidor
Ec2InstanceId: i-0abc123def456           // ID para monitorear
S3BucketName: cadetex-frontend-prod      // Nombre del bucket
```

---

## 🔒 Seguridad

### ¿Qué está expuesto a Internet?

✅ **Público**:
- CloudFront (HTTPS)
- EC2 puerto 8080 (HTTP - deberías agregar HTTPS)
- EC2 puerto 22 (SSH - usa VPN o bastion mejor)

❌ **Privado**:
- RDS (solo accesible desde EC2)
- VPC interna

---

## 📝 Comandos Útiles después del Deploy

```bash
# Ver IP del servidor
aws cloudformation describe-stacks \
  --stack-name cadetex-stack-prod \
  --query 'Stacks[0].Outputs'

# Conectar al servidor
ssh -i ~/.ssh/cadetex-backend-key ec2-user@<IP_DEL_OUTPUT>

# Ver logs del backend
sudo journalctl -u cadetex-backend -f

# Conectar a la DB desde EC2
psql -h <RDS_ENDPOINT> -U postgres -d cadetex
```

---

## ✅ Resumen

**Lo que se crea:**
1. Red privada (VPC) con subnets
2. Firewalls (Security Groups)
3. Servidor backend (EC2)
4. Base de datos (RDS)
5. Storage para frontend (S3)
6. CDN para frontend (CloudFront)

**Todo en Free Tier durante el primer año de AWS!**

