provider "google" {
    credentials = file(".gcp.credentials.json")
    project = file(".gcp.project_id")
    region = "us-central1"
    zone = "us-central1-c"
}

resource "google_storage_bucket" "photo-store" {
    name = file(".gcp.bucket_name")
    location = "us-central1"
    storage_class = "REGIONAL"
}

resource "google_storage_default_object_access_control" "public_rule" {
  bucket = google_storage_bucket.photo-store.name
  role   = "READER"
  entity = "allUsers"
}

# small database for develpoment purposes, do not use in production!
resource "google_sql_database_instance" "master" {
  name = "db-master-instance"
  database_version = "POSTGRES_11"
  region = "us-central1"

  settings {
    tier = "db-f1-micro"
    disk_type = "PD_HDD"
    availability_type = "ZONAL"
    disk_autoresize = false

    backup_configuration {
      enabled = false
    }

    ip_configuration {
      ipv4_enabled = true
      require_ssl = false
    }
  }
}

resource "google_sql_database" "photostorage_db" {
  name = "photostorage"
  instance = google_sql_database_instance.master.name
}

data "google_container_registry_repository" "registry" {
  region = "us"
}