# 🚀 Quick Startup Guide - Chat Now Working!

## ✅ What Was Fixed

1. **Added Chat Route** to frontend server.js
2. **Added Chat Link** to navigation menu (💬 Chat)
3. **Fixed chat.ejs** template to work with page layout system
4. **Integrated chat UI** with header and authentication

---

## 🎯 To Start the Application

### Terminal 1: Start Backend (Spring Boot)
```powershell
cd C:\Users\Maksym_Yepaneshnikov\spring\team-scheduler

# Set GROQ API key first
$env:GROQ_API_KEY = "your-groq-api-key-here"

# Start Spring Boot
mvn spring-boot:run
```
⏳ Wait for: `Started TeamSchedulerApplication`

### Terminal 2: Start Frontend (Node.js)
```powershell
cd C:\Users\Maksym_Yepaneshnikov\spring\team-scheduler\frontend

npm install  # Only if dependencies missing

npm start
```
⏳ Wait for: `Frontend running on http://localhost:3000`

---

## 🌐 Access the Application

### Login Page
```
http://localhost:3000/login
```

**Demo Credentials:**
- Username: `admin`
- Password: `admin`

### Once Logged In
You'll see the navigation with:
- Team Members
- My Scheduler
- Team Summary
- **💬 Chat** ← NEW!

---

## 💬 Using the Chat

1. Click **"💬 Chat"** in the navigation menu
2. Click **"+ New Chat"** button
3. Enter title: "Schedule Review"
4. Select topic: "Schedule"
5. Click **Create**
6. Type: "How's my team's workload?"
7. Press **Enter** or click **Send**
8. 🎉 Get AI response!

---

## 📝 What Was Changed

### Backend
- ✅ Routes working: `/api/chat/*`
- ✅ WebSocket ready: `/ws/chat`
- ✅ AI integration: GROQ via Spring AI

### Frontend  
- ✅ Route added: `GET /chat` → renders chat.ejs
- ✅ Navigation: Chat link in header
- ✅ UI template: Fixed and working

### Database
- ✅ Tables: Auto-created on first run
  - `chat_conversations`
  - `chat_messages`

---

## 🔧 Troubleshooting

### Backend won't start
```powershell
# Clear Maven cache
mvn clean

# Try again
mvn spring-boot:run
```

### Frontend won't start
```powershell
# Go to frontend directory
cd frontend

# Install dependencies
npm install

# Start
npm start
```

### Chat page shows error
1. Check backend is running on `localhost:8080`
2. Check GROQ API key is set
3. Verify database created tables
4. Check browser console for errors

### API not responding
```powershell
# Test health check
curl.exe http://localhost:8080/api/chat/health
# Should respond: Chat Service is running
```

---

## 🎯 Quick Test

After services start:

```powershell
# Test backend is running
curl.exe http://localhost:8080/api/chat/health

# Test frontend is running
curl.exe http://localhost:3000/login
```

Both should respond successfully!

---

## 📊 Service Ports

| Service | Port | URL |
|---------|------|-----|
| Frontend | 3000 | http://localhost:3000 |
| Backend | 8080 | http://localhost:8080 |
| Database | In-memory (H2) | - |

---

## 📝 Files Modified Today

1. ✅ `frontend/server.js` - Added `/chat` route
2. ✅ `frontend/views/header.ejs` - Added Chat link
3. ✅ `frontend/views/chat.ejs` - Fixed template structure

---

## ✨ Features Ready

✅ Real-time chat interface  
✅ Conversation history  
✅ AI responses from GROQ  
✅ Schedule context integration  
✅ Multi-conversation support  
✅ Responsive design  

---

## 🎉 You're All Set!

Everything is ready. Just start both services and the chat will appear in your UI!

**Status**: ✅ READY TO USE

---

**Date**: June 14, 2026

