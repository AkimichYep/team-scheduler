# CORS Testing & Debugging Guide

## Quick Testing in Browser Console

Open your app at `http://localhost:3000`, then open Developer Tools (F12) and paste into the Console:

### Test 1: Simple GET Request

```javascript
// Test if CORS works with a simple GET request
fetch('http://localhost:8080/api/schedule/all-users', {
    method: 'GET',
    headers: {
        'Authorization': 'Basic ' + btoa('john:john123')  // Replace with valid user
    }
})
.then(response => {
    console.log('✅ Response Status:', response.status);
    console.log('✅ CORS Header:', response.headers.get('Access-Control-Allow-Origin'));
    return response.json();
})
.then(data => {
    console.log('✅ Data received:', data);
    console.log('✅ CORS is working!');
})
.catch(error => {
    console.error('❌ CORS Error:', error.message);
    console.error('Error details:', error);
});
```

**Expected output:**
```
✅ Response Status: 200
✅ CORS Header: http://localhost:3000
✅ Data received: [{...}, {...}]
✅ CORS is working!
```

### Test 2: PUT Request (Tests Preflight)

```javascript
// Test if PUT works (requires preflight OPTIONS request)
fetch('http://localhost:8080/api/users/1', {
    method: 'PUT',
    headers: {
        'Authorization': 'Basic ' + btoa('john:john123'),
        'Content-Type': 'application/json'
    },
    body: JSON.stringify({
        username: 'john_updated',
        email: 'john.updated@example.com'
    })
})
.then(response => {
    console.log('✅ PUT Request Status:', response.status);
    console.log('✅ CORS allows PUT method!');
    return response.json();
})
.then(data => {
    console.log('✅ Updated data:', data);
})
.catch(error => {
    console.error('❌ PUT Error:', error.message);
});
```

**What happens behind the scenes:**
```
1. Browser sees: PUT to different origin (localhost:3000 → localhost:8080)
2. Browser sends OPTIONS preflight request automatically:
   OPTIONS /api/users/1
   Origin: http://localhost:3000
   Access-Control-Request-Method: PUT
   Access-Control-Request-Headers: Content-Type

3. Server responds:
   HTTP 200 OK
   Access-Control-Allow-Origin: http://localhost:3000
   Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS

4. Browser allows PUT request
5. Actual PUT sent with credentials
6. Data updated ✅
```

### Test 3: Check CORS Headers

```javascript
// Just curious what headers the server sends?
fetch('http://localhost:8080/api/schedule/all-users', {
    method: 'GET',
    headers: {
        'Authorization': 'Basic ' + btoa('john:john123')
    }
})
.then(response => {
    console.log('=== CORS Headers ===');
    console.log('Access-Control-Allow-Origin:', response.headers.get('Access-Control-Allow-Origin'));
    console.log('Access-Control-Allow-Methods:', response.headers.get('Access-Control-Allow-Methods'));
    console.log('Access-Control-Allow-Headers:', response.headers.get('Access-Control-Allow-Headers'));
    console.log('Access-Control-Allow-Credentials:', response.headers.get('Access-Control-Allow-Credentials'));
    console.log('Access-Control-Max-Age:', response.headers.get('Access-Control-Max-Age'));
    return response.json();
})
.then(data => console.log('Data:', data))
.catch(error => console.error('Error:', error));
```

---

## Testing with cURL (Command Line)

### Test 1: Check CORS Response Header

```bash
# Simple GET with CORS headers
curl -i \
  -H "Origin: http://localhost:3000" \
  -H "Authorization: Basic $(echo -n 'john:john123' | base64)" \
  http://localhost:8080/api/schedule/all-users

# Look for these headers in response:
# Access-Control-Allow-Origin: http://localhost:3000
# Access-Control-Allow-Credentials: true
```

### Test 2: Test OPTIONS Preflight

```bash
# Simulate what browser does for PUT/DELETE preflight
curl -i -X OPTIONS \
  -H "Origin: http://localhost:3000" \
  -H "Access-Control-Request-Method: PUT" \
  -H "Access-Control-Request-Headers: Content-Type" \
  http://localhost:8080/api/users/1

# Should see:
# HTTP/1.1 200 OK
# Access-Control-Allow-Origin: http://localhost:3000
# Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS
```

### Test 3: Actual PUT Request

```bash
# Send actual PUT request with credentials
curl -i -X PUT \
  -H "Authorization: Basic $(echo -n 'john:john123' | base64)" \
  -H "Content-Type: application/json" \
  -H "Origin: http://localhost:3000" \
  -d '{"username":"john_new"}' \
  http://localhost:8080/api/users/1

# Should see:
# HTTP/1.1 200 OK
# Access-Control-Allow-Origin: http://localhost:3000
# (response body with updated user)
```

---

## Browser DevTools Network Tab Debugging

### Step-by-step:

1. **Open DevTools** (F12)
2. **Go to Network tab**
3. **Disable cache** (check "Disable cache" in Network tab)
4. **Make a request** from your app (e.g., add a user, edit a schedule)
5. **Look for your API request** (filter by XHR)
6. **Click on it**
7. **Go to "Response Headers" tab**

### What to look for:

#### ✅ Success Case
```
Response Headers:
├─ 200 OK                          ← Success status
├─ Access-Control-Allow-Origin: http://localhost:3000
├─ Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS
├─ Access-Control-Allow-Credentials: true
└─ (response body)
```

#### ❌ CORS Error Case
```
No "Access-Control-Allow-Origin" header
→ Check your Spring SecurityConfig
→ Is http://localhost:3000 in allowed origins?
```

#### ⏳ Preflight Check
For PUT/DELETE, you'll see TWO requests:

```
1. OPTIONS request (preflight)
   Status: 200 OK
   Headers:
   ├─ Access-Control-Allow-Origin: http://localhost:3000
   ├─ Access-Control-Allow-Methods: GET, POST, PUT, DELETE
   └─ (empty response body)

2. PUT/DELETE request (actual)
   Status: 200 OK
   Headers:
   ├─ Access-Control-Allow-Origin: http://localhost:3000
   └─ (response body with data)
```

---

## Common CORS Issues & Solutions

### Issue 1: "No 'Access-Control-Allow-Origin' header"

**Error in Browser:**
```
Access to XMLHttpRequest at 'http://localhost:8080/api/users'
from origin 'http://localhost:3000' has been blocked by CORS policy:
Response to preflight request doesn't have required header
'Access-Control-Allow-Origin'.
```

**Causes:**
- Spring is not configured for CORS
- Frontend origin not in allowed list
- PORT mismatch (using :3001 instead of :3000)

**Solutions:**

Check SecurityConfig.java:
```java
// ✅ CORS enabled?
.cors(cors -> cors.configurationSource(corsConfigurationSource()))

// ✅ Origin in allowed list?
configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000"));

// ✅ Using correct port?
// Frontend: http://localhost:3000 ← yes, port 3000
// Backend: http://localhost:8080 ← backend port
```

### Issue 2: 401 Unauthorized on Preflight

**Error:**
```
Options request returns 401
```

**Cause:**
OPTIONS (preflight) requires authentication

**Solution:**
```java
// Make sure OPTIONS doesn't require auth
.authorizeHttpRequests(auth -> auth
    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()  // ← ONLY this
    .anyRequest().authenticated()
)

// Order MATTERS! Put permitAll() for OPTIONS BEFORE authenticated()
```

### Issue 3: Credentials Not Working

**Symptom:**
```
Authorization header not sent
CORS error with credentials
```

**Frontend Fix:**
```javascript
// Use credentials: 'include' if CORS + cookies needed
fetch('http://localhost:8080/api/data', {
    credentials: 'include'  // or 'same-origin'
})

// OR use axios with auth parameter ✅ (what we use)
axios.get(url, { auth: { username, password } })
```

**Backend Fix:**
```java
// Must allow credentials
configuration.setAllowCredentials(true);

// When true, cannot use wildcard "*" for allowed-origins
// Must specify exact origins:
configuration.setAllowedOrigins(Arrays.asList(
    "http://localhost:3000"  // ← exact, not "*"
))
```

### Issue 4: PUT/DELETE Fails

**Symptom:**
```
OPTIONS preflight passes but PUT/DELETE fails
```

**Cause:**
PUT/DELETE not in allowed methods

**Solution:**
```java
configuration.setAllowedMethods(Arrays.asList(
    "GET", "POST", "PUT", "DELETE", "OPTIONS"  // ← All needed
));
```

---

## Complete Flow Diagram

### Simple GET Request
```
┌─────────────────────┐
│ Browser              │
│ http://localhost:3000│
│                      │
│ fetch(...)           │
└──────────────┬───────┘
               │ 1. Detects cross-origin
               │    Adds Origin header
               ↓
┌──────────────────────────────┐
│ Request to Spring Backend     │
│ http://localhost:8080        │
│                              │
│ GET /api/users              │
│ Origin: http://localhost:300│
│ Authorization: Basic ...     │
└──────────────┬───────────────┘
               │ 2. Spring receives
               │    Checks CORS config
               │    Checks Authentication
               ↓
┌──────────────────────────────┐
│ Spring checks:               │
│ ✓ Is localhost:3000 allowed? │
│ ✓ Is GET method allowed?     │
│ ✓ Is user authenticated?     │
│ → YES to all!                │
└──────────────┬───────────────┘
               │ 3. Spring sends response
               │    with CORS headers
               ↓
┌──────────────────────────────────┐
│ Response to Browser              │
│                                  │
│ HTTP 200 OK                      │
│ Access-Control-Allow-Origin:     │
│   http://localhost:3000          │
│ Content-Type: application/json   │
│                                  │
│ [{ "id": 1, "name": "John" }]   │
└──────────────┬──────────────────┘
               │ 4. Browser checks header
               │    Matches Origin? YES
               ↓
┌─────────────────────┐
│ JavaScript can access response
│ console.log(data)  ✅
└─────────────────────┘
```

### Complex PUT Request with Preflight
```
┌────────────────────────────────┐
│ Browser sees PUT to different   │
│ origin with Content-Type header │
│ → Needs to ask permission first │
└────────────────────┬────────────┘

        STEP 1: PREFLIGHT
        
         ┌──────────────┐
         │ Browser sends││
         │ OPTIONS      ││
         └────────┬─────┘│
                  ↓
┌─────────────────────────────────┐
│ OPTIONS /api/users/1             │
│ Origin: http://localhost:3000    │
│ Access-Control-Request-Method: PUT
│ Access-Control-Request-Headers: Content-Type
└─────────────────┬───────────────┘
                  │
                  ↓
         ┌──────────────┐
         │ Spring checks│
         │ OPTIONS route│
         │ (no auth req)│
         └────────┬─────┘
                  │
                  ↓
         ┌──────────────────────┐
         │ HTTP 200 OK          │
         │ Access-Control-Allow │
         │ -Origin:             │
         │ http://localhost:3000│
         │ Access-Control-Allow │
         │ -Methods: ... PUT ..  │
         └────────┬─────────────┘
                  │
         ┌────────▼─────────┐
         │ Browser: preflight
         │       OK, proceed!
         └────────┬─────────┘

        STEP 2: ACTUAL REQUEST

         ┌──────────────┐
         │ Browser sends│
         │ PUT request  │
         └────────┬─────┘
                  ↓
┌─────────────────────────────────────┐
│ PUT /api/users/1                     │
│ Origin: http://localhost:3000        │
│ Authorization: Basic ...             │
│ Content-Type: application/json       │
│                                      │
│ { "name": "Jane" }                   │
└─────────────────┬───────────────────┘
                  │
                  ↓
         ┌──────────────┐
         │ Spring       │
         │ Authenticates
         │ Updates data │
         └────────┬─────┘
                  │
                  ↓
         ┌──────────────────────┐
         │ HTTP 200 OK          │
         │ Access-Control-Allow │
         │ -Origin: ...         │
         │                      │
         │ { "id": 1,           │
         │   "name": "Jane" }   │
         └────────┬─────────────┘
                  │
         ┌────────▼─────────┐
         │ Browser:         │
         │ All headers OK → 
         │ JavaScript can
         │ access! ✅
         └──────────────────┘
```

---

## Summary

| What | Where | To Check |
|------|-------|----------|
| Cross-origin request | Browser | Opens Devtools → Network → Click request |
| CORS config | Spring | SecurityConfig.java → corsConfigurationSource() |
| Allowed origins | Spring | setAllowedOrigins() |
| Allowed methods | Spring | setAllowedMethods() |
| Credentials allowed | Spring | setAllowCredentials(true) |
| Response status | Frontend | response.status or DevTools |
| CORS headers | Frontend | response.headers or DevTools → Response Headers |

---

## Next Steps

1. **Run your app** (both frontend at :3000 and backend at :8080)
2. **Test in browser console** using examples above
3. **Check DevTools Network tab** for response headers
4. **Try adding/editing/deleting** data through the UI
5. **Verify no CORS errors** in DevTools Console

**If you see CORS errors, check:**
1. Is Spring running on :8080?
2. Is Node.js running on :3000?
3. Is SecurityConfig updated with CORS?
4. Are there errors in Spring logs?

All your CORS setup is already correct! Now just verify it's working. 🎉

