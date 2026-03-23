# Registration Debugging Guide

## What Was Changed:
I've added detailed debugging logs to track the registration process on BOTH client and server sides.

## Client-Side Logging Added:
1. **UserService.registerUser()** - Now logs:
   - Exact command being sent to server (with password masked)
   - Number of fields in the command
   - Server response received
   - Any validation errors

2. **ClientConnection.sendCommand()** - Now logs:
   - What was sent to the server
   - What response was received

## Server-Side Logging (Already Added):
- Detailed validation logs for each SIGNUP
- Public ID generation details
- Full user data being inserted
- Specific error messages

## How to Test and Debug:

### Step 1: Make Sure Server is Running
```
Check for: "Rental Server Starting on port 5000..." in RentalServer terminal
```

### Step 2: Launch the Client Application
```powershell
cd "c:\Users\User\OneDrive\Desktop\HOmerental\Tolet"
mvn javafx:run -q
```

### Step 3: Try to Register with Test Data
- **Username**: `testuser123`
- **Email**: `test123@example.com`
- **Password**: `password123`
- **Phone**: `9800000123`
- **Role**: `tenant`

### Step 4: Check Console Output
Look for logs like:
```
[UserService] Sending SIGNUP command: SIGNUP|testuser123|test123@example.com|***|tenant|9800000123|
[UserService] Raw command parts: 7
[UserService] Sending to server...
[UserService] Server response: SUCCESS
[UserService] Registration SUCCESS!
```

## If Registration Still Fails:

1. **Check Console for Error Messages** - Look for:
   - `[UserService] Connection error:` → Server not running
   - `[UserService] Server error:` → Validation failed
   - `[UserService] Unexpected response:` → Unknown issue

2. **Share Server Logs** - Copy output from RentalServer terminal showing:
   - `SIGNUP: Processing registration for email=...`
   - Any error messages with `Registration error:`

3. **Share Client Logs** - Copy output from client console showing:
   - `[UserService]` messages
   - `[ClientConnection]` messages

## Quick Troubleshooting:

| Error | Cause | Solution |
|-------|-------|----------|
| "Could not connect to server" | Server not running | Start RentalServer |
| "Email is required" | Email field empty | Fill all fields |
| "Phone is already exists" | Duplicate phone | Use different phone |
| "Username already exists" | Duplicate name | Use different username |
| No response, hangs | Connection issue | Check server logs |

---

**IMPORTANT**: After you run the test, share:
1. The console output from the client application
2. The RentalServer console output
3. The exact error message you see

This will help identify exactly where the registration is failing.
