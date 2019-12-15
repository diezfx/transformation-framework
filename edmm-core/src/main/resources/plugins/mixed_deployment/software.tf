variable "compute_ip_adress"{

}

variable "compute_private_key"{

}



resource "null_resource" "cluster" {


  connection {
    host = "${compute_ip_adress}"
    private_key=file(variable.compute_private_key)
  }
  <#list ec2.remoteExecProvisioners as provisioner>
  <#if provisioner.scripts?size != 0>
  provisioner "remote-exec" {
    scripts = [
      <#list provisioner.scripts as script>
      "${script}"<#sep>,</#sep>
      </#list>
    ]
}
