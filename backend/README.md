# Backend

Node.js + Express + MongoDB API for the parental control apps.

## Local development

```bash
cd backend
npm install
cp .env.example .env
# edit .env with your MongoDB URI and JWT secret
npm run dev
```

Server runs at `http://localhost:5000`.

## API Endpoints

| Method | Path | Auth | Purpose |
|---|---|---|---|
| POST | /api/auth/signup | No | Parent creates account |
| POST | /api/auth/login | No | Parent logs in |
| POST | /api/device/create-pairing | Yes (parent) | Generate pairing code |
| POST | /api/device/pair | No | Child app pairs using code |
| GET | /api/device/list | Yes (parent) | List linked devices |
| POST | /api/device/:id/heartbeat | No | Child app heartbeat + battery |
| POST | /api/location/:id | No | Child app posts GPS ping |
| GET | /api/location/:id/latest | Yes (parent) | Get latest location |
| GET | /api/location/:id/history | Yes (parent) | Get location history |
| POST | /api/usage/:id | No | Child app posts app usage |
| GET | /api/usage/:id/:date | Yes (parent) | Get usage for a date |
| POST | /api/alerts/:id | No | Child app posts an alert |
| GET | /api/alerts/:id | Yes (parent) | Get alerts |
| POST | /api/screen/:id | No | Child app uploads screenshot |
| GET | /api/screen/:id/latest | Yes (parent) | Get latest screenshot |
| POST | /api/screen/:id/request | Yes (parent) | Ask child to send screenshot |

"No" auth endpoints are called by the child app using its `deviceId` (not a login) since the child app doesn't have its own account.
