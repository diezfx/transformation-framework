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


variable "os_auth_url" {
}

variable "os_project_id" {

}


variable "os_user_domain_name" {

}

variable "os_username" {

}
variable "os_password" {

}

variable "os_region_name" {}


provider "openstack" {
  user_name = var.os_username
  password = var.os_password
  auth_url = var.os_auth_url
  region = var.os_region_name
  user_domain_name = var.os_user_domain_name
  tenant_id = var.os_project_id
}


#TODO more of the vars in model
resource "openstack_compute_instance_v2" "${ec2.name}" {
  name = "${ec2.name}"
  image_name = "Ubuntu 18.04"
  flavor_name = "m1.nano"
  key_pair = "win10key"
  security_groups = [
    "default"]

  metadata = {
    this = "that"
  }

  network {
    name = "public-belwue"
  }

}

resource "local_file" "compute_${ec2.name}" {
  content = jsonencode( {
    "host" = {
      "address" = openstack_compute_instance_v2.${ec2.name}.access_ip_v4
    }
  })
  filename = "compute_${ec2.name}.json"
}
output "compute_${ec2.name}_address" {
  value = openstack_compute_instance_v2.${ec2.name}.access_ip_v4
}