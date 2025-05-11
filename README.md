### 4th-SC-Team-5-BE

# VitalTrip (server) 🌍🚑

**VitalTrip**은 해외 여행 중 갑작스러운 건강 문제나 응급 상황이 발생했을 때, 여행자가 신속하고 안전하게 대응할 수 있도록 지원하는 서비스입니다.  
언어 장벽을 극복하고, 가까운 의료기관을 빠르게 찾아주며, 위급 상황별 대응 매뉴얼과 AI 기반 긴급 상담 기능을 제공합니다.

## ✨ 주요 기능

- **주변 의료기관 검색**
  - 현재 위치 기반으로 가까운 병원, 약국 정보를 실시간 안내
  - Google Maps API 연동

- **증상 번역 및 음성 출력**
  - 사용자의 증상/상황을 현지 언어로 번역
  - 번역된 문장을 음성으로 재생해 의료진과의 원활한 소통 지원

- **응급 상황 대응 매뉴얼 제공**
  - 주요 응급 상황별 대응 방법을 제공하는 정적 매뉴얼

- **AI 기반 긴급 상담 (Gemini)**
  - 정형화된 매뉴얼로 해결이 어려운 상황 발생 시, LLM(Gemini) 모델을 통해 상황별 맞춤형 조언 제공

## 🛠 사용 기술 스택

- **Backend**:Java, Spring Boot, MySQL
- **Maps & Location**: Google Maps API, Geolocation API
- **AI 상담**: Gemini LLM Integration (Google AI Studio)


### ☁️ 클라우드 환경
- **Infra**: Google Cloud(Compute Engine), Terraform
- **Provider**: Google Cloud Platform (GCP)
- **Region**: `us-central1`
- **Zone**: `us-central1-c`
- **OS Image**: `ubuntu-minimal-2204-jammy-v20250502`
- **Instance Type**: `e2-small`
  - 2 vCPUs
  - 4 GB Memory 

## 🚀 서버 설치 및 실행 방법 (로컬 실행)

### docker network 구성

- 스프링서버와 mysql 서버를 이어줄 네트워크를 구성함

```bash
docker network create krew-network
```

### mysql-container 구성

1. `mysql` 이미지 풀받기

```bash
docker pull mysql
```

2. 실행하기

```bash
sudo docker run -d --name mysql-container  -p 3306:3306  -e MYSQL_ROOT_PASSWORD=${CUSTOM_PASSWORD}  --network krew-network mysql:latest
```

3. 컨테이너 접속

```bash
docker exec -it mysql-container bash
```

4. mysql 사용자 세팅

```bash
mysql -u root -p

create user '${CUSTOM_USER}'@'%' identified by '${CUSTOM_PASSWORD}';

CREATE DATABASE vitaltrip_database DEFAULT CHARACTER SET UTF8;

grant all privileges on *.* to 'krewadmin'@'%';

flush privileges;
```

### krew-backend (스프링 서버 컨테이너) 구성

1. 루트 디렉토리에 .env 파일 두기

```bash
DB_HOST=${DB_HOST}
DB_PORT=${DB_PORT}
DB_USERNAME=${CUSTOM_USER}
DB_DATABASE=vitaltrip_database
DB_PASSWORD=${CUSTOM_PASSWORD}
GEMINI_API_KEY=${GEMINI_API_KEY}
```

2. 컨테이너 실행

✅ 태그 번호는 달라질 수 있음 현재는 **1.0**

```bash
docker run -d --name vitaltrip -p 8080:8080 --env-file .env --network krew-network adorableco/vitaltrip:1.0
```


## 🌥️🌥 클라우드 인프라(Google Cloud) 서버 배포 방법 (terraform 구축)

1.`terraform` 폴더로 이동하기
```bash
 cd vitaltrip/terraform
```
2. `terraform` 폴더에 google cloud에서 생성한 서비스 계정의 키 json 파일을 추가하기
> terraform/main.tf 윗부분에 있는 json 파일명을 본인의 json 파일명으로 바꿔줘야합니다.
<img width="447" alt="스크린샷 2025-05-10 오후 12 40 54" src="https://github.com/user-attachments/assets/3e79b2b0-3488-408f-bc80-f5da9759eec9" />

3. terraform 커맨드 수행
```bash
terraform init
terraform apply
```

4. 생성된 compute engine에 ssh 접속하기 (구글 클라우드 콘솔에서 수행)


5. root 디렉토리에 .env 파일 생성 후 아래 내용 넣기
```bash
DB_HOST=mysql-container
DB_PORT=${DB_PORT}
DB_USERNAME=${CUSTOM_USER}
DB_DATABASE=vitaltrip_database
DB_PASSWORD=${CUSTOM_PASSWORD}
GEMINI_API_KEY=${GEMINI_API_KEY}
```

> ✅ 이 .env 파일이 있어야 vitaltrip 도커 컨테이너가 정상 실행됩니다. 혹시 도커 컨테이너가 자동실행 된 이후에 .env를 추가했다면 해당 도커 컨테이너를 재실행해주면 정상작동합니다. 

