# hibiku

<p align="center">
  <img src="docs/images/widgets_overview.png" alt="hibiku Widgets Overview" width="100%" />
</p>

<p align="center">
  <a href="https://developer.android.com/about/versions/16"><img src="https://img.shields.io/badge/Android-16~17%20(API%2036~37)-3DDC84?style=flat-square&logo=android&logoColor=white" alt="Android API 36~37" /></a>
  <a href="https://kotlinlang.org"><img src="https://img.shields.io/badge/Kotlin-2.2.0-7F52FF?style=flat-square&logo=kotlin&logoColor=white" alt="Kotlin" /></a>
  <a href="https://developer.android.com/jetpack/compose"><img src="https://img.shields.io/badge/Jetpack%20Compose-BOM%202026.02.00-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose" /></a>
  <a href="https://developer.android.com/jetpack/androidx/releases/glance"><img src="https://img.shields.io/badge/Jetpack%20Glance-1.1.1-34A853?style=flat-square" alt="Jetpack Glance" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-MIT-blue.svg?style=flat-square" alt="License: MIT" /></a>
</p>

---

## 🎧 Overview

**hibiku**는 Android 16 (API 36) 및 Android 17 (API 37) 환경을 타겟으로 설계된 모던 뮤직 플레이어 위젯 & 이머시브 스탠바이 모드 애플리케이션입니다.

Spotify, YouTube Music, Apple Music, Samsung Music 등 표준 `MediaSession`을 제공하는 모든 오디오 앱의 재생 상태와 앨범 아트를 실시간으로 감지하여 홈 스크린에 미려한 위젯을 제공하며, 스마트 폰을 충전기에 거치하거나 전체화면 전환 시 감성적인 **StandBy Fullscreen Player**로 확장됩니다.

---

## ✨ Key Features

### 1. 4가지 스타일의 Glance 홈 위젯
Jetpack Glance Material 3 기반의 4가지 위젯 레이아웃을 제공합니다:
* **4x2 Standard Widget**: 원형 프로그레스 링 앨범아트, 곡명/아티스트, 이전곡/재생/일시정지/다음곡 컨트롤이 완비된 메인 위젯.
* **2x2 Standard Widget**: 2x2 그리드 내에 원형 앨범아트 프로그레스 링과 텍스트 정보, 간결한 3버튼 컨트롤을 배치한 컴팩트 위젯.
* **2x2 Minimal Widget (Tap-to-Dim)**: 평소에는 앨범아트와 프로그레스 링만 깔끔하게 표시되며, 탭 시 은은한 오버레이(Dimmed Controls)가 나타나 조작할 수 있는 미니멀 위젯.
* **2x2 Pure Widget**: 배경 카드 없이 앨범아트와 프로그레스 링만 100% 투명하게 띄워 배경화면과 완벽하게 일체화되는 퓨어 스타일 위젯.

### 2. 5가지 시그니처 프로그레스 링 & Palette 컬러 연동
앨범아트 둘레를 감싸는 프로그레스 링을 취향에 맞게 선택할 수 있습니다:
* **물결 웨이브 (Squiggly Wave)**: 음악이 재생되는 동안 생동감 있게 물결치는 파형 애니메이션 프로그레스 링.
* **플로팅 듀얼 링 (Floating Clean)**: 세련된 듀얼 서클과 트랙으로 단정하고 정갈한 프로그레스 링.
* **세그먼트 도트 (Segmented Minimal)**: 미세한 눈금 세그먼트로 분할되어 하이테크한 감성을 선사하는 프로그레스 링.
* **글로우 썸 (Glow Thumb)**: 헤드 팁에 영롱한 글로우 블러 이펙트가 적용된 프로그레스 링.
* **솔리드 클래식 (Solid Classic)**: 깔끔한 기본 단일 스트로크 프로그레스 링.
* **Android Palette 연동**: 앨범아트에서 지배적인 색상(Dominant / Vibrant)을 실시간 자동 추출하여 테두리 링 및 틴트에 적용 가능.

### 3. 반응형 이머시브 스탠바이 플레이어 (Immersive StandBy Player)
스마트폰 충전 거치 시 자동 진입하거나 위젯의 확장 버튼을 눌러 풀스크린 플레이어로 진입할 수 있습니다:
* **회전 잠금 강제 무시 (Full Sensor Override)**: 안드로이드 기기의 시스템 회전 잠금(세로 고정)이 걸려 있어도 물리 자이로 센서(`SCREEN_ORIENTATION_FULL_SENSOR`)를 즉시 감지하여 탁상 거치 시 16:9 가로 스탠바이 모드로 유연하게 전환.
* **반응형 2-Pane / 1-Pane 레이아웃**: 가로 모드에서는 좌측 대형 앨범아트와 우측 곡 정보/컨트롤/파형 시크바로 분할된 스탠바이 데스크탑 레이아웃 제공, 세로 모드에서는 일체형 풀스크린 레이아웃 제공.
* **5버튼 풀 컨트롤**: 셔플 스위치, 이전곡, 볼드 필 재생/일시정지, 다음곡, 반복 모드(반복 없음 / 전곡 반복 / 한곡 반복) 완벽 지원.
* **유기적 아티스틱 앨범아트 마스크**: 8자형 오가닉 곡선, 모핑 페블/조약돌, 클래식 LP 바이닐, 12포인트 스캘럽 별빛 셰이프 마스크 적용.
* **Squiggly Sine-wave SeekBar**: 현재 재생 위치까지 부드러운 사인파 곡선으로 넘실거리는 감성적 시크바 및 실시간 터치/드래그 탐색 지원.

### 4. 배터리 친화적 스마트 티커 (Smart Ticker)
* 홈 화면 위젯의 1초 단위 프로그레스 업데이트는 **화면이 켜져 있고(SCREEN_ON)** 음악이 **실제 재생 중(isPlaying == true)** 일 때만 제한적으로 실행되어 백그라운드 배터리 소모를 0에 가깝게 최소화합니다.

---

## 📸 Screenshots

### 홈 위젯 라인업 (4x2, 2x2 Standard, 2x2 Minimal, 2x2 Pure)
![Home Widgets](docs/images/widgets_overview.png)

### 이머시브 스탠바이 플레이어
<!-- 사용자가 캡처한 실제 디바이스 스크린샷 이미지 배치 예정 -->
| 가로 스탠바이 모드 (Landscape 16:9) | 세로 전체화면 모드 (Portrait) |
|:---:|:---:|
| <img src="docs/images/immersive_landscape.png" alt="Landscape StandBy" width="100%" /> | <img src="docs/images/immersive_portrait.png" alt="Portrait StandBy" width="60%" /> |

---

## 🏗 Architecture & Modules

프로젝트는 명확한 관심사 분리(SoC)를 위해 멀티 모듈 아키텍처로 구성되어 있습니다:

```text
hibiku/
├── app/          # Application 진입점 및 의존성 주입 조립
├── core/         # 그래픽 렌더링(5종 링, 블러), Palette 추출, 유틸리티
├── domain/       # MediaPlaybackState, WidgetConfig, Repository 인터페이스
└── feature/      # Glance 위젯 4종, M3 환경설정 Activity, 이머시브 스탠바이 Activity, MediaService
```

* **`:domain`**: UI 및 플랫폼 의존성이 없는 순수 비즈니스 모델 및 재생 상태 계약.
* **`:core`**: Canvas 기반 5가지 프로그레스 링 렌더러 (`WidgetBitmapRenderer`), 비트맵 블러, 앨범아트 팔레트 추출기 (`PaletteExtractor`).
* **`:feature`**:
  * `MediaNotificationListenerService`: 활성 MediaController 및 PlaybackState 자동 추적.
  * Glance 위젯 4종 (`MusicWidget4x2`, `MusicWidget2x2Standard`, `MusicWidget2x2Minimal`, `MusicWidget2x2Pure`).
  * `WidgetConfigurationActivity`: Jetpack Compose 기반 위젯 커스텀 및 실시간 비트맵 렌더링 미리보기.
  * `ImmersivePlayerActivity`: 자이로 센서 감지 풀스크린 스탠바이 플레이어, Squiggly 시크바, 5버튼 컨트롤러.

---

## 🚀 Getting Started

### 요구 사항
* Android Studio Ladybug (2024.2) 이상 또는 Meerkat 이상 권장
* JDK 17 / 21
* Android 16 (API 36) 이상 지원 기기 또는 에뮬레이터 (compileSdk 37, minSdk 36)

### 빌드 및 실행
```bash
# 디버그 APK 빌드
./gradlew assembleDebug

# 유닛 테스트 실행
./gradlew testDebugUnitTest
```

### 권한 안내
* **알림 접근 허용 (`NotificationListenerService`)**: 위젯 및 이머시브 플레이어가 음악 앱(Spotify, YouTube Music 등)의 오디오 메타데이터 및 재생 컨트롤 세션을 연동하기 위해 '알림 접근 허용' 권한이 필요합니다. 앱 실행 시 안내 배너를 통해 원클릭으로 시스템 설정 화면으로 이동할 수 있습니다.

---

## 📄 License

이 프로젝트는 [MIT License](LICENSE) 하에 오픈소스로 제공됩니다.

```text
Copyright (c) 2026 windsekirun
```
