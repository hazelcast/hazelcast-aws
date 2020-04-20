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

To use Hazelcast embedded in your application, you need to add the plugin dependency into your Maven/Gradle file (or use [hazelcast-all](https://mvnrepository.com/artifact/com.hazelcast/hazelcast-all) which includes the plugin). Then, when you provide `hazelcast.xml`/`hazelcast.yaml` as presented below or an equivalent Java-based configuration, your Hazelcast instances discover themselves automatically.

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

Hazelcast member starts by fetching a list of all running instances filtered by the plugin parameters (`region`, etc.). Then, each instance is checked one-by-one with its IP and each of the ports defined in the `hz-port` property. When a member is discovered under IP:PORT, then it joins the cluster.

Note that this plugin supports [Hazelcast Zone Aware](https://docs.hazelcast.org/docs/latest/manual/html-single/#zone_aware) feature.

## Configuration

The plugin is prepared to work for both **AWS EC2** and **AWS ECS/Fargate** environments. However, note that the filter parameters and the requirements vary depending on the environment used.

### EC2 environment

The plugin works both for **Hazelcast Member Discovery** (forming Hazelcast cluster) and **Hazelcast Client Discovery**.

#### Hazelcast Member Discovery

Make sure that:

* you have the `hazelcast-aws.jar` (or `hazelcast-all.jar`) dependency in your classpath
* your IAM Role has `ec2:DescribeInstances` permission

Then, you can configure Hazelcast in one of the following manners.

##### XML Configuration

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

##### YAML Configuration

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

##### Java-based Configuration

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

#### Hazelcast Client Configuration

Hazelcast Client discovery parameters are the same as mentioned above.

If Hazelcast Client is run **outside AWS**, then you need to always specify the following parameters:
- `access-key`, `secret-key` - IAM role cannot be used from outside AWS
- `region` - it cannot be detected automatically
- `use-public-ip` - must be set to `true`

Note also that your EC2 instances must have public IP assigned.

Following are example declarative and programmatic configuration snippets.

##### XML Configuration

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

##### YAML Configuration

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

##### Java-based Configuration

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

### ECS/Fargate environment

The plugin works both for **Hazelcast Member Discovery** (forming Hazelcast cluster) and **Hazelcast Client Discovery**.

#### Hazelcast Member Discovery

Make sure that IAM Task Role has the following permissions:
* `ecs:ListTasks`
* `ecs:DescribeTasks`
* `ec2:DescribeNetworkInterfaces` (needed only if task have public IPs)

Then, you can configure Hazelcast in one of the following manners. Please note that `10.0.*.*` value depends on your VPC CIDR block definition.

##### XML Configuration

```xml
<hazelcast>
  <network>
    <join>
      <multicast enabled="false"/>
      <aws enabled="true" />
      </aws>
    </join>
    <interfaces enabled="true">
      <interface>10.0.*.*</interface>
    </interfaces>
  </network>
</hazelcast>
```

##### YAML Configuration

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

##### Java-based Configuration

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
* `host-header`: `ec2`, `ecs`, or the URL of a EC2/ECS API endpoint; automatically detected by default
* `connection-timeout-seconds`, `read-timeout-seconds`: connection and read timeouts when making a call to AWS API; default to `10`
* `connection-retries`: number of retries while connecting to AWS API; default to `3`
* `hz-port`: a range of ports where the plugin looks for Hazelcast members; default is `5701-5708`

Note that if you don't specify any of the properties, then the plugin discovers all Hazelcast members running in the current ECS cluster.

#### Hazelcast Client Configuration

Hazelcast Client discovery parameters are the same as mentioned above.

If Hazelcast Client is run **outside ECS cluster**, then you need to always specify the following parameters:
- `access-key`, `secret-key` - IAM role cannot be used from outside AWS
- `region` - it cannot be detected automatically
- `cluster` - it cannot be detected automatically
- `use-public-ip` - must be set to `true`

Note also that your ECS Tasks must have public IP assigned and your IAM Task Role must have `ec2:DescribeNetworkInterfaces` permission.

Following are example declarative and programmatic configuration snippets.

##### XML Configuration

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

##### YAML Configuration

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

##### Java-based Configuration

```java
clientConfig.getNetworkConfig().getAwsConfig()
      .setEnabled(true)
      .setProperty("access-key", "my-access-key")
      .setProperty("secret-key", "my-secret-key")
      .setProperty("region", "eu-central-1")
      .setProperty("cluster", "my-cluster")
      .setProperty("use-public-ip", "true");
```

### ECS environment with EC2 discovery

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

## Zone Aware

Hazelcast AWS Discovery plugin supports Hazelcast Zone Aware feature for both EC2 and ECS. When using `ZONE_AWARE` configuration, backups are created in the other Availability Zone.

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

***NOTE:*** *When using the `ZONE_AWARE` partition grouping, a cluster spanning multiple Availability Zones (AZ) should have an equal number of members in each AZ. Otherwise, it will result in uneven partition distribution among the members.*








## Configuration for AWS ECS

In order to enable discovery within AWS ECS Cluster, within `taskdef.json` or container settings, Hazelcast member should be bind to `host` network. Therefore, proper json representation for task should contain below segment:
```
"networkMode": "host"
```

Also, cluster member should have below interface binding in `hazelcast.xml`/`hazelcast.yaml` configuration file.

```xml
<interfaces enabled="true">
    <interface>10.0.*.*</interface>
</interfaces>
```

```yaml
hazelcast:
  network:
    interfaces:
      enabled: true
      interfaces:
        - 10.0.*.*
```
Please note that `10.0.*.*` value depends on your CIDR block definition.
If more than one `subnet` or `custom VPC` is used for cluster, it should be checked that `container instances` within cluster have network connectivity or have `tracepath` to each other. 


### IAM Roles in ECS Environment

hazelcast-aws supports ECS and will fetch default credentials if hazelcast is deployed into ECS environment. You don't have to configure `iam-role` tag. However, if you have a specific IAM Role to use, you can still use it via `iam-role` tag.


## AWS Autoscaling

There are specific requirements for the Hazelcast cluster to work correctly in the AWS Autoscaling Group:
* the number of instances must change by 1 at the time
* when an instance is launched or terminated, the cluster must be in the safe state

Otherwise, there is a risk of data loss or an impact on performance.

The recommended solution is to use **[Autoscaling Lifecycle Hooks](https://docs.aws.amazon.com/autoscaling/ec2/userguide/lifecycle-hooks.html)** with **Amazon SQS**, and the **custom lifecycle hook listener script**. If your cluster is small and predictable, then you can try the simpler alternative solution using **[Cooldown Period](https://docs.aws.amazon.com/autoscaling/ec2/userguide/Cooldown.html)**.

### AWS Autoscaling using Lifecycle Hooks

This solution is recommended and safe, no matter what your cluster and data sizes are. Nevertheless, if you find it too complex, you may want to try alternative solutions.

#### AWS Autoscaling Architecture

The necessary autoscaling architecture is presented on the diagram.

![architecture](markdown/images/aws-autoscaling-architecture.png)

The following activities must be performed to set up the Hazelcast AWS Autoscaling process:

* Create **AWS SQS** queue
* Add **`lifecycle_hook_listener.sh`** to each instance
  * The `lifecycle_hook_listener.sh` script can be started as the User Data script
  * Alternatively, it's possible to create a separate dedicated EC2 Instance running only `lifecycle_hook_listener.sh` in the same network and leave the autoscaled EC2 Instances only for Hazelcast members
* Set `Scaling Policy` to `Step scaling` and increase/decrease always by adding/removing `1 instance`
* Create `Lifecycle Hooks`
  * Instance Launch Hook
  * Instance Terminate Hook

#### Lifecycle Hook Listener Script

The `lifecycle_hook_listener.sh` script takes one argument as a parameter: `queue_name` (AWS SQS name). It performs operations that can be expressed in the following pseudocode.

```
while true:
    message = receive_message_from(queue_name)
    instance_ip = extract_instance_ip_from(message)
    while not is_cluster_safe(instance_ip):
        sleep 5
    send_continue_message
```

### AWS Autoscaling Alternative Solutions

The solutions below are not recommended, since they may not operate well under certain conditions. Therefore, please use them with caution.

#### Cooldown Period

[Cooldown Period](https://docs.aws.amazon.com/autoscaling/ec2/userguide/Cooldown.html) is a statically defined time interval that AWS Autoscaling Group waits before the next autoscaling operation takes place. If your cluster is small and predictable, then you can use it instead of Lifecycle Hooks. 

* Set `Scaling Policy` to `Step scaling` and increase/decrease always by adding/removing `1 instance`
* Set `Cooldown Period` to a reasonable value (which depends on the cluster and data size)

Note the drawbacks:

 * If the cluster contains a significant amount of data, it may be impossible to define one static cooldown period
 * Even if the cluster comes back to the safe state quicker, the next operation needs to wait the defined cooldown period

#### Graceful Shutdown

A solution that may sound simple and good (but is actually not recommended) is to use **[Hazelcast Graceful Shutdown](http://docs.hazelcast.org/docs/3.9.4/manual/html-single/index.html#how-can-i-shutdown-a-hazelcast-member)** as a hook on the EC2 Instance Termination. In other words, without any Autoscaling-specific features (like Lifecycle Hooks), you could adapt the EC2 Instance to wait for the Hazelcast member to shutdown before terminating the instance.

Such solution may work correctly, however is definitely not recommended for the following reasons:

* [AWS Autoscaling documentation](https://docs.aws.amazon.com/autoscaling/ec2/userguide/what-is-amazon-ec2-auto-scaling.html) does not specify the instance termination process, so it's hard to rely on anything
* Some sources ([here](https://stackoverflow.com/questions/11208869/amazon-ec2-autoscaling-down-with-graceful-shutdown)) specify that it's possible to gracefully shut down the processes, however after 20 seconds (which may not be enough for Hazelcast) the processes can be killed anyway
* The [Amazon's recommended way](https://docs.aws.amazon.com/autoscaling/ec2/userguide/lifecycle-hooks.html) to deal with graceful shutdowns is to use Lifecycle Hooks

## Hazelcast Performance on AWS

Amazon Web Services (AWS) platform can be an unpredictable environment compared to traditional in-house data centers. This is because the machines, databases or CPUs are shared with other unknown applications in the cloud, causing fluctuations. When you gear up your Hazelcast application from a physical environment to Amazon EC2, you should configure it so that any network outage or fluctuation is minimized and its performance is maximized. This section provides notes on improving the performance of Hazelcast on AWS.

### Selecting EC2 Instance Type

Hazelcast is an in-memory data grid that distributes the data and computation to the members that are connected with a network, making Hazelcast very sensitive to the network. Not all EC2 Instance types are the same in terms of the network performance. It is recommended that you choose instances that have **High** or **10 Gigabit+** network performance for Hazelcast deployments.

You can check latest Instance Types on [Amazon EC2 Instance Types](https://aws.amazon.com/ec2/instance-types/).

### Dealing with Network Latency

Since data is sent and received very frequently in Hazelcast applications, latency in the network becomes a crucial issue. In terms of the latency, AWS cloud performance is not the same for each region. There are vast differences in the speed and optimization from region to region.

When you do not pay attention to AWS regions, Hazelcast applications may run tens or even hundreds of times slower than necessary. The following notes are potential workarounds.

- Create a cluster only within a region. It is not recommended that you deploy a single cluster that spans across multiple regions.
- If a Hazelcast application is hosted on Amazon EC2 instances in multiple EC2 regions, you can reduce the latency by serving the end users requests from the EC2 region which has the lowest network latency. Changes in network connectivity and routing result in changes in the latency between hosts on the Internet. Amazon has a web service (Route 53) that lets the cloud architects use DNS to route end-user requests to the EC2 region that gives the fastest response. This latency-based routing is based on latency measurements performed over a period of time. Please have a look at [Route53](http://docs.aws.amazon.com/Route53/latest/DeveloperGuide/HowDoesRoute53Work.html).
- Move the deployment to another region. The [CloudPing](http://www.cloudping.info/) tool gives instant estimates on the latency from your location. By using it frequently, CloudPing can be helpful to determine the regions which have the lowest latency.
- The [SpeedTest](http://cloudharmony.com/speedtest) tool allows you to test the network latency and also the downloading/uploading speeds.

### Selecting Virtualization

AWS uses two virtualization types to launch the EC2 instances: Para-Virtualization (PV) and Hardware-assisted Virtual Machine (HVM). According to the tests we performed, HVM provided up to three times higher throughput than PV. Therefore, we recommend you use HVM when you run Hazelcast on EC2.

***RELATED INFORMATION***

*You can download the white paper "Amazon EC2 Deployment Guide for Hazelcast IMDG" [here](https://hazelcast.com/resources/amazon-ec2-deployment-guide/).*
