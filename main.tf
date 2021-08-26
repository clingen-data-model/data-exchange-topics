terraform {
  required_providers {
    confluentcloud = {
      source = "Mongey/confluentcloud"
    }
    kafka = {
      source  = "Mongey/kafka"
      version = "0.2.11"
    }
  }

  backend "gcs" {
    bucket = "clingen-tfstate-dataexchange-topics"
    prefix = "terraform/state"
  }
}

resource "confluentcloud_api_key" "provider_test" {
  cluster_id     = "our_neat_kafka_cluster"
  environment_id = "our_neat_environment"
}

resource "confluentcloud_service_account" "test" {
  name        = "test"
  description = "service account test"
}

locals {
  bootstrap_servers = ["server1.ccloud", "server2.ccloud", "server3.ccloud"]
}

provider "kafka" {
  bootstrap_servers = local.bootstrap_servers

  tls_enabled    = true
  sasl_username  = confluentcloud_api_key.provider_test.key
  sasl_password  = confluentcloud_api_key.provider_test.secret
  sasl_mechanism = "plain"
  timeout        = 10
}

resource "kafka_topic" "our_neat_topic" {
  name               = "our_neat_topic"
  replication_factor = 3
  partitions         = 1
  config = {
    "cleanup.policy" = "delete"
  }
}
