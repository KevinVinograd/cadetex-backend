# Cadetex Infrastructure - AWS CDK ⭐

Infraestructura como código con **AWS Cloud Development Kit** (CDK) - La opción más moderna y recomendada para AWS.

## ¿Por qué AWS CDK?

✅ **Oficial de AWS** - Soporte prioritario  
✅ **TypeScript nativo** - Mismo lenguaje que tu frontend  
✅ **Type safety** - Autocomplete y detección de errores  
✅ **Constructs de alto nivel** - Menos código, más productividad  
✅ **Testing con Jest** - Pruebas unitarias reales  
✅ **Documentación excelente** - Mejor soporte AWS  

## Requisitos

```bash
# Instalar CDK CLI
npm install -g aws-cdk

# Verificar
cdk --version
```

## Setup Inicial

```bash
cd infrastructure-cdk

# Instalar dependencias
npm install

# Build
npm run build

# Bootstrap CDK (solo la primera vez)
cdk bootstrap
```

## Configuración

### 1. Configurar AWS CLI

```bash
aws configure
```

### 2. Configurar Stack

```bash
# Modificar variables en bin/infra.ts o usar contexto
cdk deploy --context environment=prod --context projectName=cadetex
```

## Deployment

### Preview (qué se va a crear)

```bash
cdk diff
```

### Deploy

```bash
cdk deploy
```

### Destroy (eliminar todo)

```bash
cdk destroy
```

## Comandos Útiles

```bash
# Ver todos los comandos disponibles
cdk --help

# Sintetizar CloudFormation (sin deployar)
cdk synth

# Listar todos los stacks
cdk list

# Ver diferencias
cdk diff
```

## Estructura del Código

```typescript
// lib/cadetex-stack.ts
import * as ec2 from 'aws-cdk-lib/aws-ec2';

const vpc = new ec2.Vpc(this, 'Vpc', {
  maxAzs: 2,
});
```

Todo en TypeScript, con autocomplete completo.

## Outputs

Después del deploy:

```bash
aws cloudformation describe-stacks \
  --stack-name cadetex-stack-prod \
  --query 'Stacks[0].Outputs'
```

Verás:
- `BackendUrl`: http://<ip>:8080
- `FrontendUrl`: https://<cloudfront-domain>
- `RdsEndpoint`: <rds-endpoint>
- `Ec2PublicIp`: <ip-address>
- `S3BucketName`: <bucket-name>

## Ventajas sobre Terraform

### 1. Type Safety

```typescript
// ❌ Terraform: Error en runtime
resource "aws_instance" "backend" {
  instance_type = "invalid"  // No hay error hasta deploy
}

// ✅ CDK: Error en compile time
const instance = new ec2.Instance(this, 'backend', {
  instanceType: 'invalid',  // TypeScript detecta el error
});
```

### 2. Constructs de Alto Nivel

```typescript
// CDK: Mucho más simple
const database = new rds.DatabaseInstance(this, 'Database', {
  engine: rds.DatabaseInstanceEngine.postgres(),
  instanceType: ec2.InstanceType.of(ec2.InstanceClass.T3, ec2.InstanceSize.MICRO),
});

// Terraform: Más verboso
// resource "aws_db_instance" "database" {
//   engine = "postgres"
//   instance_class = "db.t3.micro"
//   ...
// }
```

### 3. Testing

```typescript
import { expect, haveResource } from 'aws-cdk-testing';
import { CadetexStack } from './cadetex-stack';

describe('CadetexStack', () => {
  test('creates VPC', () => {
    const app = new cdk.App();
    const stack = new CadetexStack(app, 'TestStack', { /*...*/ });
    
    expect(stack).to(haveResource('AWS::EC2::VPC'));
  });
});
```

## Integración con CI/CD

```yaml
# .github/workflows/infrastructure-cd.yml
- name: Deploy Infrastructure
  run: |
    cd infrastructure-cdk
    npm install
    npm run build
    cdk deploy --require-approval never
```

## Comparación con Otras Herramientas

| Feature | AWS CDK | Pulumi | Terraform |
|---------|---------|--------|-----------|
| Type Safety | ✅ | ✅ | ❌ |
| Oficial AWS | ✅ | ❌ | ❌ |
| Constructs | ✅ L3 | ✅ | ❌ |
| Multi-cloud | AWS | ✅ | ✅ |
| Curva aprendizaje | Baja | Media | Alta |

## Recursos AWS Creados

- ✅ VPC con subnets públicas y privadas
- ✅ Security Groups (EC2, RDS)
- ✅ EC2 t3.micro (Amazon Linux 2023)
- ✅ Elastic IP
- ✅ RDS PostgreSQL db.t3.micro
- ✅ S3 bucket con website hosting
- ✅ CloudFront distribution

## Costos

**AWS Free Tier** (primer año): $0/mes  
**Después del Free Tier**: ~$25-35/mes

## Tips

### Desarrollo

```bash
# Watch mode (recompila en cambios)
npm run watch

# Linting
npm run lint
```

### Troubleshooting

```bash
# Ver CloudFormation generado
cdk synth

# Ver logs de CloudFormation
aws cloudformation describe-stack-events \
  --stack-name cadetex-stack-prod \
  --max-items 10
```

## Recursos Adicionales

- [AWS CDK Docs](https://docs.aws.amazon.com/cdk/)
- [CDK Patterns](https://github.com/cdk-patterns/serverless)
- [AWS Construct Hub](https://constructs.dev/)

## Siguiente Paso

### Backend - Deploy manual (EC2)
```bash
# En EC2 (SSH)
sudo mkdir -p /opt/cadetex-backend && sudo chown ec2-user:ec2-user /opt/cadetex-backend

# En tu PC
scp -i ~/.ssh/cadetex-backend-key cadetex-backend/build/libs/cadetex-backend-v2-all.jar ec2-user@<EC2_IP>:/opt/cadetex-backend/

# En EC2 (SSH) - application.conf
sudo tee /opt/cadetex-backend/application.conf > /dev/null << 'EOF'
ktor {
  development = false
  deployment { port = 8080 }
  application { modules = [ com.cadetex.ApplicationKt.module ] }
}

database {
  host = "<RDS_ENDPOINT>"
  port = 5432
  name = "cadetex"
  user = "postgres"
  password = "<PASSWORD>"
  maxPoolSize = 10
}
EOF

# Service systemd
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

sudo systemctl daemon-reload && sudo systemctl enable cadetex-backend && sudo systemctl restart cadetex-backend

# Logs
sudo journalctl -u cadetex-backend -n 100 --no-pager
```

Seed de datos (opcional):
```bash
psql -h <RDS_ENDPOINT> -U postgres -d cadetex -f cadetex-backend/database/insert-demo-data.sql
```

### Frontend - Build y deploy (S3/CloudFront)
```bash
cd cadetex-frontend
export VITE_API_BASE_URL="http://<EC2_IP>:8080"
npm run build
aws s3 sync dist/ s3://<S3_BUCKET>/ --delete --region us-east-1
aws cloudfront create-invalidation --distribution-id <DIST_ID> --paths "/*" --region us-east-1
```

