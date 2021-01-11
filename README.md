# Hazelcast Discovery Plugin for AWS

This repository contains a plugin which provides the automatic Hazelcast member discovery in the Amazon Web Services Platform.

## Requirements

* Hazelcast 3.6+
* Linux Kernel 3.19+ (TCP connections may get stuck when used with older Kernel versions, resulting in undefined timeouts)
* Versions compatibility:
  * hazelcast-aws 3+ is compatible with hazelcast 4+
  * hazelcast-aws 2.4 is compatible with hazelcast 3.12.x
  * hazelcast-aws 2.3 is compatible with hazelcast 3.11.x
  * hazelcast-aws 2.2 is compatible with older hazelcast versions

## Embedded mode

To use Hazelcast embedded in your application, you need to add the plugin dependency into your Maven/Gradle file (or use [hazelcast-all](https://mvnrepository.com/artifact/com.hazelcast/hazelcast-all) which already includes the plugin). Then, when you provide `hazelcast.xml`/`hazelcast.yaml` as presented below or an equivalent Java-based configuration, your Hazelcast instances discover themselves automatically.

#### Maven

```xml
<dependency>
  <groupId>com.hazelcast</groupId>
  <artifactId>hazelcast-aws</artifactId>
  <version>${hazelcast-aws.version}</version>
</dependency>
```

#### Gradle

```groovy
compile group: "com.hazelcast", name: "hazelcast-aws", version: "${hazelcast-aws.version}"
```

## Understanding AWS Discovery Strategy

Hazelcast member starts by fetching a list of all running instances filtered by the plugin parameters (`region`, etc.). Then, each instance is checked one-by-one with its IP and each of the ports defined in the `hz-port` property. When a member is discovered under `IP:PORT`, then it joins the cluster.

Note that this plugin supports [Hazelcast Zone Aware](https://docs.hazelcast.org/docs/latest/manual/html-single/#zone_aware) feature.

The plugin is prepared to work for both **AWS EC2** and **AWS ECS/Fargate** environments. However, note that requirements and plugin properties vary depending on the environment you use.

## EC2 Configuration

The plugin works both for **Hazelcast Member Discovery** and **Hazelcast Client Discovery**.

### EC2 Hazelcast Member Discovery

Make sure that:

* you have the `hazelcast-aws.jar` (or `hazelcast-all.jar`) dependency in your classpath
* your IAM Role has `ec2:DescribeInstances` permission

Then, you can configure Hazelcast in one of the following manners.

#### XML Configuration

```xml
<hazelcast>
  <network>
    <join>
      <multicast enabled="false"/>
      <aws enabled="true">
        <tag-key>my-ec2-instance-tag-key</tag-key>
        <tag-value>my-ec2-instance-tag-value</tag-value>
      </aws>
    </join>
  </network>
</hazelcast>
```

#### YAML Configuration

```yaml
hazelcast:
  network:
    join:
      multicast:
        enabled: false
      aws:
        enabled: true
        tag-key: my-ec2-instance-tag-key
        tag-value: my-ec2-instance-tag-value
```

#### Java-based Configuration

```java
config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
config.getNetworkConfig().getJoin().getAwsConfig().setEnabled(true)
      .setProperty("tag-key", "my-ec2-instance-tag-key")
      .setProperty("tag-value", "my-ec2-instance-tag-value");
```

The following properties can be configured (all are optional).


* `access-key`, `secret-key`: access and secret keys of your AWS account; if not set, `iam-role` is used
* `iam-role`: IAM Role attached to EC2 instance used to fetch credentials (if `access-key`/`secret-key` not specified); if not set, default IAM Role attached to EC2 instance is used
* `region`: region where Hazelcast members are running; default is the current region
* `host-header`: `ec2`, `ecs`, or the URL of a EC2/ECS API endpoint; automatically detected by default
* `security-group-name`: filter to look only for EC2 instances with the given security group
* `tag-key`, `tag-value`: filter to look only for EC2 Instances with the given `tag-key`/`tag-value`
* `connection-timeout-seconds`, `read-timeout-seconds`: connection and read timeouts when making a call to AWS API; default to `10`
* `connection-retries`: number of retries while connecting to AWS API; default to `3`
* `hz-port`: a range of ports where the plugin looks for Hazelcast members; default is `5701-5708`

Note that if you don't specify any of the properties, then the plugin uses the IAM Role assigned to EC2 Instance and forms a cluster from all Hazelcast members running in same region.

### EC2 Hazelcast Client Configuration

Hazelcast Client discovery parameters are the same as mentioned above.

If Hazelcast Client is run **outside AWS**, then you need to always specify the following parameters:
- `access-key`, `secret-key` - IAM role cannot be used from outside AWS
- `region` - it cannot be detected automatically
- `use-public-ip` - must be set to `true`

Note also that your EC2 instances must have public IP assigned.

Following are example declarative and programmatic configuration snippets.

#### XML Configuration

```xml
<hazelcast-client>
  <network>
    <aws enabled="true">
      <access-key>my-access-key</access-key>
      <secret-key>my-secret-key</secret-key>
      <region>us-west-1</region>
      <tag-key>my-ec2-instance-tag-key</tag-key>
      <tag-value>my-ec2-instance-tag-value</tag-value>
      <use-public-ip>true</use-public-ip>
    </aws>
  </network>
</hazelcast-client>
```

#### YAML Configuration

```yaml
hazelcast-client:
  network:
    aws:
      enabled: true
      access-key: my-access-key
      secret-key: my-secret-key
      region: us-west-1
      tag-key: my-ec2-instance-tag-key
      tag-value: my-ec2-instance-tag-value
      use-public-ip: true
```

#### Java-based Configuration

```java
clientConfig.getNetworkConfig().getAwsConfig()
      .setEnabled(true)
      .setProperty("access-key", "my-access-key")
      .setProperty("secret-key", "my-secret-key")
      .setProperty("region", "us-west-1")
      .setProperty("tag-key", "my-ec2-instance-tag-key")
      .setProperty("tag-value", "my-ec2-instance-tag-value")
      .setProperty("use-public-ip", "true");
```

## ECS/Fargate Configuration

The plugin works both for **Hazelcast Member Discovery** (forming Hazelcast cluster) and **Hazelcast Client Discovery**.

Note: for the detailed description, check out [Hazelcast Guides: Getting Started with Embedded Hazelcast on ECS](https://guides.hazelcast.org/ecs-embedded/).

### ECS Hazelcast Member Discovery

Make sure that your IAM Task Role has the following permissions:
* `ecs:ListTasks`
* `ecs:DescribeTasks`
* `ec2:DescribeNetworkInterfaces` (needed only if task have public IPs)

Then, you can configure Hazelcast in one of the following manners. Please note that `10.0.*.*` value depends on your VPC CIDR block definition.

#### XML Configuration

```xml
<hazelcast>
  <network>
    <join>
      <multicast enabled="false"/>
      <aws enabled="true" />
    </join>
    <interfaces enabled="true">
      <interface>10.0.*.*</interface>
    </interfaces>
  </network>
</hazelcast>
```

#### YAML Configuration

```yaml
hazelcast:
  network:
    join:
      multicast:
        enabled: false
      aws:
        enabled: true
    interfaces:
      enabled: true
      interfaces:
        - 10.0.*.*
```

#### Java-based Configuration

```java
config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
config.getNetworkConfig().getJoin().getAwsConfig().setEnabled(true);
config.getNetworkConfig().getInterfaces().setEnabled(true).addInterface("10.0.*.*");
```

The following properties can be configured (all are optional).

* `access-key`, `secret-key`: access and secret keys of AWS your account; if not set, IAM Task Role is used
* `region`: region where Hazelcast members are running; default is the current region
* `cluster`: ECS cluster short name or ARN; default is the current cluster
* `family`: filter to look only for ECS tasks with the given family name; mutually exclusive with `service-name`
* `service-name`: filter to look only for ECS tasks from the given service; mutually exclusive with `family`
* `host-header`: `ecs` or the URL of a ECS API endpoint; automatically detected by default
* `connection-timeout-seconds`, `read-timeout-seconds`: connection and read timeouts when making a call to AWS API; default to `10`
* `connection-retries`: number of retries while connecting to AWS API; default to `3`
* `hz-port`: a range of ports where the plugin looks for Hazelcast members; default is `5701-5708`

Note that if you don't specify any of the properties, then the plugin discovers all Hazelcast members running in the current ECS cluster.

### ECS Hazelcast Client Configuration

Hazelcast Client discovery parameters are the same as mentioned above.

If Hazelcast Client is run **outside ECS cluster**, then you need to always specify the following parameters:
- `access-key`, `secret-key` - IAM role cannot be used from outside AWS
- `region` - it cannot be detected automatically
- `cluster` - it cannot be detected automatically
- `use-public-ip` - must be set to `true`

Note also that your ECS Tasks must have public IPs assigned and your IAM Task Role must have `ec2:DescribeNetworkInterfaces` permission.

Following are example declarative and programmatic configuration snippets.

#### XML Configuration

```xml
<hazelcast-client>
  <network>
    <aws enabled="true">
      <access-key>my-access-key</access-key>
      <secret-key>my-secret-key</secret-key>
      <region>eu-central-1</region>
      <cluster>my-cluster</cluster>
      <use-public-ip>true</use-public-ip>
    </aws>
  </network>
</hazelcast-client>
```

#### YAML Configuration

```yaml
hazelcast-client:
  network:
    aws:
      enabled: true
      access-key: my-access-key
      secret-key: my-secret-key
      region: eu-central-1
      cluster: my-cluster
      use-public-ip: true
```

#### Java-based Configuration

```java
clientConfig.getNetworkConfig().getAwsConfig()
      .setEnabled(true)
      .setProperty("access-key", "my-access-key")
      .setProperty("secret-key", "my-secret-key")
      .setProperty("region", "eu-central-1")
      .setProperty("cluster", "my-cluster")
      .setProperty("use-public-ip", "true");
```

## ECS Environment with EC2 Discovery

If you use ECS on EC2 instances (not Fargate), you may also set up your ECS Tasks to use `host` network mode and then use EC2 discovery mode instead of ECS. In that case, your Hazelcast configuration would look as follows.

```yaml
hazelcast:
  network:
    join:
      multicast:
        enabled: false
      aws:
        enabled: true
        host-header: ec2
    interfaces:
      enabled: true
      interfaces:
        - 10.0.*.*
```

All other parameters can be used exactly the same as described in the EC2-related section.

## AWS Elastic Beanstalk

The plugin works correctly on the AWS Elastic Beanstalk environment. While deploying your application into the Java Platform, please make sure your Elastic Beanstalk Environment Configuration satisfies the following requirements:
* EC2 security groups contain a group which allows the port `5701`
* IAM instance profile contains IAM role which has `ec2:DescribeInstances` permission (or your Hazelcast configuration contains `access-key` and `secret-key`)
* Deployment policy is `Rolling` (instead of the default `All at once` which may cause the whole Hazelcast members to restart at the same time and therefore lose data)

## High Availability

By default, Hazelcast distributes partition replicas (backups) randomly and equally among the cluster members, assuming
all the members in a cluster are identical. However, this is not safe in terms of availability when a partition and its
replicas are stored on the same rack, using the same network or power source, etc. To deal with that, Hazelcast offers
partition groups each is a logical grouping of members. If there is enough number of partition groups, a partition
itself and its backup(s) are not stored within the same group. This way Hazelcast guarantees that a possible failure
affecting more than one member at a time will not cause data loss. The details of partition groups can be found on the
documentation: 
[Partition Group Configuration](https://docs.hazelcast.org/docs/latest/manual/html-single/#partition-group-configuration)

In addition to two built-in grouping options `ZONE_AWARE` and `PLACEMENT_AWARE`, you can customize the formation of
these groups based on the network interfaces of members. See more details on custom groups on the documentation:
[Custom Partition Groups](https://docs.hazelcast.org/docs/latest/manual/html-single/#custom).


### Zone Aware

If `ZONE_AWARE` partition group is enabled, the backup(s) of a partition will be in a different availability zone
other than the partition's residing zone. If no other partition group is found in other zones, the backup(s) will
be stored in the same zone as the partition itself. Hazelcast AWS Discovery plugin supports ZONE_AWARE feature for
both EC2 and ECS.

#### XML Configuration

```xml
<partition-group enabled="true" group-type="ZONE_AWARE" />
```

#### YAML Configuration

```yaml
hazelcast:
  partition-group:
    enabled: true
    group-type: ZONE-AWARE
```

#### Java-based Configuration

```java
config.getPartitionGroupConfig()
    .setEnabled(true)
    .setGroupType(MemberGroupType.ZONE_AWARE);
```

***NOTE:*** *When using the `ZONE_AWARE` partition grouping, a cluster spanning multiple Availability Zones (AZ)
should have an equal number of members in each AZ. Otherwise, it will result in uneven partition distribution among
the members.*

### Placement Aware

If EC2 instances belong to an [AWS Placement Group](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/placement-groups.html),
then Hazelcast can form partition groups based on the placement groups of the instances. The backup(s) of a partition
will be in a different placement group other than the partition's placement group. The strategy ensures availability
especially for the clusters formed within a single availability zone. `PLACEMENT_AWARE` is not supported for ECS.

#### Partition Placement Group (PPG)

If EC2 instances belong to a PPG, then Hazelcast members will be grouped by the partitions of the PPG. For instance,
the Hazelcast members in the first partition of a PPG named `ppg` will belong to the partition group of `ppg@1`,
and those in the second partition will belong to `ppg@2` and so on. Furthermore, these groups will be specific to each
availability zone. That is, they are formed with zone names as well: `us-east-1-ppg@1`, `us-east-2-ppg@1`, etc. Hence,
if a PPG spans multiple availability zones then PLACEMENT_AWARE can still form partition groups.   
 
PPG ensures low latency between the members in the same partition of a placement group and also provides availability by
storing replicas in other partitions of the placement. As long as the partitions of a PPG contain an equal number of
instances, it will be good practice for Hazelcast clusters formed within a single zone.

#### Spread Placement Group (SPG)

SPG ensures availability by placing each instance in a group on a distinct rack. This way, it ensures availability
within a single zone. If a Hazelcast cluster is deployed with the default partition group strategy - which assumes each
member as a separate group, then this will be an appropriate configuration for the scenario where all instances belong to
a single SPG. If PLACEMENT_AWARE is enabled for a cluster under an SPG named `spg`, this will end up with a single 
partition group where all members belong to the same group (e.g. `us-east-1-spg`) - which will also have the same
logic as the default strategy. 

Forming a Hazelcast cluster within a single zone using the default partition group strategy and the instances in an SPG
will be good practice. However, SPG has a limitation of having at most 7 instances per zone. If you need more than 7
instances within a zone and want to ensure availability as much as possible, then consider using PPG. Another alternative
could be using more than one SPG. However, this action needs to be taken with care. Because two instances from different
SPGs can share the same rack - although they belong to different partition groups.

#### Cluster Placement Group (CPG)

CPG ensures low latency by packing instances close together inside an availability zone. If you favor latency over
availability, then CPG will serve your purpose. When deploying a Hazelcast cluster to the instances of a single CPG,
partition group strategy should not be a concern as you favor latency. Even if you enable PLACEMENT_AWARE for the instances
of a CFG named `cpg`, this will end up with a single partition group where all members belong to the same group 
(e.g. `us-east-1-cpg`) - which will also have the same logic as the default strategy. 

If you use more than one CPG - say `cpg1` and `cpg2`, for the instances within the same zone, the PLACEMENT_AWARE strategy 
will group the instances as `us-east-1-cpg1` and `us-east-1-cpg2`. This is somehow reasonable in terms of availability.
However, it might not be guaranteed that `cpg1` and `cpg2` won't share the same rack. Thus, PPG would fit better in this
case. If you use more than one CPG from different zones, this time ZONE_AWARE would be a better alternative to 
PLACEMENT_AWARE as it will pack all the instances within a single zone into the same partition group.

#### XML Configuration

```xml
<partition-group enabled="true" group-type="PLACEMENT_AWARE" />
```

#### YAML Configuration

```yaml
hazelcast:
  partition-group:
    enabled: true
    group-type: PLACEMENT_AWARE
```

#### Java-based Configuration

```java
config.getPartitionGroupConfig()
    .setEnabled(true)
    .setGroupType(MemberGroupType.PLACEMENT_AWARE);
```

***NOTE:*** *When using the `PLACEMENT_AWARE` partition grouping, a cluster spanning multiple Placement Groups (PG)
should have an equal number of members in each PG (and in each partition if the placement group is PPG). Otherwise,
it will result in uneven partition distribution among the members.*

***NOTE:*** *When using the `PLACEMENT_AWARE` with Cluster Placement Groups or Spread Placement Groups, remember 
that there will be only one partition group per placement group. So, favor Partition Placement Groups with Hazelcast
clusters formed within a single zone. If you use other placement groups with `PLACEMENT_AWARE` set up your placements
with care to have reasonable partition groups.*

## Autoscaling

Hazelcast is prepared to work correctly within the autoscaling environments. Note that there are two specific requirements to prevent Hazelcast from losing data:
* the number of members must change by 1 at the time
* when a member is launched or terminated, the cluster must be in the safe state

Read about details in the blog post: [AWS Auto Scaling with Hazelcast](https://hazelcast.com/blog/aws-auto-scaling-with-hazelcast/).

## AWS EC2 Deployment Guide

You can download the white paper "Amazon EC2 Deployment Guide for Hazelcast IMDG" [here](https://hazelcast.com/resources/amazon-ec2-deployment-guide/).

## How to find us?

In case of any question or issue, please raise a GH issue, send an email to [Hazelcast Google Groups](https://groups.google.com/forum/#!forum/hazelcast) or contact as directly via [Hazelcast Gitter](https://gitter.im/hazelcast/hazelcast).
