
# UI Inspector

[English](README.md)

**UI Inspector**는 안드로이드 개발 중 UI 레이아웃 요소를 시각적으로 검사할 수 있는 디버깅 오버레이 라이브러리입니다.

이 저장소는 [uhufor/inspector](https://github.com/uhufor/inspector)를 포크해 Figma 등 디자인 명세와 실행 중인 Android UI를 비교하기 위한 기능을 추가했습니다. 이미 Android 라이브러리이므로 별도로 라이브러리화할 필요는 없습니다. 다른 앱에 연결하는 방법은 [이 포크 사용하기](#이-포크-사용하기)를 확인하세요. 원본 Maven 의존성만 추가하면 **이번 포크의 변경은 반영되지 않습니다.**

### 이번 포크에서 추가한 기능

- **PROP의 Compose 스타일**: 텍스트, 글자색·배경색의 `#AARRGGBB`와 미리보기, 원본 `sp` 글자 크기, 읽을 수 있는 굵기·이탤릭·폰트 family·줄 높이·자간·정렬.
- **디자인 확인**: 읽을 수 있는 모서리 반경·테두리·로컬 레이어 alpha, 텍스트 레이아웃 영역 및 선택 가능한 baseline/경계 가이드. 읽을 수 없거나 혼합된 값은 **확인 불가**로 표시하고, 그라데이션·이미지를 단색으로 추측하지 않습니다.
- **PROP 표시 조절**: 네 모서리 드래그로 크기 조절, 섹션 제목 클릭으로 접기/펼치기. 크기와 접힘 상태는 앱 재실행 후에도 유지됩니다. 긴 값은 좌우 드래그로 읽습니다.
- **실행 개선**: 중복 창 갱신 감소, 최종 선택 후보만 스타일 추출, 늦게 설치해도 이미 열린 Activity를 찾아 첫 ON 처리.

기존 View 편집 기능은 유지하며 Compose는 읽기 전용입니다. 상세 지원 범위와 검증 결과는 아래 [Compose 스타일 검사](#compose-스타일-검사)에 정리되어 있습니다.

이 프로젝트는 현재는 종료된 **Window VQA** 앱의 UI 검사(측정) 기능에서 영감을 받았습니다.

**디버그 빌드 전용**으로 사용하며, **no-op 모듈**을 연결한 릴리즈 빌드에는 검사 오버레이와 스타일 추출기가 포함되지 않습니다.

| 스크린샷 1                              | 스크린샷 2                              | 스크린샷 3                              | 스크린샷 4                              |
| --------------------------------------- | :-------------------------------------- | :-------------------------------------- | :-------------------------------------- |
| <img src="./art/art_0.png" width="180"> | <img src="./art/art_1.png" width="180"> | <img src="./art/art_2.png" width="180"> | <img src="./art/art_3.png" width="180"> |

---

## 주요 기능

### 일반 검사 모드

- 검사 기능을 활성화하면 선택 가능한 UI 요소에 외곽선이 표시됩니다.
- 요소를 탭하면 다음 정보가 표시됩니다:
    - 해당 요소의 **가로 및 세로 크기**
    - **상위 뷰와의 거리** (위, 아래, 왼쪽, 오른쪽)
    - 모든 정보는 화면 위에 오버레이 형태로 나타납니다.

### 상대 검사 모드

- 선택 가능한 요소를 길게 누르면 상대 검사 모드가 활성화됩니다.
- 첫 번째 선택한 요소는 **빨간색**, 두 번째 요소는 **파란색**으로 강조 표시됩니다.
- 두 요소 간의 거리(위, 아래, 왼쪽, 오른쪽)가 각각 표시됩니다.
    - **거리가 0인 방향은 표시되지 않습니다.**

### 뷰 시스템 호환성

- 다음 UI 트리 구조를 모두 지원합니다:
    - 전통적인 `XML` 기반 UI
    - `Jetpack Compose` 기반 UI

---

## Compose 제약사항

**Compose** 환경에서는 **SemanticsNode**를 통해 UI 요소를 추출합니다. 따라서 컴포저블 선언 시 Semantics를 생성하지 않는 노드는 선택 가능한 요소로 표시되지 않습니다. 아래 예시와 같이 SemanticsNode가 생성되도록 코드를 구성해야 합니다:

```kotlin
@Composable
fun Profile(profile: Profile, modifier: Modifier) {
  Box(
    modifier = Modifier
      .semantics {} // 컨테이너 자체 선택이 필요할 때만 추가
      .size(100.dp)
  ) {
      Image(
        painter = painterResource(id = R.drawable.ic_profile_placeholder),
        contentDescription = "profile holder", // 옵션 1
        modifier = Modifier
          // 또는 .semantics {}                 // 옵션 2
          .fillMaxSize(),
        contentScale = ContentScale.Crop
      )
  }
}
```

---

## 권한

이 라이브러리는 오버레이 UI를 표시하기 위해 **"다른 앱 위에 그리기"** 권한이 필요합니다.
먼저 **사용할 앱**의 `src/debug/AndroidManifest.xml`에 아래 권한을 추가합니다.
(debug 전용 매니페스트에 선언하면 상용 버전에 이 권한이 포함되지 않습니다.)

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
  <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
</manifest>
```

앱 설치 후에 이 권한을 UI 요소 검사 수행 전에 앱 권한으로 부여해야 합니다.
(설정 > 애플리케이션 > 당신의 앱 > 다른 앱 위에 표시)

---

## 부가 기능

### 스와이프 제스쳐 네비게이션

측정 모드에서 검사(측정) 대상이 작은 요소일 경우, 스와이프 제스처 네비게이션을 활용하면 더욱 쉽게 선택할 수 있습니다. 검사 기능을 활성화한 후, 아무 UI 요소나 선택합니다. 이후 화면에서 상하좌우 네 방향으로 스와이프 제스처를 입력하면, 선택된 요소가 스와이프 방향에 따라 변경됩니다. 두 가지 타입의 뷰 트리 이동 방식을 지원합니다.

기본 방식은 **Hierarchy** 모드로, 부모, 자식, 형제 노드를 직관적으로 탐색합니다. 예를 들어, 오른쪽으로 스와이프하면 형제 노드를 순차적으로 이동합니다. 가장 마지막 형제 노드에 도달하면, 더 이상 선택이 이동되지 않습니다.

다른 방식은 **DFS(깊이 우선 탐색)** 모드로, 전위 순회를 통해 뷰 트리를 이동합니다. 예를 들어 오른쪽으로 스와이프하면 형제 노드를 탐색하는 것이 아니라, 현재 노드의 자식 노드를 먼저 순회한 뒤 다음 형제 노드로 이동합니다.

|           Hierachy (Default)            |                   DFS                   |                데모 (DFS)                 |
| :-------------------------------------: | :-------------------------------------: | :---------------------------------------: |
| <img src="./art/art_4.png" width="270"> | <img src="./art/art_5.png" width="270"> | <img src="./art/art_m_0.gif" width="270"> |

그림에서는 스와이프 제스처 방향에 따라 트리 이동이 어떻게 이루어지는지 보여줍니다. 예를 들어, DFS 모드에서는 Top → B로의 이동이 GD, GR 두 가지 스와이프 제스처로 가능합니다. **GD**은 Gesture Down의 줄임 표현입니다.

### 상대 가이드 스타일(NONE/EDGE/FULL)

상대 검사 모드에서 가이드라인의 표현 방식을 스타일로 제어할 수 있습니다.

- **NONE**
    - 거리선, 화살표, 라벨만 표시합니다.
- **EDGE**
    - 두 요소의 가장 가까운 면에 짧은 엣지 가이드만 표시하고, 거리선과 라벨도 함께 표시합니다.
    - 요소 간 간격이 너무 좁으면 엣지 가이드는 생략됩니다.
- **FULL**
    - 두 요소의 상대 관계를 선형으로 보여주는 가이드라인을 표시합니다.
    - 두 요소가 완전히 분리된 경우에는 primary는 상대 면까지만, secondary는 전체 외곽까지 표시합니다.
    - 축 중 하나라도 겹치면 양쪽 모두 겹치는 투영 또는 인접 면까지만 표시합니다.
    - 한 요소가 다른 요소 내부에 완전히 포함된 경우, 내부 요소의 가이드라인은 바깥 요소의 면까지 연장됩니다.

|                   None                    |                   Edge                    |                   FULL                    |
| :---------------------------------------: | :---------------------------------------: | :---------------------------------------: |
| <img src="./art/art_m_1.gif" width="270"> | <img src="./art/art_m_2.gif" width="270"> | <img src="./art/art_m_3.gif" width="270"> |

### 상세 보기(Details) 및 편집 기능

선택된 요소의 상세 정보를 플로팅 패널로 확인할 수 있으며, 일부 속성 편집 기능을 제공합니다.

- **지원 범위**
    - 편집 기능은 **Android View 시스템**에 제공됩니다. XML과 코드로 생성한 View 모두 포함합니다.
    - Jetpack Compose 요소의 속성 편집은 지원하지 않습니다.
- **가능한 편집 항목 예시**
    - margin/padding 및 TextView의 텍스트·글자 크기·색상
- **기타**
    - 플로팅 패널의 UI 스케일 조정, 크기 저장 및 섹션별 접기/펼치기가 가능합니다.

|                 상세 보기                 |                   편집                    |                 UI 스케일                 |
| :---------------------------------------: | :---------------------------------------: | :---------------------------------------: |
| <img src="./art/art_m_4.gif" width="270"> | <img src="./art/art_m_5.gif" width="270"> | <img src="./art/art_m_6.gif" width="270"> |

### 설정/토글 API

다음 API를 통해 런타임에서 동작을 제어할 수 있습니다.

- **상대 가이드 스타일**
  ```kotlin
  Inspector.setRelativeGuideStyle(RelativeGuideStyle.EDGE)
  val style = Inspector.getRelativeGuideStyle()
  ```

- **트리 탐색 방식**
  ```kotlin
  Inspector.setTraverseType(TraverseType.HIERARCHICAL)
  Inspector.setTraverseType(TraverseType.DFS)
  ```

- **단위 표시 방식**
  ```kotlin
  Inspector.setUnitMode(UnitMode.DP)
  Inspector.setUnitMode(UnitMode.PX)
  ```

- **상세 보기 패널**
  ```kotlin
  Inspector.enableDetailsView(true)
  Inspector.enableDetailsView(false)
  Inspector.setDetailsViewUiScale(1.0f)
  ```

---

## 다운로드

### 이 포크 사용하기

`inspector-runtime`과 `inspector-noop`은 이미 Android 라이브러리 모듈입니다. 별도 배포 없이 사용하려면 이 저장소를 앱 프로젝트 옆에 체크아웃합니다.

```text
workspace/
  MyApp/
  inspector/    # OverpaidPeople/inspector 체크아웃
```

**사용할 앱의** `settings.gradle.kts`에서 `pluginManagement` 바깥에 아래 [composite build](https://docs.gradle.org/8.11.1/userguide/composite_builds.html) 설정을 추가합니다. 실제 체크아웃 위치에 맞게 경로를 수정하고, 기존 플러그인·저장소 설정은 유지합니다.

```kotlin
includeBuild("../inspector") {
    name = "inspector-fork"
    dependencySubstitution {
        substitute(module("io.github.uhufor:inspector-runtime"))
            .using(project(":inspector-runtime"))
        substitute(module("io.github.uhufor:inspector-noop"))
            .using(project(":inspector-noop"))
    }
}
```

그다음 **앱 모듈의** `build.gradle.kts`에 의존성을 추가합니다.

```kotlin
dependencies {
    debugImplementation("io.github.uhufor:inspector-runtime:SNAPSHOT")
    releaseImplementation("io.github.uhufor:inspector-noop:SNAPSHOT")
}
```

위 치환 설정을 함께 사용하면 이 좌표는 원격 배포본이 아니라 **로컬 포크 소스**로 연결됩니다. 포크 자체의 버전 카탈로그와 전이 의존성도 함께 사용하므로 배포된 `SNAPSHOT` 파일이 필요하지 않습니다. 치환 설정을 유지하고, 검증한 커밋/태그로 체크아웃해 사용하세요. 이미 두 모듈을 같은 Gradle 빌드에 연결했다면 샘플처럼 `debugImplementation(project(":inspector-runtime"))`, `releaseImplementation(project(":inspector-noop"))`을 사용합니다.

저장소의 빌드 기준은 Gradle **8.11.1**, AGP **8.9.2**, Kotlin **2.1.10**, Gradle 실행용 Java **17**, `compileSdk = 35`, `minSdk = 28`입니다. Compose 추출은 UI/Foundation **1.6.8**에서 검증했습니다. 소비 앱의 빌드 도구·SDK도 호환되게 맞춰야 하며, 다른 Compose 버전의 추출 호환성을 보장하지 않습니다.

의존성·초기화 예제는 별도 소비 앱에서도 확인했습니다. Debug/Release 빌드가 통과했고, 생성된 debug APK에는 포크의 검사 클래스가 포함되고 release APK에는 제외되는 것을 확인했습니다.

원격 의존성 주소만으로 배포하려면 포크 소유의 배포 좌표·버전·인증 정보를 먼저 구성해야 합니다. 기존 배포 스크립트는 여전히 원본 `io.github.uhufor` 네임스페이스를 대상으로 하며, 이 저장소에 푸시하는 것만으로 포크 배포본이 생성되지는 않습니다.

### 원본 배포본 (참고)

기존 프로젝트의 Maven Central 의존성은 참고용으로 유지합니다. **위 포크 치환 설정이 없으면 아래 좌표는 이 포크가 아닌 원본 구현을 사용합니다.**

```kotlin
dependencies {
  debugImplementation("io.github.uhufor:inspector-runtime:latest.release")
  releaseImplementation("io.github.uhufor:inspector-noop:latest.release")
}
```

---

## 예제 코드

기존 Application/Activity에 아래 초기화 코드를 합쳐 사용하세요. Compose의 첫 composition 전에 설치하며, 별도 Application을 새로 만들었다면 앱 매니페스트에도 등록해야 합니다. `install()`은 권한이 없을 때 오버레이 권한 설정을 엽니다. 권한을 허용하고 돌아오면 Activity의 `onResume()`에서 설치를 마치고 버튼을 표시합니다.

```kotlin
import android.app.Activity
import android.app.Application
import android.provider.Settings
import com.uhufor.inspector.Inspector

class InspectorApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 사용 전 설치가 필요합니다
        Inspector.install(this)
    }
}

// 검사 기능을 수동으로 제어하고 싶을 경우
class InspectorActivity : Activity() {
    override fun onResume() {
        super.onResume()
        if (!Settings.canDrawOverlays(this)) return
        Inspector.install(this) // 권한 허용 후 재시도하며 현재 Activity도 갱신합니다.
        Inspector.enableDetailsView(true)
        Inspector.showFloatingTrigger()
    }

    override fun onPause() {
        Inspector.disableInspection()
        Inspector.hideFloatingTrigger()
        super.onPause()
    }

    fun enableInspection() {
        Inspector.enableInspection()
    }

    fun disableInspection() {
        Inspector.disableInspection()
    }
}
```

---

## Compose 스타일 검사

플로팅 버튼은 위에서부터 **검사 ON/OFF**, **DP/PX 전환**, **PROP 상세 표시** 순서입니다. DP/PX는 크기·거리 표시를 바꾸며 Compose 글자 크기는 원본 `sp`를 유지합니다. 검사 대상은 라이브러리를 연결한 앱의 View/Compose 트리이며, 버튼 아래의 다른 앱 UI 트리를 읽는 기능은 아닙니다.

앱의 각 `Text`나 컴포넌트를 수정할 필요 없이, 기존 상세 패널에서 다음 정보를 확인할 수 있습니다.

- 실제 텍스트 내용 (`[AnnotatedString(...)]` 형식이 아닌 문자열)
- 글자 색상과 배경색: `#AARRGGBB` 및 투명도를 구분하는 체크무늬 색상 미리보기
- 글자 크기: Compose 원본 `sp` 값 (화면 측정의 DP/PX 설정과 무관)
- 글자 굵기: 읽을 수 있는 경우 숫자 값, 이탤릭 여부

기존 설정에 추가 API나 전용 Modifier는 필요하지 않습니다. **첫 Compose 화면이 생성되기 전에** `Application.onCreate()`에서 설치하세요.

```kotlin
override fun onCreate() {
    super.onCreate()
    Inspector.install(this)
}
// 검사 중 상세 패널 표시
Inspector.enableDetailsView(true)
```

runtime의 `install()`은 Compose의 전역 `isDebugInspectorInfoEnabled`를 켭니다. 이는 이후 생성되는 Modifier의 검사 메타데이터를 보존하며 검사 종료 시에도 유지됩니다. 개별 컴포넌트 변경 없이 `Modifier.background`의 색상/브러시를 구분하기 위한 설정입니다. 이미 생성된 Modifier에는 소급 적용되지 않으므로, 늦게 설치했다면 화면을 다시 생성해야 배경 정보를 얻을 수 있습니다. 메타데이터 보존 비용은 debug runtime에만 발생하며 release의 `inspector-noop`에는 적용되지 않습니다.

Activity가 이미 표시된 뒤 `Application` 컨텍스트로 설치해도 현재 앱 창에서 Activity를 복구하므로, 첫 번째 플로팅 버튼으로 검사를 켜기 위해 백그라운드에 갔다 올 필요가 없습니다. Activity 컨텍스트로 다시 `install()`해도 추적 대상이 갱신되고, 같은 Application에 생명주기 콜백을 중복 등록하지 않습니다. 배경 메타데이터를 얻으려면 여전히 첫 composition 전 설치를 권장합니다.

### 실행 비용

- 검사 시작·새로고침·검사 오버레이 크기 변경 시 현재 View/Compose 트리를 스캔하고 스타일 스냅샷을 보관합니다. 노드 수와 텍스트/Modifier 복잡도에 따라 실행 시간과 메모리 사용량이 증가합니다. 주기적으로 전체 화면을 재스캔하지는 않습니다.
- 요소를 탭할 때는 위치로 최종 후보를 고른 다음 해당 Compose 노드의 스타일만 추출합니다. 중간 후보의 텍스트 레이아웃 콜백은 호출하지 않습니다.
- PROP 값의 자동 흘림 애니메이션을 제거했습니다. 긴 값은 **값 영역을 좌우로 드래그**해서 읽습니다. 사용하지 않는 섹션을 접으면 그 콘텐츠도 composition에서 빠집니다.
- 검사를 끄면 검사 오버레이와 스캔 스냅샷을 해제합니다. 설치 시 켜 둔 Compose 검사 메타데이터는 유지됩니다. `releaseImplementation(project(":inspector-noop"))` 구성에는 이러한 검사 처리가 없습니다.
- Debug 빌드와 에뮬레이터에서 얻은 수치는 실제 기기의 배포 빌드 성능과 다릅니다. 이 검증은 기능 회귀 테스트이며 CPU/FPS 개선율을 측정한 벤치마크는 아닙니다.

### 지원 범위와 확인 불가

- 현재 BOM `2024.08.00`이 해석하는 Compose UI/Foundation **1.6.8**을 기준으로 구현했습니다. 텍스트는 선택한 unmerged Semantics 노드의 `SemanticsActions.GetTextLayoutResult`에서 읽고, 실패/미제공 시 Semantics의 `Text` 또는 `EditableText` 내용만 사용합니다. 실패하는 콜백은 검사를 중단시키지 않습니다. 비밀번호 노드는 원문과 스타일을 조회하지 않습니다.
- 배경은 `layoutInfo.getModifierInfo()`와 `InspectableValue`로 **선택한 노드 자체의 단일 `Modifier.background`**를 읽습니다. 이 버전에 없는 새 배경 Semantics API나 Compose 비공개 필드 reflection에 의존하지 않습니다. 다른 Compose 버전의 호환성을 보장하지 않으며, 검사 메타데이터를 읽지 못하면 확인 불가로 표시합니다.
- 부모 배경을 자식의 배경으로 간주하지 않습니다. 표시하는 색상은 선언된 배경 페인트이며, 부모/창 배경과 합성된 최종 화면 픽셀이나 graphicsLayer의 최종 알파가 아닙니다. 모서리가 있는 배경도 페인트 색상을 표시합니다.
- 그라데이션/ShaderBrush, 여러 background, 이미지 `paint`, 표준 커스텀 draw Modifier, Surface의 내부 드로잉, Canvas 등에서 단색을 확정할 수 없으면 **확인 불가**로 표시합니다. `SolidColor` 브러시는 명시된 alpha를 적용합니다. 일반 커스텀 Modifier의 드로잉 동작은 알아낼 수 없으므로 이 도구를 픽셀 분석기로 사용하지 마세요.
- AnnotatedString의 겹친 SpanStyle을 병합한 뒤, 색상·크기·굵기·이탤릭 각각이 전체 텍스트에서 동일할 때만 값을 표시합니다. 혼합된 항목, 미지정 값, 글자 크기의 `em` 값은 확인 불가입니다. 줄 높이·자간의 `em`은 원본 단위로 표시합니다. 글꼴의 실제 렌더링 결과/합성 굵기가 아닌 레이아웃에 제공된 스타일 값입니다.
- 기존 Semantics 기반 선택 범위는 유지됩니다. Semantics가 없는 순수 Box/Canvas는 새로 선택 대상이 되지 않습니다. 그런 컨테이너 자체를 선택해야 할 때만 해당 컨테이너에 `Modifier.semantics { }`를 추가할 수 있습니다. 표준 Text/TextField에는 필요하지 않습니다. `clearAndSetSemantics`로 숨긴 내부 스타일은 복구하지 않습니다.
- 기존 크기·부모와의 거리·마진·패딩·요소 간 거리 기능 및 XML/View 텍스트 편집을 유지합니다. Compose 스타일 표시는 읽기 전용입니다. XML/View 배경도 그라데이션과 ripple 효과색을 단색 배경으로 오인하지 않도록 처리합니다.

의존성 구성은 그대로 유지하세요.

```kotlin
debugImplementation(project(":inspector-runtime"))
releaseImplementation(project(":inspector-noop"))
```

테스트: `:inspector-runtime:testDebugUnitTest`, `:inspector-runtime:connectedDebugAndroidTest`. 계측 테스트에는 Android API 28 이상 기기/에뮬레이터가 필요합니다.

### PROP 패널 크기 조절

PROP 패널의 **네 모서리에 있는 사선 표시를 누른 채 드래그**하면 너비와 높이를 조절할 수 있습니다. 드래그하는 모서리의 반대쪽은 고정됩니다. 패널 내부는 기존처럼 스크롤하고, XML/View 편집과 가이드 토글도 그대로 사용할 수 있습니다.

- 손을 떼면 마지막 크기를 앱 내부 설정에 dp로 저장합니다. PROP를 닫았다 열거나 앱 프로세스를 다시 실행해도 복원됩니다. 앱 데이터 삭제 또는 앱 삭제 후 재설치 시 초기화됩니다.
- 기본 카드 크기는 기존 `148 × 180dp`이며 기존 UI 배율을 반영합니다. 최소 크기는 UI 배율을 반영한 `120 × 120dp`, 최대 크기는 사용 가능한 화면 영역입니다. 사용자가 조절한 크기는 글자 UI 배율과 별도로 저장합니다.
- 회전이나 작은 화면에서는 화면 안에 맞게 일시적으로 크기를 제한합니다. 자동 제한으로 저장된 선호 크기를 덮어쓰지 않으므로, 넓은 화면으로 돌아오면 복원됩니다.
- 취소된 드래그는 시작 크기로 돌아가며 저장하지 않습니다. 패널 위치는 기존 플로팅 버튼에 붙는 방식을 유지하고, 크기만 기억합니다.
- 추가 권한이나 설정은 필요하지 않으며 release의 `inspector-noop` 구성도 유지됩니다.
- 드래그 중 창 갱신은 화면 프레임마다 최신 크기 한 번으로 합치고, 동일한 위치·크기의 중복 요청은 건너뜁니다. 손을 떼면 대기 중인 마지막 크기를 반영한 뒤 저장합니다.

### PROP 섹션 접기/펼치기

**Details, Distance, Margin / Padding, Text, Actions, Appearance, Alignment** 제목을 누르면 해당 섹션 내용만 접거나 펼칠 수 있습니다. 접어도 제목과 화살표는 남으므로 다시 누르면 복원됩니다. 처음에는 모든 섹션이 펼쳐져 있습니다.

- 섹션들은 서로 독립적입니다. 예를 들어 Details를 접으면 ID 정보만 숨기며 Distance 등 다른 섹션은 유지합니다. 선택 요소에 해당하지 않는 섹션은 기존처럼 표시하지 않습니다.
- 접힘 상태는 선택 요소·단위·패널 크기를 바꿔도 유지되고, PROP를 다시 열거나 앱을 다시 실행해도 복원됩니다. 크기 설정과 함께 앱 내부에 저장하며, 기존에 저장한 패널 크기는 바꾸지 않습니다.
- XML/View의 **Edit** 버튼은 제목과 별도로 동작합니다. 접힌 섹션에서는 Edit도 숨기고, 펼치면 다시 사용할 수 있습니다. 편집 후 돌아와도 다른 섹션의 접힘 상태를 유지합니다.
- 접기는 PROP 안의 정보 표시만 변경합니다. 실제 측정과 이미 켜 둔 베이스라인/텍스트 경계 가이드에는 영향을 주지 않습니다.

### 앱에서 바로 확인하는 디자인 속성

별도 디자인 파일이나 폰트 파일을 가져올 필요 없이, 기존 PROP 상세 패널을 스크롤해 다음 정보를 확인합니다.

- **Text**: 기존 글자색·크기·굵기에 폰트 family, 줄 높이, 자간, 텍스트 정렬, font padding 여부를 추가했습니다. Compose 줄 높이·자간은 원본 `sp`/`em`을 유지합니다. View 줄 높이는 런타임 측정 `px`, 자간은 `em`입니다. 미지정 값이나 혼합된 값은 확인 불가입니다.
- **Appearance**: 모서리 반경, 테두리 두께·색상 및 미리보기, 로컬 레이어 alpha를 표시합니다. 모서리는 Shape의 실제 크기와 LTR/RTL 방향을 반영한 TL/TR/BR/BL 순서입니다. 반경은 DP/PX 설정에 따르고, 테두리는 선언된 dp를 표시합니다. `0.dp` 테두리는 `Hairline (1px)`로 구분합니다.
- **Alignment**: 텍스트 레이아웃 너비·높이·줄 수와 첫 줄/마지막 줄 baseline을 표시합니다. Baseline 수치는 **텍스트 레이아웃의 맨 위**에서 측정한 거리입니다. `Baseline guide`를 켜면 자홍색 선, `Text bounds guide`를 켜면 노란색 경계를 그립니다. 가이드는 기본 Off이며 선택 요소를 바꿔도 설정을 유지합니다. 레이아웃을 읽지 못하면 토글도 사용할 수 없습니다.
- Compose 패딩·마진은 현재 자동 추출하지 못하므로 0 대신 **확인 불가**로 표시합니다. 실제 부모/요소 간 거리 측정은 그대로 사용할 수 있습니다.

지원 한계:

- 폰트 family는 레이아웃에 지정된 시스템 family나 앱에 포함된 font 리소스 이름입니다. 실제 glyph별 fallback 폰트 이름을 추측하지 않습니다. View Typeface의 family 이름, 동적/로딩 폰트 이름은 확인 불가일 수 있습니다.
- 레이어 alpha는 읽을 수 있는 **선택 노드의 명시적 graphicsLayer** 값들의 곱입니다. 부모 alpha, 배경색 자체의 alpha, 최종 합성 픽셀과 구분합니다. 레이어가 없거나 `graphicsLayer { ... }` 콜백에 값이 숨겨져 있으면 확인 불가입니다. 앱의 콜백은 검사 목적으로 실행하지 않습니다.
- 복수 테두리의 너비·색상, 그라데이션 테두리의 단일 색상, 서로 다른 Shape가 겹친 경우의 단일 반경, 일반 Path/비대칭 타원 반경은 확인 불가입니다. View에서는 공개 API로 읽을 수 있는 배경 모서리와 view alpha를 표시하며, drawable 테두리는 확인 불가입니다.
- 노란 경계는 **텍스트 레이아웃 영역**이며 글자의 실제 잉크 픽셀 윤곽은 아닙니다. Compose 1.6.8에서 공개된 첫 줄/마지막 줄 baseline만 표시하고 중간 줄을 보간하지 않습니다. 장식 컨테이너와 내부 텍스트 크기가 다른 입력창은 텍스트 원점을 확정할 수 없어 가이드를 표시하지 않습니다. View 가이드는 변환되지 않은 일반 TextView에서 지원하며, 회전/크기 변환이 있으면 좌표를 추측하지 않습니다.
- 검사 정보는 선택/스캔 시점의 값입니다. 화면이나 상태가 바뀌었다면 새로 선택하거나 새로고침해 확인하세요.

샘플 앱에서 **Compose UI → Background color samples**의 파란 텍스트를 선택하면 `16sp`, `24sp` 줄 높이, `0.5sp` 자간, `sans-serif`, Center 정렬, `8dp` 모서리, `2dp / #FF0D47A1` 테두리, `90%` 레이어 alpha를 비교할 수 있습니다. 가이드 토글은 Alignment 아래에 있습니다.

### 이번 변경의 검증 결과

- 단위 테스트 **35개 통과**: 스타일 추출·가이드 좌표, 네 모서리 리사이즈·화면 제한·dp 크기 복원, 섹션별 독립 접힘 상태, 연속 창 갱신 요청 병합·최종 반영·취소를 검증했습니다.
- 계측 테스트 **28개 모두 실행 통과** (`:inspector-runtime:connectedDebugAndroidTest`, Pixel 9 Pro / Android 16 에뮬레이터): 기존 스타일·가이드·측정 회귀, 실제 오버레이 창 리사이즈·재생성, 모든 섹션의 제목 유지와 콘텐츠 숨김, 선택/단위 변경 시 접힘 유지, Edit 버튼 분리, 섹션 상태 저장·크기 설정 보존을 검증했습니다. 실제 PROP 창에서 7개 섹션을 총 42번 접고 펼친 뒤에도 창 연결과 크기가 유지됐습니다. 늦은 Application 설치 후 첫 ON, Activity 재등록·정리, 최종 선택 후보만 레이아웃 조회, 긴 값의 자동 이동 방지와 수동 스크롤도 검증했습니다. 오버레이 권한 선언은 계측 테스트 APK에만 추가했습니다.
- 실제 샘플의 PROP 창을 물리 드래그로 `820 × 1114 → 880 × 1204 → 820 × 1114px`로 늘리고 줄이는 동작을 확인했습니다. 샘플 앱 프로세스를 종료·재실행한 뒤에도 저장된 dp 크기에 맞는 `820 × 1114px` 창이 복원됐습니다.
- `:app:assembleDebug`, `:app:assembleRelease`, `:inspector-runtime:assembleRelease`, `:inspector-noop:assembleRelease` 통과.
- `:inspector-runtime:lintDebug` 통과: 오류 0개, 기존 `ic_compose.xml`의 긴 벡터 경로 경고 1개.
- APK DEX 확인: debug에는 `ComposeStyleExtractor`, `ComposeAppearanceExtractor`, `TextGuideOptions`가 있고 release에는 없습니다. 샘플의 기존 runtime 아이콘 참조 두 곳은 샘플 소유 리소스로 옮겨 no-op release 빌드 오류를 수정했습니다. 최종 debug 앱을 에뮬레이터에 설치했습니다.

추출 API 근거: [GetTextLayoutResult](https://developer.android.com/reference/kotlin/androidx/compose/ui/semantics/SemanticsActions), [LayoutInfo.getModifierInfo](https://developer.android.com/reference/kotlin/androidx/compose/ui/layout/LayoutInfo). 사용 버전의 Background 구현은 로컬에 해석된 Foundation 1.6.8 source JAR에서도 확인했습니다.

## 라이선스

```
Copyright [2025] [uhufor (김해중)]

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
