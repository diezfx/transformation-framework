variable "region" {
  default = "eu-west-1"
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


data "tls_public_key" "priv_key" {
  private_key_pem = file($
{ec2.privKeyFile})
}


resource "openstack_compute_instance_v2" ${ec2.name} {
name = ${ec2.name}
image_name = "Ubuntu 18.04"
flavor_name = "m1.small"
key_pair = ${ec2.keyName}
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
      "hostname" = openstack_compute_instance_v2.${ec2.name}.access_ip_v4
  })
  filename = "${ec2.name}_computed_properties.json"
}
output "compute_${ec2.name}_address" {
  value = openstack_compute_instance_v2.${ec2.name}.access_ip_v4
}