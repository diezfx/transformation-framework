package components

relation_types : [Name=_]: Relation_Type

components : [Name=_]: Component

component_types: [Name=_]: Component_type

Component :: {

	type?:        string
	description?: string
	metadata: {}
	operations?: Operations
	properties?: [Name=_]: string | int
	artifacts?: [...{[Name=_]: string}]
	relations?: [...{[Name=_]: string}]
}

components : [Name=_]: ComputeComp | Component



//idea einfach alle componentten zur auswahl stellen
// properties ist type und desc nicht nötig

ComputeComp : {

	type: "compute"
    lkjsdfjlksfdkjl:"sdflkjsdfjkl"

}

component_types: compute: {
	properties: {
		os_family: {
			type:          "string"
			description:   "Specifies the type of operating system"
			default_value: "linux"
		}
		machine_image: {
			type:        "string"
			description: "The name of the machine image to use"
		}
		instance_type: {
			type:        "string"
			description: "The name of the instance type to provision"
		}
		key_name: {
			type:        "string"
			description: "The name of the key pair to use for authentication"
		}
		public_key: {
			type:        "string"
			description: "The public key of the key pair to use for authentication"
		}
		ip_address: {
			type:        "string"
			description: "The ip address of the machine when deployed"
			computed:    true
		}
	}
}

Component_type :: {
	metadata?:    _ | _|_
	extends?:     null | string
	description?: string
	properties?: [Name=_]: Property
	operations?: Operations
}

Property :: {
	type:           "string" | "integer"
	description?:   string
	default_value?: string | int
	computed?:      bool
}

Operations : {
	create? :  null | string
	start?:    null | string
	stop? :    null | string
	configure: *null | string
	delete:    *null | string
}

Relation_Type :: {
	extends:     string | null
	properties?: _ | _|_
	operations?: _ | _|_
}
