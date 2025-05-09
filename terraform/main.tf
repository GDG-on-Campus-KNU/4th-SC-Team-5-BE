provider "google" {
  project = var.project_id
  credentials = "${file("knu-krew-9d1da73edc55.json")}"
  region  = "us-central1"
  zone    = "us-central1-c"
}

resource "google_compute_network" "vitaltrip_network" {
  name = "vitaltrip-network"
}

resource "google_compute_firewall" "allow_8080_3306" {
  name    = "allow-22-8080-3306"
  network = google_compute_network.vitaltrip_network.name

  allow {
    protocol = "tcp"
    ports    = ["22", "8080", "3306"]
  }

  source_ranges = ["0.0.0.0/0"]
  direction     = "INGRESS"
}

resource "google_compute_instance" "vitaltrip_instance" {
  name         = "vitaltrip-instance"
  machine_type = "e2-small"
  zone         = "us-central1-c"

  boot_disk {
    initialize_params {
      image = "ubuntu-os-cloud/ubuntu-minimal-2204-jammy-v20250502"
    }
  }

  network_interface {
    network = google_compute_network.vitaltrip_network.name

    access_config {
      // 외부 IP 할당
    }
  }

  metadata_startup_script = file("startup.sh")

  tags = ["http-server", "mysql-access"]
}
