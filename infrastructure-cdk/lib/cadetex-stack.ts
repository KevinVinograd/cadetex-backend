import * as cdk from 'aws-cdk-lib';
import { Construct } from 'constructs';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as s3 from 'aws-cdk-lib/aws-s3';
import * as cloudfront from 'aws-cdk-lib/aws-cloudfront';
import * as origins from 'aws-cdk-lib/aws-cloudfront-origins';
import * as rds from 'aws-cdk-lib/aws-rds';

export interface CadetexStackProps extends cdk.StackProps {
  environment: string;
  projectName: string;
}

export class CadetexStack extends cdk.Stack {
  public readonly backendUrl: string;
  public readonly frontendUrl: string;
  public readonly rdsEndpoint: string;

  constructor(scope: Construct, id: string, props: CadetexStackProps) {
    super(scope, id, props);

    const { environment, projectName } = props;

    // VPC
    const vpc = new ec2.Vpc(this, 'Vpc', {
      maxAzs: 2,
      natGateways: 0, // Para ahorrar costos (Free Tier)
      subnetConfiguration: [
        {
          cidrMask: 24,
          name: 'PublicSubnet',
          subnetType: ec2.SubnetType.PUBLIC,
        },
        {
          cidrMask: 24,
          name: 'PrivateSubnet',
          subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS,
        },
      ],
      ipAddresses: ec2.IpAddresses.cidr('10.0.0.0/16'),
    });

    // Security Group para EC2
    const ec2SecurityGroup = new ec2.SecurityGroup(this, 'EC2SecurityGroup', {
      vpc,
      description: 'Security group for EC2 backend server',
      allowAllOutbound: true,
    });

    ec2SecurityGroup.addIngressRule(
      ec2.Peer.anyIpv4(),
      ec2.Port.tcp(22),
      'Allow SSH'
    );

    ec2SecurityGroup.addIngressRule(
      ec2.Peer.anyIpv4(),
      ec2.Port.tcp(8080),
      'Allow Backend API'
    );

    // Security Group para RDS
    const rdsSecurityGroup = new ec2.SecurityGroup(this, 'RDSSecurityGroup', {
      vpc,
      description: 'Security group for RDS PostgreSQL',
      allowAllOutbound: true,
    });

    // Permitir PostgreSQL desde EC2
    rdsSecurityGroup.addIngressRule(
      ec2SecurityGroup,
      ec2.Port.tcp(5432),
      'Allow PostgreSQL from EC2'
    );

    // ✅ PERMITIR DESDE INTERNET (Para testing - remover en producción)
    rdsSecurityGroup.addIngressRule(
      ec2.Peer.anyIpv4(),
      ec2.Port.tcp(5432),
      'Allow PostgreSQL from Internet (TESTING ONLY)'
    );

    // EC2 Instance
    const backendInstance = new ec2.Instance(this, 'Backend', {
      vpc,
      vpcSubnets: { subnetType: ec2.SubnetType.PUBLIC },
      instanceType: ec2.InstanceType.of(ec2.InstanceClass.T3, ec2.InstanceSize.MICRO),
      machineImage: ec2.MachineImage.latestAmazonLinux2023(),
      securityGroup: ec2SecurityGroup,
      keyName: 'cadetex-backend-key',
      userData: ec2.UserData.custom(`
        #!/bin/bash
        yum update -y
        amazon-linux-extras install java-openjdk17 -y
        mkdir -p /opt/cadetex-backend
        chown ec2-user:ec2-user /opt/cadetex-backend

        cat > /etc/systemd/system/cadetex-backend.service <<'SERVICEEOF'
[Unit]
Description=Cadetex Backend Service
After=network.target

[Service]
Type=simple
User=ec2-user
WorkingDirectory=/opt/cadetex-backend
ExecStart=/usr/bin/java -jar /opt/cadetex-backend/cadetex-backend-v2-all.jar
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
SERVICEEOF

        systemctl daemon-reload
        systemctl enable cadetex-backend
      `),
    });

    // Elastic IP
    const eip = new ec2.CfnEIP(this, 'BackendEIP', {
      instanceId: backendInstance.instanceId,
      domain: 'vpc',
    });

    // DB Subnet Group
    const dbSubnetGroup = new rds.SubnetGroup(this, 'DBSubnetGroup', {
      vpc,
      subnetGroupName: `${projectName}-db-subnet-group`,
      description: 'Subnet group for RDS database',
      vpcSubnets: {
        subnetType: ec2.SubnetType.PUBLIC, // ✅ Cambiado a PUBLIC para que RDS sea accesible
      },
    });

    // RDS PostgreSQL
    const database = new rds.DatabaseInstance(this, 'Database', {
      engine: rds.DatabaseInstanceEngine.postgres({
        version: rds.PostgresEngineVersion.VER_16,
      }),
      instanceType: ec2.InstanceType.of(ec2.InstanceClass.T3, ec2.InstanceSize.MICRO),
      vpc,
      subnetGroup: dbSubnetGroup,
      securityGroups: [rdsSecurityGroup],
      allocatedStorage: 20,
      maxAllocatedStorage: 100,
      storageType: rds.StorageType.GP3,
      databaseName: 'cadetex',
      publiclyAccessible: true, // ✅ PÚBLICA para testing (cambiar a false en producción)
      multiAz: false,
      backupRetention: cdk.Duration.days(7),
      removalPolicy: cdk.RemovalPolicy.DESTROY, // Para desarrollo
      deleteAutomatedBackups: true,
      enablePerformanceInsights: false, // Para Free Tier
    });

    // S3 Bucket para Frontend
    const frontendBucket = new s3.Bucket(this, 'FrontendBucket', {
      bucketName: `${projectName}-frontend-${environment}`,
      publicReadAccess: true,
      blockPublicAccess: new s3.BlockPublicAccess({
        blockPublicAcls: false,
        blockPublicPolicy: false,
        ignorePublicAcls: false,
        restrictPublicBuckets: false,
      }),
      websiteIndexDocument: 'index.html',
      websiteErrorDocument: 'index.html',
      removalPolicy: cdk.RemovalPolicy.DESTROY, // Para desarrollo
      autoDeleteObjects: true,
    });

    // CloudFront Distribution
    const distribution = new cloudfront.Distribution(this, 'FrontendDistribution', {
      defaultBehavior: {
        origin: new origins.S3Origin(frontendBucket),
        viewerProtocolPolicy: cloudfront.ViewerProtocolPolicy.ALLOW_ALL,
        compress: true,
        cachePolicy: cloudfront.CachePolicy.CACHING_OPTIMIZED,
      },
      defaultRootObject: 'index.html',
      errorResponses: [
        {
          httpStatus: 404,
          responseHttpStatus: 200,
          responsePagePath: '/index.html',
          ttl: cdk.Duration.minutes(0),
        },
      ],
    });

    // Outputs
    this.backendUrl = `http://${eip.ref}:8080`;
    this.frontendUrl = `https://${distribution.distributionDomainName}`;
    this.rdsEndpoint = database.instanceEndpoint.hostname;

    new cdk.CfnOutput(this, 'BackendUrl', {
      value: this.backendUrl,
      description: 'Backend API URL',
    });

    new cdk.CfnOutput(this, 'FrontendUrl', {
      value: this.frontendUrl,
      description: 'Frontend application URL',
    });

    new cdk.CfnOutput(this, 'RdsEndpoint', {
      value: this.rdsEndpoint,
      description: 'RDS PostgreSQL endpoint',
    });

    new cdk.CfnOutput(this, 'Ec2PublicIp', {
      value: eip.ref,
      description: 'EC2 public IP',
    });

    new cdk.CfnOutput(this, 'Ec2InstanceId', {
      value: backendInstance.instanceId,
      description: 'EC2 instance ID',
    });

    new cdk.CfnOutput(this, 'S3BucketName', {
      value: frontendBucket.bucketName,
      description: 'S3 bucket name for frontend',
    });
  }
}

