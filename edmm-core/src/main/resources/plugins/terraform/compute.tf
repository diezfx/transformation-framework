variable "region" {
  default = "eu-west-1"
}

variable "key_name" {
  default = "id_rsa"
}

variable "public_key_path" {
  default = "id_rsa.pub"
}

variable "ssh_user" {
  default = "ubuntu"
}

variable "aws_subnet_id"{
}

variable "aws_security_group_id"{

}

resource "aws_key_pair" "auth" {
  key_name = var.key_name
  public_key = file(var.public_key_path)
}

resource "aws_instance" "${ec2.name}" {
  ami = "${ec2.ami}"
  instance_type = "${ec2.instanceType}"
  key_name = aws_key_pair.auth.id
  vpc_security_group_ids = [var.aws_security_group_id]
  subnet_id = var.aws_subnet_id
}