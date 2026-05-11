# WebSocket (STOMP) Test Guide for Postman

Postman의 WebSocket 기능을 사용하여 출석 실시간 업데이트를 테스트하는 방법입니다.

## 1. 연결 설정
1. Postman에서 **New > WebSocket** 선택
2. URL 입력:  배포 서버 -`wss://api.pw3hub.xyz/ws-attendance/websocket`,  로컬 - `ws://localhost:8080/ws-attendance/websocket`
3. **Connect** 클릭

## 2. STOMP 핸드셰이크 (CONNECT)
연결 후, 아래 내용을 **Message** 탭에 붙여넣고 **Send**를 누르세요.
(맨 아래 `^@` 문자는 STOMP의 종료 문자인 NULL을 의미합니다.)

```text
CONNECT
accept-version:1.1,1.2
heart-beat:10000,10000

^@
```

```base64
Q09OTkVDVAphY2NlcHQtdmVyc2lvbjoxLjEsMS4yCmhlYXJ0LWJlYXQ6MTAwMDAsMTAwMDAKCgA=
```

서버로부터 `CONNECTED` 메시지가 오면 성공입니다.

## 3. 채널 구독 (SUBSCRIBE)
실시간 출석 정보를 받기 위해 아래 내용을 전송하세요.

```text
SUBSCRIBE
id:sub-0
destination:/topic/attendance

^@
```


```base64
U1VCU0NSSUJFCmlkOnN1Yi0wCmRlc3RpbmF0aW9uOi90b3BpYy9hdHRlbmRhbmNlCgoA
```
이제 서버가 대기 상태가 되었습니다.

## 4. 테스트 방법
1. 기존의 출석 체크 API (예: `POST /api/attendances/scan`)를 호출합니다.
2. Postman WebSocket 창에 실시간으로 JSON 데이터가 들어오는지 확인합니다.

---

### 주의사항
* **NULL 문자**: 만약 `^@`로 전송했을 때 서버에서 응답이 없다면, Postman 메시지 설정에서 `Text` 대신 `Binary` 혹은 전용 STOMP 툴을 사용해야 할 수도 있습니다. 하지만 대부분의 최신 Postman은 `^@` 혹은 단순히 전송을 눌렀을 때의 처리를 지원합니다.
* **주소**: 반드시 끝에 `/websocket`을 붙여야 합니다. (SockJS 우회용)
