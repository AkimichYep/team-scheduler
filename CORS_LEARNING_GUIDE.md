# CORS (Cross-Origin Resource Sharing) - Complete Learning Guide

## 📖 Table of Contents
1. [Fundamentals](#fundamentals)
2. [How CORS Works](#how-cors-works)
3. [Your Setup](#your-setup)
4. [Testing CORS](#testing-cors)
5. [Common Issues](#common-issues)
6. [Production Checklist](#production-checklist)

---

## Fundamentals

### What is an "Origin"?

An origin consists of THREE parts:
```
Origin = Protocol + Domain + Port

Examples:
✅ http://localhost:3000      (frontend)
✅ http://localhost:8080      (backend)
❌ http://localhost:3001      (different port = different origin!)
❌ https://localhost:3000     (different protocol = different origin!)
❌ http://example.com:3000    (different domain = different origin!)
```

### Why CORS Exists - Security Example

**Without CORS protection:**
```
Attacker's website (evil.com) has this code:
fetch('http://bank.com/api/transfer', {
    method: 'POST',
    body: { amount: 1000, to: 'attacker' }
})
// Bank API processes it with YOUR credentials! 💀
```

**With CORS protection:**
```
Browser checks: "Is evil.com allowed to access bank.com?"
bank.com says: "No, only https://mybank.com allowed"
Browser blocks request ✅
```

---

## How CORS Works

### Request Types

#### 1️⃣ Simple Requests (No Preflight)
Used for: GET, HEAD, POST with simple content-types

**Flow:**
```
Browser Request:
┌───────────────────────────────┐
│ GET /api/users HTTP/1.1        │
│ Origin: http://localhost:3000  │ ← Browser adds automatically
│ Host: localhost:8080           │
│ User-Agent: Chrome...          │
└───────────────────────────────┘
                ↓
        Server receives
        Checks: Is localhost:3000 in allowed origins? YES
                ↓
        Response:
        ┌───────────────────────────────────────┐
        │ HTTP/1.1 200 OK                        │
        │ Access-Control-Allow-Origin: ...      │
        │ http://localhost:3000                 │
        │                                       │
        │ [{ "id": 1, "username": "john" }]    │
        └───────────────────────────────────────┘
                ↓
Browser checks CORS headers
Access-Control-Allow-Origin matches Origin?
YES → JavaScript receives the data ✅
NO  → JavaScript blocked (CORS error) ❌
```

#### 2️⃣ Preflight Request (For Complex Requests)
Used for: PUT, DELETE, PATCH, POST with custom headers

**Why preflight?**
- The browser wants to make sure the server supports the method BEFORE sending actual data
- Saves bandwidth - if server says "no PUT allowed", don't waste time sending real request

**Flow:**
```
Step 1: Browser sends OPTIONS preflight
┌────────────────────────────────────────┐
│ OPTIONS /api/users/1 HTTP/1.1          │
│ Origin: http://localhost:3000          │
│ Access-Control-Request-Method: PUT     │ ← "I want to use PUT"
│ Access-Control-Request-Headers:        │
│   Content-Type                         │ ← "I want to send this header"
└────────────────────────────────────────┘
                ↓
        Server checks configuration:
        1. Is localhost:3000 allowed? YES
        2. Is PUT in allowed methods? YES
        3. Is Content-Type in allowed headers? YES
                ↓
Step 2: Server responds to OPTIONS
┌────────────────────────────────────────────┐
│ HTTP/1.1 200 OK                            │
│ Access-Control-Allow-Origin: ...           │
│   http://localhost:3000                    │
│ Access-Control-Allow-Methods: ...          │
│   GET, POST, PUT, DELETE, OPTIONS         │
│ Access-Control-Allow-Headers: ...          │
│   Content-Type, Authorization             │
│ Access-Control-Max-Age: 3600               │
│ (empty body - preflight only)              │
└────────────────────────────────────────────┘
                ↓
Browser sees all checks pass
                ↓
Step 3: Browser sends actual request
┌────────────────────────────────────────┐
│ PUT /api/users/1 HTTP/1.1              │
│ Origin: http://localhost:3000          │
│ Content-Type: application/json         │
│                                        │
│ { "username": "jane" }                 │
└────────────────────────────────────────┘
                ↓
        Server processes update
                ↓
Step 4: Server responds with data
┌────────────────────────────────────────┐
│ HTTP/1.1 200 OK                        │
│ Access-Control-Allow-Origin: ...       │
│   http://localhost:3000                │
│ { "id": 1, "username": "jane" }        │
└────────────────────────────────────────┘
                ↓
Browser allows JavaScript to use data ✅
```

### CORS Headers Reference

| Header | Sent by | Values | Example | Purpose |
|--------|---------|--------|---------|---------|
| `Origin` | Browser | origin | `http://localhost:3000` | Tells server where request came from |
| `Access-Control-Allow-Origin` | Server | origin or `*` | `http://localhost:3000` | Which origins can access |
| `Access-Control-Allow-Methods` | Server | HTTP verbs | `GET, POST, PUT, DELETE` | Which HTTP methods allowed |
| `Access-Control-Allow-Headers` | Server | header names | `Content-Type, Authorization` | Which request headers allowed |
| `Access-Control-Allow-Credentials` | Server | `true` or `false` | `true` | Whether credentials (auth) included |
| `Access-Control-Max-Age` | Server | seconds | `3600` | Cache preflight for 1 hour |
| `Access-Control-Expose-Headers` | Server | header names | `X-Total-Count` | Which response headers JavaScript can see |

---

## Your Setup

### Architecture

```
┌─────────────────────────┐
│   Frontend              │
│   (http://localhost:3000)│
│   Node.js + Express     │
│   Serves: EJS views,    │
│            static files │
└────────────┬────────────┘
             │ API calls to backend
             │ (fetch, $.ajax, etc.)
             │ CORS request!
             ↓
┌─────────────────────────────┐
│   Backend                   │
│ (http://localhost:8080)     │
│   Spring Boot               │
│   REST API endpoints        │
│   Database queries          │
└─────────────────────────────┘
```

### Your CORS Configuration

**In SecurityConfig.java:**

```java
// 1. CORS is ENABLED
.cors(cors -> cors.configurationSource(corsConfigurationSource()))

// 2. OPTIONS requests allowed without auth (for preflight)
.requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()

// 3. Allowed origins configured
configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000"));

// 4. Credentials allowed (for Basic Auth)
configuration.setAllowCredentials(true);

// 5. All necessary methods allowed
configuration.setAllowedMethods(Arrays.asList(
    "GET", "POST", "PUT", "DELETE", "OPTIONS"
));
```

---

## Testing CORS

### Test 1: Check if CORS is Working

**Option A: Using Browser Console**

```javascript
// Open browser console (F12) on http://localhost:3000
// Paste this:

fetch('http://localhost:8080/api/proxy/schedule/all-users', {
    method: 'GET',
    headers: {
        'Authorization': 'Basic ' + btoa('user:pass')  // Base64 encode credentials
    }
})
.then(r => r.json())
.then(data => console.log('✅ CORS works!', data))
.catch(err => console.error('❌ CORS error:', err))
```

**Option B: Using curl command**

```bash
# Open terminal and run:
curl -i \
  -H "Origin: http://localhost:3000" \
  -H "Access-Control-Request-Method: GET" \
  -H "Access-Control-Request-Headers: Content-Type" \
  -X OPTIONS \
  http://localhost:8080/api/proxy/schedule/all-users

# Look for these headers in response:
# Access-Control-Allow-Origin: http://localhost:3000
# Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS
```

### Test 2: Monitor Network Requests

**In Browser DevTools:**
1. Open DevTools (F12)
2. Go to Network tab
3. Filter by XHR (XMLHttpRequest)
4. Make a request
5. Click the request
6. Go to Response Headers tab
7. Look for `Access-Control-Allow-Origin` header

You should see:
```
Access-Control-Allow-Origin: http://localhost:3000
Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS
Access-Control-Allow-Credentials: true
```

### Test 3: Check Preflight Request

**For PUT/DELETE requests:**
1. Open DevTools (F12)
2. Go to Network tab
3. Make a PUT or DELETE request
4. You'll see TWO requests:
   - **First**: `OPTIONS` (preflight) - should get 200 OK
   - **Second**: `PUT` or `DELETE` (actual request) - should get 200 OK

---

## Common Issues

### ❌ Issue 1: "No 'Access-Control-Allow-Origin' header"

**Symptoms:**
```
Access to XMLHttpRequest at 'http://localhost:8080/api/...'
from origin 'http://localhost:3000' has been blocked by CORS policy
```

**Causes & Solutions:**

| Cause | Solution |
|-------|----------|
| Backend not configured for CORS | Add CORS configuration to SecurityConfig |
| Frontend origin not in allowed list | Add `http://localhost:3000` to `setAllowedOrigins()` |
| Using `http://localhost:3001` instead of `:3000` | Check the exact port in your setup |
| Using `https://` on development | Change to `http://` for local testing |

**Debug Steps:**
```javascript
// 1. Check what origin browser is sending
console.log(window.location.origin);  // Should be http://localhost:3000

// 2. Check backend response headers
// Open DevTools → Network → Click request → Response Headers
// Look for Access-Control-Allow-Origin

// 3. Test with curl
curl -i -H "Origin: http://localhost:3000" http://localhost:8080/api/users
```

### ❌ Issue 2: Preflight Request Fails

**Symptoms:**
- PUT/DELETE requests don't work
- See OPTIONS request returns 401 or 403

**Solution:**
```java
// Make sure OPTIONS is allowed WITHOUT authentication
.requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()

// Must come BEFORE anyRequest().authenticated()
.authorizeHttpRequests(auth -> auth
    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()  // ← HERE
    .anyRequest().authenticated()
)
```

### ❌ Issue 3: Credentials Not Sent

**Symptoms:**
- Authentication fails in cross-origin requests
- Headers like `Authorization` not included

**Solution:**
```javascript
// Frontend: Include credentials
fetch('http://localhost:8080/api/data', {
    method: 'GET',
    credentials: 'include',  // ← Add this! or can use 'same-origin'
    headers: {
        'Authorization': 'Basic ' + btoa('user:pass')
    }
})

// Backend: Allow credentials
configuration.setAllowCredentials(true);  // ← Must be true
```

---

## Production Checklist

### Before Deploying to Production

- [ ] **Update Allowed Origins**
  ```java
  configuration.setAllowedOrigins(Arrays.asList(
      "https://yourdomain.com",      // Production frontend
      "https://www.yourdomain.com"   // With www
      // Remove: "http://localhost:3000"
  ));
  ```

- [ ] **Use HTTPS everywhere**
  ```java
  // Development: http://localhost:3000
  // Production: https://yourdomain.com
  ```

- [ ] **Disable H2 Console**
  ```java
  // Remove this line before production!
  .requestMatchers("/h2-console/**").permitAll()
  ```

- [ ] **Limit Exposed Headers**
  ```java
  // Don't expose unnecessary headers
  configuration.setExposedHeaders(Arrays.asList(
      "Content-Type",
      "X-Total-Count"  // Only what you actually need
  ));
  ```

- [ ] **Set appropriate Max-Age**
  ```java
  // Production: cache preflight for longer
  configuration.setMaxAge(86400L);  // 24 hours
  ```

- [ ] **Use environment variables**
  ```java
  // Instead of hardcoding
  @Value("${app.cors.allowed-origins}")
  private String allowedOrigins;
  
  // Then in application.properties or application-prod.properties
  app.cors.allowed-origins=https://yourdomain.com
  ```

---

## Summary

**CORS in 30 seconds:**
1. Browser detects cross-origin request
2. Browser sends Origin header
3. Server checks if origin is allowed
4. If yes: server sends `Access-Control-Allow-*` headers
5. Browser receives headers and allows JavaScript to access the response

**Your Configuration Does:**
- ✅ Allows frontend at `http://localhost:3000` to access backend
- ✅ Allows all HTTP methods (GET, POST, PUT, DELETE)
- ✅ Allows credentials (username:password)
- ✅ Handles preflight requests (OPTIONS) automatically

**To Test:**
1. Open DevTools (F12)
2. Make a fetch request
3. Check Response Headers for `Access-Control-Allow-Origin`
4. No CORS error = success! ✅

