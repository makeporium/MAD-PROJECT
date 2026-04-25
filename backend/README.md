# MAD Backend (Node + Express + MySQL + Firebase Auth)

## 1) Setup
1. Copy `.env.example` to `.env` and fill values.
2. Put Firebase service account JSON file at path in `FIREBASE_SERVICE_ACCOUNT_PATH`.
3. Make sure MySQL is running locally.
4. Install dependencies:
   - `npm install`

## 2) Initialize database
- `npm run db:init`
- `npm run db:seed`

## 3) Run backend
- Dev: `npm run dev`
- Prod: `npm start`

Server starts at `http://localhost:5000` by default.

## 4) API overview
- `GET /health`
- `POST /api/auth/google` (Firebase ID token exchange)
- `GET|POST /api/moods`
- `GET /api/recommends`
- `GET /api/community/rooms`
- `GET|POST /api/community/rooms/:roomId/messages`
- `GET /api/events`
- `GET /api/resources?topic=PPD`
- `GET|POST /api/reminders`
- `POST /api/support/ai`
- `POST /api/support/sos`

All `/api/*` routes except auth require `Authorization: Bearer <accessToken>`.
