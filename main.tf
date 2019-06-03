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