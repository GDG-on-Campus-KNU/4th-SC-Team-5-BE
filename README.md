### 4th-SC-Team-5-BE

# VitalTrip (Server) 🌍🚑

**VitalTrip** is a service designed to support travelers facing sudden health issues or emergencies while abroad.  
It helps overcome language barriers, locates nearby medical facilities, and offers emergency response manuals along with AI-powered consultation.

## ✨ Key Features

- **Nearby Medical Facility Finder**
  - Provides real-time information on nearby hospitals and pharmacies based on the user's current location  
  - Integrated with Google Maps API

- **Symptom Translation & Voice Output**
  - Translates the user's symptoms or situation into the local language  
  - Plays the translated message out loud to facilitate communication with medical staff

- **Emergency Response Manual**
  - Static manuals offering instructions for various emergency scenarios

- **AI-Powered Emergency Consultation (Gemini)**
  - Offers contextual guidance using the Gemini LLM when manuals alone are insufficient

## 🛠 Tech Stack

- **Backend**: Java, Spring Boot, MySQL  
- **Maps & Location**: Google Maps API, Geolocation API  
- **AI Consultation**: Gemini LLM Integration (Google AI Studio)

### ☁️ Cloud Infrastructure
- **Infra**: Google Cloud (Compute Engine), Terraform  
- **Provider**: Google Cloud Platform (GCP)  
- **Region**: `us-central1`  
- **Zone**: `us-central1-c`  
- **OS Image**: `ubuntu-minimal-2204-jammy-v20250502`  
- **Instance Type**: `e2-small`
  - 2 vCPUs  
  - 4 GB Memory


## 🚀 Server Setup & Run Guide (Local)

### Set Up Docker Network

Create a network to connect the Spring Boot server with the MySQL container:

```bash
docker network create krew-network
```

### Set Up MySQL Container

1. Pull the MySQL image:

```bash
docker pull mysql
```

2. Run the container:

```bash
sudo docker run -d --name mysql-container -p 3306:3306 -e MYSQL_ROOT_PASSWORD=${CUSTOM_PASSWORD} --network krew-network mysql:latest
```

3. Access the container:

```bash
docker exec -it mysql-container bash
```

4. Configure MySQL user:

```bash
mysql -u root -p

create user '${CUSTOM_USER}'@'%' identified by '${CUSTOM_PASSWORD}';

CREATE DATABASE vitaltrip_database DEFAULT CHARACTER SET UTF8;

grant all privileges on *.* to 'krewadmin'@'%';

flush privileges;
```

### Set Up `krew-backend` (Spring Server Container)

1. Place a `.env` file in the root directory:

```bash
DB_HOST=${DB_HOST}
DB_PORT=${DB_PORT}
DB_USERNAME=${CUSTOM_USER}
DB_DATABASE=vitaltrip_database
DB_PASSWORD=${CUSTOM_PASSWORD}
GEMINI_API_KEY=${GEMINI_API_KEY}
```

2. Run the container

✅ Note: Tag number may change. Current version is **1.0**

```bash
docker run -d --name vitaltrip -p 8080:8080 --env-file .env --network krew-network adorableco/vitaltrip:1.0
```


## 🌥️ Deploying to Google Cloud (Terraform)

1. Navigate to the `terraform` directory:

```bash
cd vitaltrip/terraform
```

2. Add your service account JSON key file (from Google Cloud) to the `terraform` directory  
> Make sure to update the JSON filename in `main.tf` to match your actual file.
<img width="447" alt="스크린샷 2025-05-10 오후 12 40 54" src="https://github.com/user-attachments/assets/3e79b2b0-3488-408f-bc80-f5da9759eec9" />

3. Run Terraform commands:

```bash
terraform init
terraform apply
```

4. SSH into the created Compute Engine instance (via Google Cloud Console)

5. Create a `.env` file in the root directory with the following content:

```bash
DB_HOST=mysql-container
DB_PORT=${DB_PORT}
DB_USERNAME=${CUSTOM_USER}
DB_DATABASE=vitaltrip_database
DB_PASSWORD=${CUSTOM_PASSWORD}
GEMINI_API_KEY=${GEMINI_API_KEY}
```

> ✅ This `.env` file is required for the VitalTrip container to run properly.  
> If the container was started before creating the file, make sure to restart the container for it to work correctly. 

