# Family Safety - Parental Control App

Transparent parental monitoring system: **backend (MongoDB)** + **child Android app** + **parent Android app**.

⚠️ **Important - Please read:** This is built as a *transparent* monitoring tool. The child device always shows:
- A permanent notification while monitoring is active
- Android's own screen-capture notification while screen sharing is on
- A visible app icon and setup screen explaining what's being shared

This is intentional and cannot be removed — Android enforces these indicators at the OS level for any app, and hiding them would turn this into illegal spyware. See the conversation this was built from for more context.

---

## What's included

```
backend/              → Node.js + Express + MongoDB API (deploy free on Render/Railway)
android-child-app/    → Installed on the child's phone (Kotlin, Android Studio project)
android-parent-app/   → Installed on your (parent's) phone (Kotlin, Android Studio project)
.github/workflows/    → GitHub Actions - auto-builds both APKs when you push to GitHub
```

## Features implemented

| Feature | Status |
|---|---|
| Location tracking (live + history) | ✅ Working |
| App usage / screen time reports | ✅ Working |
| New app install alerts | ✅ Working |
| SOS emergency button | ✅ Working |
| Low battery alerts | ✅ Working |
| Screen snapshot sharing (AirDroid-style) | ✅ Working (periodic snapshots, not full video) |
| Real-time push via Socket.io | ✅ Working |
| Web content filtering | ⬜ Not included yet (needs VPN service - can add later) |
| App blocking by schedule | ⬜ Not included yet (needs Accessibility Service - can add later) |

---

## Step 1: Set up MongoDB (free, lifetime)

1. Go to https://www.mongodb.com/cloud/atlas/register and create a free account
2. Create a free **M0 cluster** (this tier is free forever, no credit card needed for M0)
3. Create a database user (username + password)
4. Under Network Access, allow access from anywhere (`0.0.0.0/0`) for simplicity
5. Click "Connect" → "Drivers" → copy your connection string, it looks like:
   ```
   mongodb+srv://<username>:<password>@cluster0.xxxxx.mongodb.net/parental_control?retryWrites=true&w=majority
   ```

## Step 2: Deploy the backend (free)

Easiest option: **Render.com** free tier.

1. Push this whole folder to a new GitHub repo (see Step 4 below)
2. Go to https://render.com → New → Web Service → connect your GitHub repo
3. Set:
   - Root directory: `backend`
   - Build command: `npm install`
   - Start command: `npm start`
4. Add environment variables (from `backend/.env.example`):
   - `MONGODB_URI` → your connection string from Step 1
   - `JWT_SECRET` → any long random string
5. Deploy. You'll get a URL like `https://your-app.onrender.com`

> Note: Render's free tier sleeps after inactivity and takes ~30s to wake up on the first request. Fine for personal use.

## Step 3: Update the app URLs

Before building the APKs, update the backend URL in **both** apps:

- `android-child-app/app/src/main/java/com/pcontrol/child/network/ApiClient.kt` → change `BASE_URL`
- `android-parent-app/app/src/main/java/com/pcontrol/parent/network/ApiClient.kt` → change `BASE_URL`

Change from `http://10.0.2.2:5000` to your real Render URL, e.g. `https://your-app.onrender.com`.

## Step 4: Push to GitHub & auto-build APKs

```bash
cd parental-control-app
git init
git add .
git commit -m "Initial commit"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/YOUR_REPO.git
git push -u origin main
```

Once pushed, go to your repo → **Actions** tab. The `Build APKs` workflow runs automatically and produces two downloadable APKs:
- `child-app-debug-apk`
- `parent-app-debug-apk`

Download them from the workflow run's **Artifacts** section, then transfer to the respective phones and install (you'll need to allow "install from unknown sources" since these aren't from Play Store).

## Step 5: Set up devices

1. Install the **parent app** on your phone → Sign Up → Log In
2. Tap "+ Add New Device" → enter a name → get a pairing code
3. Install the **child app** on the child's phone
4. Open it, read the on-screen explanation, enter the pairing code
5. Grant permissions one by one as prompted (location, usage access, call log)
6. Monitoring starts — child will see a persistent notification confirming this

---

## Extending this project

- **Web filtering**: implement using Android's `VpnService` API to route DNS through a filtering service
- **App blocking by schedule**: use `AccessibilityService` to detect foreground app and show a block overlay
- **Full live video** (not just snapshots): upgrade `ScreenCaptureService` to stream via WebRTC + a signaling server (e.g. using `socket.io` you already have in the backend)
- **Push notifications**: add Firebase Cloud Messaging so the parent app gets alerts even when closed

## Legal note

Before deploying this for real use, check your local laws regarding monitoring apps, especially for older minors — many regions require the monitored person to be informed, which this app already does by design. Consult local regulations if unsure.
