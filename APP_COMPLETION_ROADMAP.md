# App Completion Roadmap (Frontend + Backend)

This file is the full checklist to make the app fully working end-to-end.

Current status: UI is mostly ready, backend APIs exist, auth is partly connected.  
Main gap: most buttons/screens still use hardcoded text + `Toast` placeholders instead of real API data/actions.

---

## 1) Must-Fix Setup (Blockers)

- [ ] Keep backend running on LAN and verify from phone:
  - `GET /health` works from app.
- [ ] Keep Firebase project consistent:
  - `app/google-services.json` project == backend `FIREBASE_PROJECT_ID`.
- [ ] Confirm JWT is saved after login and available for protected APIs.

---

## 2) Screen-by-Screen Integration Tasks

## Auth (Sign In / Sign Up)
- [x] Google sign in from app.
- [x] Firebase ID token exchange with backend `/api/auth/google`.
- [ ] Add logout button/menu action (clear token + Firebase sign out + return to sign in).
- [ ] Optional: show signed-in user name/photo from auth response.

## Home Screen (`HomeFragment`)
- [x] Basic backend health check.
- [ ] Replace static welcome text with real user name from backend auth response (or Firebase profile).
- [ ] Remove demo-only static content that should be dynamic.

## Mood Check-in (`MoodCheckinFragment` + `CalendarFragment`)
- [ ] Save mood to backend via `POST /api/moods` (with JWT).
- [ ] Load latest moods via `GET /api/moods` and show at least simple list/state.
- [ ] Replace "coming soon" / "saved successfully" hardcoded toasts with API result.

## Recommendations (`RecommendsFragment`)
- [ ] Fetch recommendations via `GET /api/recommends`.
- [ ] Show recommendation title/category/description in UI.
- [ ] Replace "Finding your perfect match..." toast with real loading + result.

## Community (`CommunityFragment` + `ChatDialogFragment`)
- [ ] Load rooms from `GET /api/community/rooms`.
- [ ] Open selected room and load messages via `GET /api/community/rooms/:roomId/messages`.
- [ ] Send message via `POST /api/community/rooms/:roomId/messages`.
- [ ] Replace all static chat title/description with backend room data.

## Events (`EventsFragment`)
- [ ] Fetch events from `GET /api/events`.
- [ ] Show event date/time and join link.
- [ ] "Join session" button should open link from backend event data.
- [ ] Replace static placeholder toasts.

## Info Hub (`InfoHubFragment`)
- [ ] Fetch resources via `GET /api/resources`.
- [ ] Filter by topic chips using `GET /api/resources?topic=<topic>`.
- [ ] Open selected article URL when card is clicked.

## AI Support (`AiSupportFragment`)
- [ ] Send typed message to `POST /api/support/ai`.
- [ ] Show backend AI reply in chat area (simple list is enough).

## Urgent Help (`MainActivity` urgent actions)
- [ ] Connect urgent action to `POST /api/support/sos`.
- [ ] Show confirmation on success and error toast on fail.

---

## 3) Minimal Technical Improvements Needed (Still Beginner-Friendly)

- [ ] Add one simple `authorizedRequest(...)` helper in `BackendClient` to auto-attach `Authorization: Bearer <token>`.
- [ ] Add 1-2 basic model classes only where needed (optional; can keep parsing JSON directly).
- [ ] Keep one network class (`BackendClient`) instead of many files to stay simple.

---

## 4) Hardcoded/Placeholder Content To Replace

- [ ] Hardcoded welcome name on home screen.
- [ ] All "coming soon", "redirecting", "joining...", "finding match..." placeholder toasts.
- [ ] Static community room labels if backend data is available.
- [ ] Static article/event texts where backend data should render.

---

## 5) Definition of "Complete App"

The app is complete when:

- [ ] User can sign in with Google and stay logged in.
- [ ] Every major button triggers a real backend API call.
- [ ] Every main tab shows backend data instead of dummy placeholders.
- [ ] Errors are shown clearly (both app toast + backend terminal logs).
- [ ] Phone-based testing works on local network.

---

## 6) Suggested Build Order (Fastest Working Path)

1. Mood APIs (save + list)  
2. Recommendations list  
3. Events list + join link  
4. Resources list + topic filter  
5. Community rooms + messages  
6. AI support + SOS  
7. Logout and polish

---

## 7) What I Will Do Next (Implementation Sequence)

- Step 1: connect Mood check-in fully.  
- Step 2: connect Recommendations + Events.  
- Step 3: connect Info Hub filtering.  
- Step 4: connect Community chat APIs.  
- Step 5: connect AI support + SOS.  
- Step 6: cleanup hardcoded text and finalize.
