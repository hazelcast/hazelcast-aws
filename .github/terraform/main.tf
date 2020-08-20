terraform {
  required_providers {
    aws = {
      source = "hashicorp/aws"
      version = "~> 3.2"
    }
  }
  required_version = ">= 0.13"
}

provider "aws" {
  region = var.aws_region
}

data "aws_ami" "ubuntu" {
  most_recent = true

  filter {
    name   = "name"
    values = ["ubuntu/images/hvm-ssd/ubuntu-bionic-18.04-amd64-server-*"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }

  owners = ["099720109477"]
}

#################### IAM role creation for discovery ###################

resource "aws_iam_role" "discovery_role" {
  name = "discovery_role"

  assume_role_policy = <<-EOF
  {
    "Version": "2012-10-17",
    "Statement": [
      {
        "Action": "sts:AssumeRole",
        "Principal": {
          "Service": "ec2.amazonaws.com"
        },
        "Effect": "Allow",
        "Sid": ""
      }
    ]
  }
  EOF
}

resource "aws_iam_role_policy" "discovery_policy" {
  name = "discovery_policy"
  role = aws_iam_role.discovery_role.id

  policy = <<-EOF
  {
    "Version": "2012-10-17",
    "Statement": [
      {
        "Action": [
          "ec2:DescribeInstances"
        ],
        "Effect": "Allow",
        "Resource": "*"
      }
    ]
  }
  EOF
}

resource "aws_security_group" "sg" {
  name = "test_sg"

  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port   = 5701
    to_port     = 5707
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }


  # Allow outgoing traffic to anywhere.
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_iam_instance_profile" "discovery_instance_profile" {
  name = "discovery_instance_profile"
  role = aws_iam_role.discovery_role.name
}

resource "aws_key_pair" "keypair" {
  key_name   = "id_rsa"
  public_key = file("${var.local_key_path}/${var.aws_key_name}.pub")
}
###########################################################################

resource "aws_instance" "hazelcast_member" {
  count                = var.member_count
  ami                  = data.aws_ami.ubuntu.id
  instance_type        = var.aws_instance_type
  iam_instance_profile = aws_iam_instance_profile.discovery_instance_profile.name
  security_groups      = [aws_security_group.sg.name]
  key_name             = aws_key_pair.keypair.key_name
  tags = {
    Name                 = "Hazelcast-AWS-Member-${count.index + 1}"
    "${var.aws_tag_key}" = var.aws_tag_value
  }
  connection {
    type        = "ssh"
    user        = "ubuntu"
    host        = self.public_ip
    timeout     = "180s"
    agent       = false
    private_key = file("${var.local_key_path}/${var.aws_key_name}")
  }

    provisioner "remote-exec" {
      inline = [
        "mkdir -p /home/${var.username}/jars",
        "mkdir -p /home/${var.username}/logs",
        "while [ ! -f /var/lib/cloud/instance/boot-finished ]; do echo 'Waiting for cloud-init...'; sleep 1; done",
        "sudo apt-get update",
        "sudo apt-get -y install openjdk-8-jdk wget",
        "sleep 30"
      ]
    }

    provisioner "file" {
      source      = "scripts/start_aws_hazelcast_member.sh"
      destination = "/home/${var.username}/start_aws_hazelcast_member.sh"
    }

    provisioner "file" {
      source      = "scripts/verify_member_count.sh"
      destination = "/home/${var.username}/verify_member_count.sh"
    }


    provisioner "file" {
        source      = "~/lib/hazelcast-aws.jar"
        destination = "/home/${var.username}/jars/hazelcast-aws.jar"
    }

    provisioner "file" {
        source      = "~/lib/hazelcast.jar"
        destination = "/home/${var.username}/jars/hazelcast.jar"
    }

    provisioner "file" {
        source      = "hazelcast.yaml"
        destination = "/home/${var.username}/hazelcast.yaml"
    }


  provisioner "remote-exec" {
    inline = [
      "cd /home/${var.username}",
      "chmod 0755 start_aws_hazelcast_member.sh",
      "./start_aws_hazelcast_member.sh  ${var.aws_region} ${var.aws_tag_key} ${var.aws_tag_value} ${var.aws_connection_retries}",
      "sleep 20",
      "tail -n 20 ./logs/hazelcast.stdout.log"
    ]
  }

  provisioner "remote-exec" {
    inline = [
      "sleep 10",
      "cd /home/${var.username}",
      "chmod 0755 verify_member_count.sh",
      "sh verify_member_count.sh  ${var.member_count}",
    ]
  }

}

resource "aws_instance" "hazelcast_mancenter" {
  ami                  = data.aws_ami.ubuntu.id
  instance_type        = var.aws_instance_type
  iam_instance_profile = aws_iam_instance_profile.discovery_instance_profile.name
  security_groups      = [aws_security_group.sg.name]
  key_name             = aws_key_pair.keypair.key_name
  tags = {
    Name                 = "Hazelcast-AWS-Management-Center"
    "${var.aws_tag_key}" = var.aws_tag_value
  }   

  connection {
    type        = "ssh"
    user        = "ubuntu"
    host        = self.public_ip
    timeout     = "180s"
    agent       = false
    private_key = file("${var.local_key_path}/${var.aws_key_name}")
  }

  provisioner "file" {
    source      = "scripts/start_aws_hazelcast_management_center.sh"
    destination = "/home/ubuntu/start_aws_hazelcast_management_center.sh"
  }


  provisioner "file" {
    source      = "scripts/verify_mancenter.sh"
    destination = "/home/${var.username}/verify_mancenter.sh"
  }

  provisioner "file" {
    source      = "hazelcast-client.yaml"
    destination = "/home/ubuntu/hazelcast-client.yaml"
  }

  provisioner "remote-exec" {
    inline = [
      "while [ ! -f /var/lib/cloud/instance/boot-finished ]; do echo 'Waiting for cloud-init...'; sleep 1; done",
      "sudo apt-get update",
      "sudo apt-get -y install openjdk-8-jdk wget unzip",
      "sleep 30"
    ]
  }

  provisioner "remote-exec" {
    inline = [
      "cd /home/ubuntu",
      "chmod 0755 start_aws_hazelcast_management_center.sh",
      "./start_aws_hazelcast_management_center.sh ${var.hazelcast_mancenter_version}  ${var.aws_region} ${var.aws_tag_key} ${var.aws_tag_value} ",
      "sleep 20",
      "tail -n 20 ./logs/mancenter.stdout.log",
    ]
  }
}

resource "null_resource" "verify_mancenter" {

  depends_on =  [aws_instance.hazelcast_member]
  connection {
    type        = "ssh"
    user        = "ubuntu"
    host        = aws_instance.hazelcast_mancenter.public_ip
    timeout     = "180s"
    agent       = false
    private_key = file("${var.local_key_path}/${var.aws_key_name}")
  }


  provisioner "remote-exec" {
  inline = [
    "cd /home/${var.username}",
    "chmod 0755 verify_mancenter.sh",
    "./verify_mancenter.sh  ${var.member_count}",
  ]
}
}
