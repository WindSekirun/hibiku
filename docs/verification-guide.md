# hibiku 위젯 동기화 검증 가이드 및 테스트 시나리오

이 문서는 **hibiku** 음악 위젯의 실시간 미디어 동기화 파이프라인을 실단말 및 에뮬레이터 환경에서 안정적으로 검증하기 위한 절차, 테스트 시나리오, 자동화 검증 명령어를 정리한 문서입니다.

---

## 1. 동기화 파이프라인 아키텍처

위젯 동기화는 5개 단계(`Phase 1 ~ Phase 5`)로 구성되어 동작합니다:

| 단계 | 명칭 | 동작 주체 | 설명 |
|---|---|---|---|
| **Phase 1** | 액션 트리거 | `ActionCallback` | 위젯 내 재생/일시정지/이전곡/다음곡 버튼 클릭 이벤트 발생 |
| **Phase 2** | 미디어 상태 감지 | `MediaNotificationListenerService` | 활성 세션의 메타데이터/재생상태 변경 감지 및 `MediaPlaybackRepository` 갱신 |
| **Phase 3** | 디바운스 및 큐잉 | `WidgetUpdateHelper` | 메타데이터·재생상태·큐 연속 변경으로 인한 버스트 요청을 120ms 동안 conflate/debounce |
| **Phase 4** | 위젯 렌더링 게시 | `WidgetUpdateHelper` | Glance `compose(options)`로 `RemoteViews`를 인프로세스 직접 생성 후 `AppWidgetManager.updateAppWidget` 즉시 전송 |
| **Phase 5** | 컴포저블 렌더링 | `GlanceAppWidget` | `provideGlance` 및 `MusicWidgetContent`가 최신 곡 정보 및 앨범아트, 컨트롤 비율을 구성 |

---

## 2. 검증 환경 및 사전 준비

1. **단말 연결**: ADB(USB 또는 Wi-Fi 무선 디버깅)로 연결된 실제 Android 단말
2. **알림 접근 권한 허용**:
   ```bash
   adb shell cmd notification allow_listener io.github.windsekirun.hibiku/io.github.windsekirun.hibiku.feature.service.MediaNotificationListenerService
   ```
3. **홈 화면 위젯 배치**:
   * 홈 화면에 `hibiku` 4x2 또는 2x2 위젯이 최소 1개 이상 배치되어 있어야 합니다.
4. **미디어 플레이어 준비**:
   * Apple Music, Spotify, YouTube Music 등 표준 `MediaSession`을 지원하는 음악 앱에서 곡 재생 시작.

---

## 3. 핵심 검증 시나리오

### 시나리오 1. 상태바 알림창 미디어 컨트롤 조작 (단일 곡 넘김)
* **목적**: 사용자가 상태바를 내리고 알림 패널 내 미디어 플레이어 카드의 "다음 곡(Next)" 버튼을 1회 눌렀을 때, 곡이 정확히 1곡 넘어가고 홈 화면 위젯에 200ms 내외로 즉시 반영되는지 검증.
* **절차**:
  1. 현재 재생 곡 및 홈 화면 위젯 렌더링 상태 확인.
  2. 상태바 확장 (`adb shell cmd statusbar expand-notifications`).
  3. One UI 퀵패널 미디어 카드의 "다음 곡" 버튼 단 1회 탭 (`adb shell input tap 960 830` ※ 해상도 1080x2520 기준).
  4. 1~2초 대기 후 상태바 닫기 (`adb shell cmd statusbar collapse`).
  5. 홈 화면 위젯이 단 1곡만 정확히 변경되었는지 확인.
* **기대 결과**:
  * 2곡 이상 연속 스킵 없이 정확히 1곡만 전환됨.
  * WorkManager 스케줄링 대기 없이 즉각(0.2초 이내) 홈 위젯의 곡명/아티스트가 변경됨.
  * 컨트롤 버튼 및 요소들의 크기가 축소되지 않고 정상 비율을 유지함.

---

### 시나리오 2. 홈 화면 위젯 내 컨트롤 직접 클릭
* **목적**: 홈 화면의 `hibiku` 위젯 자체 컨트롤(이전곡, 재생/일시정지, 다음곡)을 눌렀을 때 파이프라인 전 구간이 정상 동작하는지 검증.
* **절차**:
  1. 홈 화면 위젯의 다음 곡 버튼 좌표 터치 (UI Automator 또는 좌표 탭).
  2. 미디어 앱으로 전송된 스킵 명령 실행 및 메타데이터 변경 감지.
  3. 위젯 렌더링 갱신 확인.
* **기대 결과**:
  * `[WIDGET_PIPELINE] Phase 1: Clicked NextActionCallback` 로그 출력.
  * 미디어 앱 곡 전환 ➔ `Phase 2` ➔ `Phase 3` ➔ `Phase 4` ➔ `Phase 5` 순차 완료.

---

### 시나리오 3. 외부 하드웨어 / 미디어 키 이벤트 넘김
* **목적**: 블루투스 이어폰, 헤드셋 버튼, 키보드 단축키 등 외부 미디어 키 이벤트 수신 시 위젯 동기화 검증.
* **절차**:
  1. ADB 명령으로 미디어 키 이벤트 전송:
     ```bash
     # 다음 곡
     adb shell input keyevent 87
     # 일시정지 / 재생 토글
     adb shell input keyevent 85
     ```
  2. 홈 화면 위젯 반영 상태 확인.
* **기대 결과**:
  * 외부 이벤트로 곡이 변경되어도 `MediaNotificationListenerService`가 즉시 감지하여 위젯 동기화 완료.

---

### 시나리오 4. 음악 앱 자체 UI에서 곡 변경
* **목적**: 사용자가 Apple Music이나 Spotify 앱 화면에서 다른 곡을 직접 탭하여 재생했을 때 백그라운드 위젯 동기화 검증.
* **절차**:
  1. 음악 앱 화면에서 임의의 다른 곡 재생.
  2. 홈 화면으로 복귀 (`adb shell input keyevent 3`).
  3. 위젯 표시 정보 확인.
* **기대 결과**:
  * 홈 화면 복귀 시 이미 변경된 곡 정보가 즉시 표출되어 있음.

---

### 시나리오 5. 이머시브 플레이어 진입 및 복귀
* **목적**: 위젯 우상단 확장 버튼을 눌러 스탠바이 이머시브 플레이어에 진입하거나 빠져나올 때 강제 동기화 검증.
* **절차**:
  1. 위젯 우상단 확장 버튼 클릭으로 `ImmersivePlayerActivity` 실행.
  2. 이머시브 화면 내에서 곡 넘김 조작.
  3. 뒤로가기로 홈 화면 복귀.
* **기대 결과**:
  * 액티비티 라이프사이클(`onResume`) 및 런치 시 강제 동기화(`force=true`)로 홈 화면 위젯과 이머시브 화면 간 상태 일치.

---

## 4. 실단말 자동화 검증 명령어 모음

### 1) 실시간 파이프라인 로그 모니터링
```bash
adb logcat -v time -s MediaNotificationListener:V WidgetUpdateHelper:V WIDGET_PIPELINE:V MusicWidget4x2:V
```

### 2) 현재 시스템 미디어 세션 상태 확인
```bash
adb shell dumpsys media_session | grep -E "(package|description|state=PlaybackState)"
```

### 3) 상태바 조작
```bash
# 상태바 알림창 펼치기
adb shell cmd statusbar expand-notifications

# 상태바 닫기
adb shell cmd statusbar collapse
```

### 4) 홈 화면 위젯 렌더링 OCR 검증 (CLI 자동화)
스크린샷 이미지를 직접 뷰어로 열지 않고 Tesseract OCR을 통해 텍스트만 추출하여 안전하게 검증합니다:

```bash
# 1. 홈 화면 캡처
adb shell "screencap -p /sdcard/verify.png" && adb pull /sdcard/verify.png /private/tmp/verify.png

# 2. 4x2 위젯 영역 크롭 (1080x2520 해상도 기준 X:70~1010, Y:161~667)
sips -c 510 940 --cropOffset 200 0 /private/tmp/verify.png --out /private/tmp/widget_crop.png

# 3. 텍스트 추출 및 확인
tesseract /private/tmp/widget_crop.png stdout
```

---

## 5. 알려진 주의사항 및 팁

1. **대용량 이미지 로드 방지**:
   * AI 에이전트 세션 내에서 700KB 이상의 고해상도 단말 스크린샷 PNG를 `view_file` 도구로 직접 읽을 경우 모델 처리 오류(빈 응답)가 발생할 수 있습니다. 화면 검증 시에는 위 4절의 OCR 명령어 또는 픽셀 슬라이스 스크립트를 사용해야 합니다.
2. **WorkManager 지연 방지**:
   * Glance의 기본 `updateAll()`은 백그라운드에서 WorkManager로 위임되어 최대 수십 초 지연될 수 있습니다. 프로덕션 코드에서는 반드시 `widget.compose(context, glanceId, options)` + `appWidgetManager.updateAppWidget()` 조합을 사용하여 즉시 IPC 전송하도록 유지해야 합니다.
3. **위젯 옵션(Options) 전달 필수**:
   * `widget.compose` 호출 시 `options` 인자에 `appWidgetManager.getAppWidgetOptions(id)`를 전달하지 않으면 위젯 크기가 기본 최솟값으로 처리되어 버튼과 폰트가 비정상적으로 축소됩니다. 항상 런처의 실제 크기 옵션을 함께 전달해야 합니다.
