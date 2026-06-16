# L7_POS

JavaFX 기반의 데스크톱 POS(Point Of Sale) 시스템입니다.
상품 등록, 상품 조회, 상품 수정, 삭제 기능을 중심으로 매장 상품 데이터를 관리할 수 있도록 구현한 로컬 POS 프로그램입니다.

<img width="3840" height="2160" alt="이미지 2026  6  16  오후 7 19 (1)" src="https://github.com/user-attachments/assets/21123cda-7498-46ee-9e2c-dea20de82ede" />

<img width="3840" height="2160" alt="이미지 2026  6  16  오후 7 19" src="https://github.com/user-attachments/assets/1426ff9e-6de4-4931-9dbd-4a4e85b653b4" />

<img width="3840" height="2160" alt="이미지 2026  6  16  오후 7 18" src="https://github.com/user-attachments/assets/14639379-89e7-49f0-bda4-ecb2016078e9" />

## 프로젝트 개요

L7_POS는 매장에서 사용할 수 있는 간단한 POS 관리 프로그램을 목표로 개발되었습니다.
JavaFX를 이용해 데스크톱 GUI를 구성하고, SQLite를 사용하여 별도의 서버 없이 로컬 환경에서 데이터를 저장할 수 있도록 설계했습니다.

## 주요 기능

### 상품 관리

* 상품 등록
* 상품 목록 조회
* 상품 정보 수정
* 상품 삭제
* 상품 코드 기반 검색
* 테이블 기반 상품 목록 표시

### 로컬 데이터 저장

* SQLite 기반 로컬 DB 사용
* 사용자 홈 디렉터리에 DB 파일 저장
* 별도 DB 서버 없이 실행 가능

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

### Build Tool

* Gradle Kotlin DSL

### Packaging

* jpackage

## 개발 환경

* Java 21
* Gradle
* JavaFX 21.0.6
* SQLite JDBC
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

L7_POS는 SQLite를 사용하며, 로컬 환경에 DB 파일을 저장합니다.

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

상품명, 상품 코드, 가격 등 상품 정보를 입력하여 새로운 상품을 등록할 수 있습니다.

### 상품 조회 화면

등록된 상품 목록을 테이블 형태로 확인할 수 있으며, 상품 코드 검색을 통해 원하는 상품을 빠르게 찾을 수 있습니다.

### 상품 수정 및 삭제

상품 목록에서 선택한 상품을 기준으로 정보를 수정하거나 삭제할 수 있습니다.

## 프로젝트 특징

* JavaFX 기반 데스크톱 애플리케이션
* SQLite를 이용한 로컬 데이터 저장
* 서버 없이 단독 실행 가능
* 상품 등록, 조회, 수정, 삭제 기능 구현
* Gradle 기반 빌드 및 jpackage 패키징 지원
* POS 시스템의 기본 구조 학습 및 확장에 적합

## 향후 개선 사항

* 판매 내역 관리 기능 추가
* 영수증 출력 기능 추가
* 재고 관리 기능 추가
* 매출 통계 화면 추가
* 로그인 및 사용자 권한 관리 기능 추가
* 상품 카테고리 관리 기능 추가
* 바코드 스캔 기능 연동
* 백업/복구 기능 고도화

## 실행 화면

추후 실행 화면 이미지를 추가할 수 있습니다.

```text
docs/images/main.png
docs/images/product-register.png
docs/images/product-list.png
```

## 개발 목적

이 프로젝트는 JavaFX, Gradle, SQLite를 활용하여 실제 데스크톱 POS 프로그램의 기본 구조를 구현하는 것을 목적으로 합니다.
상품 데이터를 등록하고 관리하는 흐름을 직접 구현하면서 GUI 애플리케이션 구조, 로컬 DB 연동, 패키징 과정을 학습하기 위해 제작되었습니다.

## 라이선스

개인 학습 및 포트폴리오 목적으로 제작된 프로젝트입니다.
