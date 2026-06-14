# 🔧 Chat 500 Error - Comprehensive Fix

## ❌ What's Happening
Express routes are throwing 500 errors when calling the backend. This could be:
1. Backend not running
2. Backend not compiled with chat routes
3. Session/auth format issue

---

## ✅ Complete Fix Steps

### **Step 1: Stop All Services**
- **Terminal 1** (Backend): Press **Ctrl+C**
- **Terminal 2** (Frontend): Press **Ctrl+C**
- Wait 3 seconds for clean shutdown

---

### **Step 2: Rebuild Backend (IMPORTANT!)**
```powershell
cd C:\Users\Maksym_Yepaneshnikov\spring\team-scheduler

# Clean rebuild
mvn clean install -DskipTests

# This will take 2-3 minutes
# Watch for: "BUILD SUCCESS"
```

---

### **Step 3: Start Backend**
```powershell
# In new Terminal 1:
cd C:\Users\Maksym_Yepaneshnikov\spring\team-scheduler

# Set GROQ API key
$env:GROQ_API_KEY = "your-groq-api-key-here"

# Start Spring Boot
mvn spring-boot:run

# Wait for:
# "Started TeamSchedulerApplication in X.XXX seconds"
# "Tomcat started on port(s): 8080"
```

---

### **Step 4: Start Frontend** 
```powershell
# In new Terminal 2:
cd C:\Users\Maksym_Yepaneshnikov\spring\team-scheduler\frontend

npm start

# Wait for:
# "Frontend running on http://localhost:3000"
```

---

### **Step 5: Test Chat**
1. Open `http://localhost:3000`
2. Login with admin/admin
3. Click **💬 Chat**
4. Click **+ New Chat**
5. Enter title: "Test"
6. Select context: "Schedule"
7. Click **Create**

---

## 🐛 If You Still Get 500 Error:

### **Check Express Log Output**
Look in Terminal 2 (Frontend) for error messages like:
```
Creating conversation: { ... }
Error creating conversation: Error: ...
```

Common errors:
- **"User not found"** → Check login is working
- **"Cannot read property"** → Auth object format issue
- **"ECONNREFUSED"** → Backend not running on 8080

---

## 🔍 Debug Checklist

- [ ] Backend running? (Should see "Tomcat started on port(s): 8080")
- [ ] Frontend running? (Should see "Frontend running on http://localhost:3000")
- [ ] GROQ_API_KEY set? (`echo $env:GROQ_API_KEY` should show key)
- [ ] Build successful? (mvn says "BUILD SUCCESS")
- [ ] Port 8080 accessible? (`curl http://localhost:8080/api/chat/health`)
- [ ] Port 3000 accessible? (Open in browser)

---

## 📝 What Changed

| File | Changes |
|------|---------|
| `frontend/server.js` | Added error logging to chat routes |
| `frontend/src/services/api.js` | Added chat API methods |
| `ChatController.java` | Already has endpoints |

---

## 🚀 Expected Workflow After Fix

```
Frontend Chat Button Click
    ↓
POST /api/proxy/chat/conversations (Express)
    ↓ (logs: "Creating conversation...")
Express checks authentication
    ↓
Calls api.createChatConversation() 
    ↓ (with Basic Auth)
Axios POST to http://localhost:8080/api/chat/conversations
    ↓
Spring Boot processes (with authentication)
    ↓
Creates chat in H2 database
    ↓
Returns ChatConversationResponse
    ↓
Express sends JSON back to frontend
    ↓
Chat UI displays new conversation ✅
```

---

## ✨ After This Works

The chat will be fully functional:
- ✅ Create conversations
- ✅ Send messages
- ✅ Get AI responses  
- ✅ View history
- ✅ Archive chats

---

## 🆘 Still Stuck?

1. **Check Terminal 1 (Backend) Logs:**
   - Should show database tables created
   - Should show chat controller mapped

2. **Check Terminal 2 (Frontend) Logs:**
   - Should show Express routes loaded
   - Should show "Creating conversation:" with details

3. **Share What You See:**
   - Any error messages from terminals
   - Response status codes
   - Failed at which step

---

**Do the clean rebuild + restart both services. Chat should work!** 🎉

If not, check the Express terminal logs for the actual error message.

