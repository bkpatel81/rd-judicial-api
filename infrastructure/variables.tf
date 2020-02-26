variable "product" {
  type = "string"
}

variable "raw_product" {
  default = "rd" // jenkins-library overrides product for PRs and adds e.g. pr-123-ia
}

variable "component" {
  type = "string"
}

variable "location" {
  type    = "string"
  default = "UK South"
}

variable "env" {
  type = "string"
}

variable "subscription" {
  type = "string"
}

variable "ilbIp" {}

variable "common_tags" {
  type = "map"
}

variable "capacity" {
  default = "1"
}

variable "instance_size" {
  default = "I1"
}

variable "appinsights_instrumentation_key" {
  default = ""
}

variable "root_logging_level" {
  default = "INFO"
}

variable "log_level_spring_web" {
  default = "INFO"
}

variable "log_level_rd" {
  default = "INFO"
}

variable "team_name" {
  default     = "RD"
}

variable "managed_identity_object_id" {
  default = ""
}

variable "postgresql_version" {
  default = "11"
}

