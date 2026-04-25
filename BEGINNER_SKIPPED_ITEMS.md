# Beginner Skipped Items

This file tracks things we are intentionally skipping to keep code simple and file count low.

## Skipped for now

- Global API layer with Retrofit interfaces for every endpoint
- Repository pattern / MVVM architecture split
- Proper error categories, retries, and offline handling
- Token storage and auto-refresh logic
- Unit tests and instrumentation tests for network layer
- CI/CD and deployment setup

## Already existing but optional in backend

- `backend/src/scripts/initDb.js`
- `backend/src/scripts/seedDb.js`
- `backend/sql/001_init.sql`
- `backend/sql/002_seed.sql`

You can keep these for convenience, but they are not mandatory for basic API connection.

## Mandatory manual setup (you do once)

- Add `app/google-services.json` from your Firebase project.
- In Firebase Console, enable Google sign-in provider.
- In backend `.env`, set `FIREBASE_PROJECT_ID` and `FIREBASE_SERVICE_ACCOUNT_PATH`.
