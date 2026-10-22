# Reels upload — debug logs

## Android (phone)

**Log tag:** `IndieReelUpload`

With the device connected:

```bash
adb logcat -c
adb logcat -s IndieReelUpload
```

Then post a video from the app. You should see lines like:

- `upload-config strategy=PRESIGNED_S3 bucket=indiestate-reels-uat …`
- `presign ok videoId=… objectKey=raw/…`
- `S3 PUT start contentType=video/mp4 contentLength=…`
- On failure: `S3 PUT HTTP 403 body=…` with AWS `<Code>` / `<Message>` when S3 returns XML

### API vs S3 (important)

```bash
adb reverse tcp:8080 tcp:8080
```

This only tunnels **your backend** (`localhost:8080`). **Presigned uploads still go from the phone directly to** `*.s3.ap-south-1.amazonaws.com` on the **public internet**.

If Logcat or the app shows **`Unable to resolve host … amazonaws.com`**:

- The phone has no working DNS/internet to AWS (not an IAM 403).
- Fix: Wi‑Fi or mobile data with internet, disable broken VPN/private DNS, or test on another network.

Check from the device:

```bash
adb shell ping -c 2 indiestate-reels-uat.s3.ap-south-1.amazonaws.com
```

If ping fails, fix connectivity before debugging IAM.

## Backend (Spring Boot)

**Logger name:** `reel-upload` (not a Logcat tag)

Run backend in a terminal and filter:

```bash
./start-backend.sh 2>&1 | grep reel-upload
```

On **startup**, look for:

- `startup headBucket OK bucket=indiestate-reels-uat …`
- `startup putObject OK …` **or** `startup putObject FAILED …`

On **upload** (when the app posts):

- `createUpload userId=… videoId=… objectKey=raw/…`
- `presigned PUT bucket=… key=raw/…`
- After complete: `complete … s3ObjectExists=true` (if false, the phone PUT never landed in S3)

Default log level for this logger is **DEBUG** in `application.yml` (`logging.level.reel-upload: DEBUG`).

## InvalidAccessKeyId (your current error)

Logcat:

```text
S3 InvalidAccessKeyId: The AWS Access Key Id you provided does not exist in our records.
```

The **backend** signs presigned URLs with `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` from `.env`. S3 is rejecting that key (rotated, deleted, typo, or wrong AWS account).

**Fix:**

1. AWS Console → IAM → Users → your reels user → **Security credentials** → **Create access key**.
2. Update `indieestatebackend/.env` (both `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY`).
3. Restart `./start-backend.sh`.
4. Confirm startup log: `startup headBucket OK` and `startup putObject OK`.
5. Upload again from the app (new presign uses the new key).

Do not commit `.env` to git.

## How to interpret 403

| Startup `putObject` | Phone PUT | Likely cause |
|---------------------|-----------|----------------|
| FAILED | 403 | IAM user in `.env` lacks `s3:PutObject` on `raw/*` |
| OK | 403 | Presigned signature / headers (check `contentLength` in IndieReelUpload) or bucket policy blocking anonymous-style presigned access |
| OK | 200 but `s3ObjectExists=false` on complete | Wrong key or different bucket than expected |

See `docs/REELS_ARCHITECTURE.md` for IAM policy example.
