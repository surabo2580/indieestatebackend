terraform {
  required_version = ">= 1.5"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

variable "aws_region" {
  type    = string
  default = "ap-south-1"
}

variable "project_name" {
  type    = string
  default = "indieestate"
}

variable "reels_s3_bucket" {
  type    = string
  default = "indiestate-reels-uat"
}

# Wire modules here as you harden production (see README.md).
# Example:
# module "network" { source = "./modules/network" project_name = var.project_name }
# module "ecs"     { source = "./modules/ecs"     ... task roles from module.iam ... }

output "next_steps" {
  value = "Implement modules/network, rds, elasticache, msk, iam, ecs — see infra/terraform/README.md"
}
