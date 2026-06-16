# L7_POS

JavaFX 기반의 데스크톱 POS(Point Of Sale) 시스템입니다.
판매할 상품의 코드와 가격을 등록한 뒤, 여러 개의 상품을 하나의 판매내역 단위로 묶어 저장하고 관리할 수 있도록 구현한 로컬 POS 프로그램입니다.

## 프로젝트 개요

L7_POS는 매장에서 사용할 수 있는 간단한 POS 관리 프로그램을 목표로 개발되었습니다.
상품 정보를 먼저 등록하고, 등록된 상품 코드를 기반으로 판매 상품을 추가하여 하나의 판매내역을 생성할 수 있습니다.

JavaFX를 이용해 데스크톱 GUI를 구성하였으며, SQLite를 사용하여 별도의 서버 없이 로컬 환경에서 데이터를 저장할 수 있도록 설계했습니다.

## 주요 기능

### 상품 등록

* 판매할 상품 코드 등록
* 상품 가격 등록
* 상품 코드 중복 방지
* 등록된 상품 정보를 판매내역 등록 시 활용

### 판매내역 등록

* 등록된 상품 코드를 기준으로 판매 상품 추가
* 여러 개의 상품을 하나의 판매내역으로 묶어 저장
* 판매 상품별 가격 및 금액 저장
* 판매내역의 총 수량 및 총 금액 계산
* 판매 상세 항목 저장

### 판매내역 관리

* 등록된 판매내역 목록 조회
* 판매내역 상세 정보 확인
* 판매내역 수정
* 판매내역 삭제
* 테이블 기반 판매내역 표시

### 로컬 데이터 저장

* SQLite 기반 로컬 DB 사용
* 별도 DB 서버 없이 실행 가능
* 상품 정보와 판매내역을 로컬 환경에 저장

### 백업 기능

* 애플리케이션 실행 시 DB 백업 가능
* 백업 파일을 별도 폴더에 저장
* 로컬 데이터 손상에 대비한 백업 구조 제공

## 기술 스택

### Language

* Java 21

### GUI

* JavaFX 21.0.6
* FXML
* CSS

### Database

* SQLite
* JDBC
* JPA

### Build Tool

* Gradle Kotlin DSL

### Packaging

* jpackage

## 개발 환경

* Java 21
* Gradle
* JavaFX 21.0.6
* SQLite
* macOS / Windows 실행 가능 구조

## 프로젝트 구조 예시

```text
L7_POS
├── src
│   └── main
│       ├── java
│       │   └── com
│       │       └── l7pos
│       │           └── l7_pos
│       │               ├── Launcher.java
│       │               ├── HelloApplication.java
│       │               ├── controller
│       │               ├── entity
│       │               ├── repository
│       │               └── util
│       └── resources
│           └── com
│               └── l7pos
│                   └── l7_pos
│                       ├── main-view.fxml
│                       └── css
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## 실행 방법

### 1. 프로젝트 클론

```bash
git clone <repository-url>
cd L7_POS
```

### 2. Java 버전 확인

```bash
java -version
```

Java 21 이상이 필요합니다.

### 3. Gradle 실행

```bash
./gradlew run
```

Windows 환경에서는 다음 명령어를 사용할 수 있습니다.

```bash
gradlew run
```

## 패키징 방법

macOS 환경에서는 다음 명령어로 `.dmg` 파일을 생성할 수 있습니다.

```bash
./gradlew clean jpackage
```

패키징 결과물 예시:

```text
build/jpackage/L7_POS-1.0.0.dmg
```

Windows 환경에서는 `.exe` 생성을 위해 WiX Toolset 설치가 필요할 수 있습니다.

## 데이터베이스

<img width="1536" height="1024" alt="L7_POS ERD" src="https://github.com/user-attachments/assets/b883680d-24d4-46fc-96fd-89cd530f49f8" />

L7_POS는 SQLite를 사용하며, 로컬 환경에 DB 파일을 저장합니다.

### 주요 테이블

#### product

판매할 상품의 기본 정보를 저장하는 테이블입니다.

* 상품 ID
* 상품 코드
* 가격

#### sale

하나의 판매내역 정보를 저장하는 테이블입니다.

* 판매 번호
* 판매 일시
* 총 수량
* 총 금액

#### sale_item

판매내역에 포함된 개별 상품 정보를 저장하는 테이블입니다.
하나의 판매내역은 여러 개의 판매 상세 항목을 가질 수 있습니다.

* 판매 상세 ID
* 판매 번호
* 바코드
* 상품 코드
* 상품명
* 색상
* 사이즈
* 가격
* 금액

DB 경로 예시:

```text
jdbc:sqlite:${user.home}/l7pos.db
```

백업 파일은 다음과 같은 형태로 저장될 수 있습니다.

```text
L7_POS/backup/l7pos_YYYY-MM-DD.db
```

## 주요 화면

### 상품 등록 화면

<img width="3840" height="2160" alt="상품 등록 화면" src="https://github.com/user-attachments/assets/21123cda-7498-46ee-9e2c-dea20de82ede" />

판매할 상품의 코드와 가격을 입력하여 상품 정보를 등록할 수 있습니다.
등록된 상품 정보는 판매내역 등록 시 상품 코드 기준으로 조회되어 사용됩니다.

### 판매내역 등록 화면

<img width="3840" height="2160" alt="판매내역 등록 화면" src="https://github.com/user-attachments/assets/1426ff9e-6de4-4931-9dbd-4a4e85b653b4" />

등록된 상품의 바코드 또는 상품 코드를 입력하여 판매 상품을 추가할 수 있습니다.
여러 개의 상품을 하나의 판매내역 단위로 묶어 저장할 수 있으며, 판매내역의 총 수량과 총 금액을 관리할 수 있습니다.

### 판매내역 조회 화면

<img width="3840" height="2160" alt="판매내역 조회 화면" src="https://github.com/user-attachments/assets/14639379-89e7-49f0-bda4-ecb2016078e9" />

등록된 판매내역 목록을 테이블 형태로 확인할 수 있습니다.
선택한 판매내역의 상세 정보를 확인할 수 있으며, 판매내역 수정 및 삭제가 가능합니다.

## 프로젝트 특징

* JavaFX 기반 데스크톱 POS 애플리케이션
* SQLite를 이용한 로컬 데이터 저장
* 서버 없이 단독 실행 가능
* 상품 코드와 가격 등록 기능 구현
* 하나의 판매내역에 여러 상품을 등록하는 구조 구현
* 판매내역 총 수량 및 총 금액 관리
* 판매내역 조회, 수정, 삭제 기능 구현
* Gradle 기반 빌드 및 jpackage 패키징 지원
* POS 시스템의 기본 데이터 흐름 학습에 적합

## 향후 개선 사항

* 상품명, 색상, 사이즈 등 상품 정보 관리 기능 확장
* 재고 관리 기능 추가
* 영수증 출력 기능 추가
* 매출 통계 화면 추가
* 로그인 및 사용자 권한 관리 기능 추가
* 상품 카테고리 관리 기능 추가
* 바코드 스캔 기능 연동
* 판매 취소 및 환불 기능 추가
* 백업/복구 기능 고도화
