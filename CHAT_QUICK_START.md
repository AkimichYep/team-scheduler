# AI Chat Quick Start Guide

## 🚀 Get Started in 5 Minutes

### Step 1: Build the Project
```powershell
cd C:\Users\Maksym_Yepaneshnikov\spring\team-scheduler
mvn clean install
```

### Step 2: Set GROQ API Key
```powershell
$env:GROQ_API_KEY = "your-groq-api-key"
```

### Step 3: Run the Application
```powershell
mvn spring-boot:run
```

### Step 4: Open Chat Interface
```
http://localhost:8080/chat?userId=1
```

### Step 5: Start Chatting!
1. Click **"+ New Chat"**
2. Enter title: "My First Chat"
3. Select context: **Schedule**
4. Click **Create**
5. Type: "How is my team's workload?"
6. Press **Enter** or click **Send**
7. 🎉 Chat with AI!

---

## 💬 Chat Features

### Real-Time Messaging
- Messages appear instantly via WebSocket
- Typing indicators for AI responses
- Message history is saved automatically

### Context-Aware Responses
- AI understands your schedule data
- Provides personalized recommendations
- References team information

### Multiple Conversations
- Create separate chat threads
- Revisit previous conversations
- Organized by topic

---

## 🎯 Example Questions to Ask

### Schedule Analysis
- "How's my team's workload distribution?"
- "What are the busiest days next month?"
- "Show me coverage gaps"

### Recommendations
- "How can we improve scheduling?"
- "What shift patterns work best?"
- "Any bottlenecks I should address?"

### Team Discussion
- "How's team satisfaction looking?"
- "Best practices for our shifts?"
- "Ways to balance the workload?"

---

## 🔌 API Testing (REST)

### Create Conversation
```powershell
$params = "userId=1&title=Test Chat&context=schedule"
Invoke-WebRequest -Method POST `
  "http://localhost:8080/api/chat/conversations?$params"
```

### Send Message
```powershell
$json = @{
    conversation_id = 1
    content = "Analyze my schedule"
    context = "schedule"
} | ConvertTo-Json

Invoke-WebRequest -Method POST `
  -Uri "http://localhost:8080/api/chat/messages?userId=1" `
  -ContentType "application/json" `
  -Body $json
```

### Get Conversation History
```powershell
Invoke-WebRequest "http://localhost:8080/api/chat/conversations/1/messages"
```

---

## 📱 Frontend Integration

### Access Chat Page
```
http://localhost:8080/chat?userId=1
http://localhost:8080/chat?userId=2
```

### Add to Navigation
Update `header.ejs` to include:
```html
<a href="/chat" class="nav-link">💬 Chat</a>
```

---

## ⚙️ Database Schema

### Automatic Table Creation
Tables are created automatically via JPA:

**chat_conversations**
- id, user_id, title, context, created_at, updated_at, active

**chat_messages**
- id, user_id, conversation_id, content, role (USER/ASSISTANT), created_at

---

## 🔍 Troubleshooting

| Problem | Solution |
|---------|----------|
| WebSocket won't connect | Check browser console, verify `/ws/chat` endpoint |
| AI doesn't respond | Verify GROQ API key, check internet, see Spring logs |
| Messages not saving | Check database connection, verify repos are autowired |
| Chat UI not loading | Ensure view is mapped in controller, check view path |
| Build fails | Run `mvn clean`, check Java version is 21+ |

---

## 📊 Monitoring

### Check Service Health
```powershell
curl.exe http://localhost:8080/api/chat/health
# Response: Chat Service is running
```

### View Logs
```powershell
# Watch for chat-related logs:
# - "Created new conversation"
# - "Saved user message"
# - "Saved AI response"
# - "WebSocket connected"
```

---

## 🎓 Architecture Overview

```
User Browser
    ↓
EJS Chat UI (chat.ejs)
    ↓
WebSocket (SockJS + STOMP)
    ↓
Spring Boot Backend
    ├─→ ChatController (REST)
    ├─→ WebSocketChatController (Real-time)
    ├─→ ChatService (Business Logic)
    └─→ ChatMessage/ChatConversation (Entities)
    ↓
Database (H2/PostgreSQL)
    ├─→ chat_messages table
    └─→ chat_conversations table
    ↓
GRO AI API
    (for generating responses)
```

---

## 📝 What Was Added

### Backend
- ✅ ChatMessage & ChatConversation models
- ✅ Chat repositories for data persistence
- ✅ ChatService with context-aware AI
- ✅ ChatController (REST endpoints)
- ✅ WebSocketChatController (real-time messaging)
- ✅ WebSocketConfig (STOMP setup)

### Frontend
- ✅ chat.ejs (complete chat UI)
- ✅ Socket client integration
- ✅ Message rendering & real-time updates
- ✅ Conversation management

### Configuration
- ✅ WebSocket & SockJS dependencies in pom.xml

---

## 🚦 API Endpoints Summary

| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `/api/chat/conversations` | Create new chat |
| GET | `/api/chat/conversations` | List all chats |
| GET | `/api/chat/conversations/{id}` | Get specific chat |
| POST | `/api/chat/messages` | Send message & get AI response |
| GET | `/api/chat/conversations/{id}/messages` | Get chat history |
| POST | `/api/chat/conversations/{id}/archive` | Archive chat |
| GET | `/api/chat/health` | Health check |

---

## 🎨 UI Features

### Left Sidebar
- List of all conversations
- New Chat button
- Message count per conversation
- Last updated time

### Main Chat Area
- Message display with timestamps
- Auto-scroll to latest message
- User messages (blue) vs AI (gray)
- Typing animations

### Input Area
- Text input with character limit
- Send button
- Enter key shortcut
- Helpful tips

---

## 🔐 Security Notes

- ✅ Authentication required (Spring Security)
- ✅ User-specific conversations
- ✅ CORS enabled for frontend
- ✅ API key stored in environment variable
- ✅ Messages encrypted in database (optional)

---

## 📈 Next Steps

1. **Test the chat** - Try asking questions about schedules
2. **Integrate with UI** - Add chat link to navigation menu
3. **Customize prompts** - Adjust context prompts in ChatService
4. **Add features** - Rich messages, attachments, exports
5. **Monitor usage** - Track tokens and conversations
6. **Scale up** - Add caching, improve performance

---

## 💡 Pro Tips

### Ask for Specific Insights
❌ "Tell me about my schedule"  
✅ "Analyze team coverage for next week and suggest optimal shifts"

### Use Context Selection
Choose the right topic when creating chat:
- **Schedule** - For shift analysis
- **Team** - For workload distribution
- **Planning** - For future scheduling

### Save Important Chats
Name conversations descriptively:
- "May Schedule Review"
- "Team Optimization Discussion"
- "Q2 Planning"

### Reference Earlier Chats
You can always return to previous conversations to continue discussions!

---

## 📞 Need Help?

Check these docs:
- **Full Guide**: `AI_CHAT_INTEGRATION.md`
- **Spring AI**: `SPRING_AI_SETUP.md`
- **Project Reference**: `QUICK_REFERENCE.md`

---

## 🎉 You're Ready!

Your Team Scheduler now has AI-powered chat. Start conversations, get recommendations, and optimize your team's schedule!

**Happy Chatting!** 💬🚀

---

**Version**: 1.0 Quick Start  
**Last Updated**: June 14, 2026

