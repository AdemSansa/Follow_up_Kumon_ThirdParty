# Feature: Google Meet for online check-ins

Online check-ins (meeting type **Video call**) automatically get a Google
Meet link, and the parent receives it by email the day before. Parents
need no Google account or app — they open the link in a browser.

**No paid Google Workspace needed.** A free Gmail account works. The
Google Calendar API and Meet links cost nothing.

## Who hosts the meetings

Two kinds of connected Google accounts, both set up in the app under
**Settings → Google Meet**:

| Account | Connected by | Used for |
| --- | --- | --- |
| **Center account** (shared) | an admin, once | every online check-in by default — all meetings live in this one Google Calendar; staff sign in to it to run meetings |
| **My own account** (optional) | each staff member | that staff member's own check-ins (when they're the assigned staff) |

Rule: the assigned staff member's own account if they connected one (and
it still works); otherwise the center account. To use **one single shared
account** for everything, just connect the center account and nothing else.

## How it works

```text
Follow-up (Video call, open)
   │  48h before (MEETINGS_CREATE_LEAD)
   ▼
Background job → Google Calendar API (as the host account)
   creates an event with a Meet conference on that account's calendar
   → link saved in online_meetings
   │  24h before (MEETINGS_INVITE_LEAD)
   ▼
Background job → email to the parent(s) with the link
   → logged in notifications (never sent twice for the same time)
   │
Rescheduled in the app → Google event moved (same link); parent emailed the new time if they had the old one
Cancelled in the app   → Google event deleted; parent told if they had the link
```

- **Who gets the email:** the parent chosen for the meeting if they have an
  email address; otherwise every parent linked to the student who has one.
  If none has an email, the dashboard says so and nothing is sent.
- **What the email contains:** child's first name, time, duration, staff
  name and the link. Never ratings, concerns or notes (business rule 9).
- **Google's own invite emails are switched off** (`sendUpdates=none`) so
  the parent only gets the app's email at the right time. The parent is
  still added as a guest on the event.
- **The app is the source of truth** (business rule 10). Change times in
  the app, not in Google Calendar — edits made in Google are not read back.
- **Plan edits keep committed check-ins:** an upcoming check-in that already
  has a Meet event or a sent email is not deleted when its plan is edited.
- **Manual controls** on each dashboard card: *Create link now*, *Join
  Google Meet*, *Copy link*, *Send invite now* / *Resend invite*.
- **Stored securely:** only a Google refresh token is kept, encrypted
  (AES-GCM, `TOKEN_ENCRYPTION_KEY`). It is never returned by the API.
  Disconnecting revokes it at Google.

## One-time setup (~10 minutes, free)

Done once by whoever manages the app, with any Google account.

1. **Create a project** at <https://console.cloud.google.com> (e.g.
   "Follow-Up app").
2. **Enable the Google Calendar API** — *APIs & Services → Library →
   Google Calendar API → Enable*.
3. **OAuth consent screen** — *APIs & Services → OAuth consent screen*
   (called *Google Auth Platform → Branding / Audience* in the newer UI):
   - User type **External**; app name, support email.
   - Scopes: add `.../auth/calendar.events` (plus `openid` and `email`).
   - **Publishing status: click "Publish app" (In production).** Leave it in
     *Testing* only for a quick trial: in Testing, only listed test users
     can connect and Google expires the connection after 7 days.
   - Without Google's verification, people connecting see a *"Google hasn't
     verified this app"* screen → **Advanced → Go to … (unsafe)**. That's
     expected for a small internal app (limit: 100 connected users).
     Verification is free but requires a privacy policy and review.
4. **Create the OAuth client** — *Credentials → Create credentials → OAuth
   client ID → Web application*:
   - Authorized redirect URI:
     `http://localhost:8080/api/google/oauth/callback` (dev) — and later your
     real backend URL + `/api/google/oauth/callback`.
   - Copy the **Client ID** and **Client secret**.
5. **Configure the backend** (`.env` or environment), then restart it:

   ```bash
   GOOGLE_CLIENT_ID=1234-abc.apps.googleusercontent.com
   GOOGLE_CLIENT_SECRET=GOCSPX-...
   GOOGLE_REDIRECT_URI=http://localhost:8080/api/google/oauth/callback
   FRONTEND_URL=http://localhost:4200
   TOKEN_ENCRYPTION_KEY=<long random string>
   APP_TIMEZONE=Europe/Paris          # your center's time zone
   MEETINGS_AUTOMATION_ENABLED=true
   ```

6. **Connect the center account** — log in as an admin → **Settings** →
   *Connect Google account* → choose the shared Gmail → allow calendar access.
7. **Email for real parents** — in dev, emails go to Mailpit
   (<http://localhost:8025>) and never leave your machine. For real
   delivery from a Gmail account, turn on 2-Step Verification on it,
   create an **app password** (<https://myaccount.google.com/apppasswords>),
   then:

   ```bash
   MAIL_HOST=smtp.gmail.com
   MAIL_PORT=587
   MAIL_USERNAME=yourcenter@gmail.com
   MAIL_PASSWORD=<16-character app password>
   MAIL_SMTP_AUTH=true
   MAIL_STARTTLS=true
   MAIL_FROM=yourcenter@gmail.com
   CENTER_NAME=Your Center Name
   ```

   Gmail allows about 500 emails a day, plenty for check-in invites.

## Good to know (free Gmail)

- Meetings hosted by a free account are limited to **60 minutes when 3 or
  more people join** (one-to-one calls can run much longer). A 30-minute
  check-in with one parent is well within this.
- Parents who aren't signed in to Google "knock" and the host admits them.
- With a **shared center account**, everyone who runs meetings signs in to
  the same Google account — keep its password and 2-Step Verification
  under the admin's control, and prefer personal accounts if staff should
  not share a login.

## Checking it works

1. Restart the backend; **Settings** shows the Google section without the
   "not set up" warning.
2. Connect the center account (admin).
3. Create a follow-up with meeting type **Video call** within the next 48
   hours, for a student whose parent has an email.
4. On the dashboard (day of that check-in), click **Create link now** → a
   **Join Google Meet** button appears and the event shows in that Google
   Calendar.
5. Click **Send invite now** → the email appears in Mailpit (dev) or the
   parent's inbox.

## Troubleshooting (shown in Settings or under the dashboard card)

| Message contains | Meaning / fix |
| --- | --- |
| `not set up on the server` | `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` missing — step 5. |
| `redirect_uri_mismatch` (on Google's page) | `GOOGLE_REDIRECT_URI` isn't exactly listed in the OAuth client — step 4. |
| `No Google account is connected` | Connect the center account in Settings (step 6). |
| `stopped working … reconnect it` | Access was revoked, the password changed, or the app was still in *Testing* (7-day expiry). Click **Reconnect** in Settings. |
| `Calendar API has not been used` | Step 2 not done in the same project as the OAuth client. |
| `Email is not configured` / connection refused | Mail server not reachable — start Mailpit (`docker compose up -d`) or fix `MAIL_*`. |

Failed links are retried by the job up to 5 times; *Create link now* always
retries immediately.

## Not built yet

- Sending by SMS / WhatsApp (Phase 3). Staff can copy the link meanwhile.
- Reading back changes made directly in Google Calendar.
