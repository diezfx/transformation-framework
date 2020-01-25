variable "compute_ip_adress" {

}

variable "compute_private_key" {

}


resource "null_resource" "${name}" {


  connection {
    host = "${compute_ip_adress}"
    private_key = file(variable.compute_private_key)
  }


  <
  #list files as provisioner>
  provisioner "file" {
    source = "${provisioner.source}"
    destination = "${provisioner.destination}"
  }
  <
  /
  #list>

  <
  #if provisioner.scripts?size != 0>
  provisioner "remote-exec" {
    scripts = [
      <
    #list scripts as script>
    "${script}"
    <
    #sep>,</#sep>
    <
    /
    #list>
  ]
    <
    /
    #if>
  }