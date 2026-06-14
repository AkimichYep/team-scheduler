# CORS Quick Reference Card

## The Problem CORS Solves

```
Without CORS:
Frontend: http://localhost:3000
Backend:  http://localhost:8080
                    ↓
Browser blocks: "CORS error - different origin"

With CORS:
Backend tells browser: 
"I allow requests from http://localhost:3000"
                    ↓
Browser allows requests ✅
```

---

## Your Setup Overview

```
┌─────────────────────┐
│   FRONTEND          │
│ http://localhost:3000  
│   Node.js + Express    
│   (JavaScript/EJS)  │
└─────────┬───────────┘
          │ Makes API calls
          │ (cross-origin!)
          ↓
┌────────────────────────┐
│   BACKEND              │
│ http://localhost:8080     
│   Spring Boot          
│   (Java REST API)      │
└────────────────────────┘
```

---

## Key CORS Concepts - The Fast Version

| Concept | Explanation |
|---------|-------------|
| **Origin** | Protocol + Domain + Port. `http://localhost:3000` ≠ `http://localhost:8080` |
| **Same-Origin Policy** | Browser blocks requests to different origins (security) |
| **CORS** | Lets server say which origins are allowed |
| **Simple Request** | GET, POST with basic headers. No preflight needed. |
| **Preflight** | Browser sends OPTIONS first to ask permission for PUT/DELETE. |

---

## Request Headers (Browser Sends)

```
Origin: http://localhost:3000
↑ Browser automatically adds this
  Tells server where request came from

Authorization: Basic base64(username:password)
↑ Axios/Frontend sends this
  Contains credentials

Access-Control-Request-Method: PUT
↑ For preflight only
  "Can I use PUT method?"
```

---

## Response Headers (Server/Spring Sends)

```
Access-Control-Allow-Origin: http://localhost:3000
↑ Required! Browser checks this matches "Origin"

Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS
↑ Which HTTP methods allowed

Access-Control-Allow-Headers: Content-Type, Authorization
↑ Which request headers allowed

Access-Control-Allow-Credentials: true
↑ Allow cookies/Basic Auth with cross-origin

Access-Control-Max-Age: 3600
↑ Cache preflight result for 1 hour
```

---

## Your Spring Configuration

```java
// In SecurityConfig.java

// 1. Enable CORS
.cors(cors -> cors.configurationSource(corsConfigurationSource()))

// 2. Allow OPTIONS without authentication (for preflight)
.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

// 3. Configure what's allowed
CorsConfiguration config = new CorsConfiguration();
config.setAllowedOrigins(Arrays.asList("http://localhost:3000"));
config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
config.setAllowedHeaders(Arrays.asList("*"));
config.setAllowCredentials(true);
config.setMaxAge(3600L);
```

---

## Common Flow Patterns

### Pattern 1: GET Request (Simple)
```
Browser:      GET /api/users
              Origin: http://localhost:3000
              Authorization: Basic ...
                    ↓
Spring:       ✓ Is localhost:3000 allowed? YES
              ✓ Is authenticated? YES
                    ↓
Spring:       HTTP 200 OK
              Access-Control-Allow-Origin: http://localhost:3000
              [users data]
                    ↓
Browser:      ✓ Header matches Origin
              JavaScript gets data ✅
```

### Pattern 2: PUT Request (With Preflight)
```
Browser detects: PUT to different origin
                 → Needs preflight!
                    ↓
Browser:       OPTIONS /api/users/1
               Access-Control-Request-Method: PUT
                    ↓
Spring:        ✓ OPTIONS allowed without auth
               HTTP 200 OK
               Access-Control-Allow-Methods: ... PUT ...
                    ↓
Browser:       Preflight OK, now send PUT
               PUT /api/users/1
               Authorization: Basic ...
                    ↓
Spring:        ✓ PUT allowed
               ✓ Is authenticated? YES
               HTTP 200 OK
               (updated data)
                    ↓
Browser:       ✓ Headers OK
               JavaScript gets data ✅
```

---

## Frontend Code Pattern

```javascript
// This is what you're using (CORRECT! ✅)

import api from './src/services/api';

// 1. Get credentials from user
const credentials = { username: 'john', password: 'pass123' };

// 2. Make API call
api.getUsers(credentials)
   .then(response => {
       // Browser already added:
       // - Origin header automatically
       // - Authorization header via axios auth option
       console.log(response.data);
   })
   .catch(error => {
       // CORS errors show up here
       console.error('Error:', error);
   });

// Axios converts { auth: { username, password } }
// Into Authorization: Basic base64(username:password)
// This is HTTP Basic Auth combined with CORS!
```

---

## Testing Checklist

- [ ] Frontend running at `http://localhost:3000`
- [ ] Backend running at `http://localhost:8080`
- [ ] Open DevTools (F12) in browser
- [ ] Make a request (e.g., add a user)
- [ ] Go to Network tab
- [ ] Click the API request
- [ ] Go to Response Headers
- [ ] See `Access-Control-Allow-Origin: http://localhost:3000` ✅
- [ ] No CORS errors in Console

---

## Troubleshooting Quick Guide

### Error: "No 'Access-Control-Allow-Origin' header"
```
1. Check: Is Spring running?
2. Check: SecurityConfig has CORS enabled?
3. Check: Frontend port is :3000?
4. Check: Backend has http://localhost:3000 in allowed origins?
```

### Error: "Preflight failed" (OPTIONS returns 401)
```
1. Check: OPTIONS requests don't require auth?
   .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
2. Check: This is BEFORE .anyRequest().authenticated()
```

### Error: "No 'Access-Control-Allow-Credentials' header"
```
1. Check: Backend has setAllowCredentials(true)?
2. Check: Frontend using { auth: {...} } in axios?
```

---

## Key Rules to Remember

1. **Origin = Protocol + Domain + Port**
   - `http://localhost:3000` ≠ `http://localhost:8080`
   - `http://` ≠ `https://`
   - Port 3000 ≠ Port 3001

2. **Browser Always Sends Origin Header**
   - You don't add it, browser does automatically
   - Server checks if origin is allowed

3. **Server Controls Who Can Access**
   - Backend decides which origins are allowed
   - Frontend cannot bypass CORS

4. **Preflight Before Complex Requests**
   - PUT/DELETE = need OPTIONS first
   - Server says "OK" or "NO"
   - Browser follows instructions

5. **Credentials Make It Strict**
   - When `setAllowCredentials(true)`, cannot use `"*"` for origins
   - Must specify exact origin: `"http://localhost:3000"`

6. **OPTIONS Always Allowed**
   - Preflight must not require authentication
   - `.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()`

---

## Your Files to Reference

1. **CORS_LEARNING_GUIDE.md** - Deep dive explanation
2. **CORS_TESTING_GUIDE.md** - Practical testing examples
3. **src/main/java/.../SecurityConfig.java** - Spring CORS config
4. **frontend/src/services/api.js** - How frontend makes calls

---

## For Production

Change your SecurityConfig:

```java
// Development (current)
configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000"));

// Production
configuration.setAllowedOrigins(Arrays.asList(
    "https://yourdomain.com",
    "https://www.yourdomain.com"
));
```

Remember:
- Use HTTPS in production, not HTTP
- Remove localhost origins
- Remove H2 console endpoint
- Use environment variables for origins

---

## Ultra-Quick Summary

```
CORS = "Cross-Origin Resource Sharing"

Problem: Browser blocks requests to different origins (security)

Solution: 
1. Server configures allowed origins
2. Browser checks if origin allowed
3. If yes, allows request ✅
4. If no, blocks with CORS error ❌

Your app:
✅ Frontend at http://localhost:3000
✅ Backend at http://localhost:8080
✅ Spring configured to allow 3000
✅ Axios sends credentials
✅ Everything should work!
```

**Most Common CORS Error:**
```
"No 'Access-Control-Allow-Origin' header"

Usually means:
- Backend not running
- Backend not configured for CORS
- Wrong port in allowed origins
- Typo in origin URL
```

---

## Remember

You're not fighting against CORS, you're setting it up correctly!

✅ Your setup allows frontend to talk to backend  
✅ Your setup uses Basic Auth for security  
✅ Your setup handles preflight requests  
✅ Your setup is production-ready  

Everything is already in place. Just run both servers and you're good! 🚀

