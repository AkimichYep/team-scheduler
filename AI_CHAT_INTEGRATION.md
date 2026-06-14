# AI Chat Integration Guide

## Overview
Your Team Scheduler now includes a full-featured AI Chat system with:
- ✅ Context-aware scheduling assistant
- ✅ Message persistence and history
- ✅ Real-time WebSocket messaging
- ✅ Interactive EJS frontend chat UI
- ✅ Multiple conversation threads

## Architecture

```
┌─────────────────────────────────────────────────────┐
│                 Frontend (EJS)                      │
│  - Chat UI Components                              │
│  - WebSocket Connection (SockJS + STOMP)           │
│  - Message Display & Real-time Updates             │
└──────────────────┬──────────────────────────────────┘
                   │
                   │ WebSocket/REST API
                   │
┌──────────────────▼──────────────────────────────────┐
│            Spring Boot Backend                      │
│                                                     │
│  ┌─────────────────────────────────────────────┐  │
│  │ ChatController (REST) & WebSocketController  │  │
│  │ - Conversation Management                   │  │
│  │ - Message Handling                          │  │
│  └─────────────────────────────────────────────┘  │
│                         ▲                          │
│                         │                          │
│  ┌──────────────────────▼──────────────────────┐  │
│  │ ChatService                                  │  │
│  │ - Message Processing                         │  │
│  │ - Context Building (Schedule Integration)   │  │
│  │ - AI Response Generation (GROQ/Spring AI)  │  │
│  │ - Data Persistence                          │  │
│  └──────────────────────────────────────────────┘  │
│                         ▲                          │
│                         │                          │
│  ┌──────────────────────┴──────────────────────┐  │
│  │  Repositories & Models                      │  │
│  │ - ChatMessage                               │  │
│  │ - ChatConversation                          │  │
│  │ - User Integration                          │  │
│  └─────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────┘
                   │
                   │ Database
                   │
┌──────────────────▼──────────────────────────────────┐
│           H2/PostgreSQL Database                    │
│  - chat_messages table                              │
│  - chat_conversations table                         │
└─────────────────────────────────────────────────────┘
```

## Database Schema

### chat_conversations table
```sql
CREATE TABLE chat_conversations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    context VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    active BOOLEAN DEFAULT TRUE,
    token_count BIGINT DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

### chat_messages table
```sql
CREATE TABLE chat_messages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    conversation_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    metadata TEXT,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (conversation_id) REFERENCES chat_conversations(id)
);
```

## REST API Endpoints

### Conversation Management

#### 1. Create New Conversation
```http
POST /api/chat/conversations
Query Parameters:
  - userId (required): Long - User ID
  - title (optional): String - Conversation title
  - context (optional): String - Topic context (schedule, team, planning, etc.)

Response:
{
  "id": 1,
  "user_id": 1,
  "title": "Schedule Review",
  "description": "",
  "context": "schedule",
  "created_at": "2024-06-14T10:30:00",
  "updated_at": "2024-06-14T10:30:00",
  "active": true,
  "message_count": 0
}
```

#### 2. Get User Conversations
```http
GET /api/chat/conversations?userId=1

Response:
[
  {
    "id": 1,
    "user_id": 1,
    "title": "Schedule Review",
    "context": "schedule",
    "created_at": "2024-06-14T10:30:00",
    "updated_at": "2024-06-14T11:00:00",
    "active": true,
    "message_count": 5
  },
  ...
]
```

#### 3. Get Specific Conversation
```http
GET /api/chat/conversations/{conversationId}

Response:
{
  "id": 1,
  "user_id": 1,
  "title": "Schedule Review",
  "description": "",
  "context": "schedule",
  "created_at": "2024-06-14T10:30:00",
  "updated_at": "2024-06-14T11:00:00",
  "active": true,
  "message_count": 5
}
```

#### 4. Archive Conversation
```http
POST /api/chat/conversations/{conversationId}/archive

Response:
Conversation archived successfully
```

### Message Management

#### 1. Send Message (REST)
```http
POST /api/chat/messages?userId=1
Content-Type: application/json

Request Body:
{
  "conversation_id": 1,
  "content": "How can I balance the team's workload?",
  "context": "schedule"
}

Response:
{
  "id": 5,
  "conversation_id": 1,
  "user_id": 1,
  "content": "Based on your schedule data, here are recommendations...",
  "role": "ASSISTANT",
  "created_at": "2024-06-14T10:35:00",
  "metadata": "{...}"
}
```

#### 2. Get Conversation History
```http
GET /api/chat/conversations/{conversationId}/messages

Response:
[
  {
    "id": 1,
    "conversation_id": 1,
    "user_id": 1,
    "content": "How is my team's schedule looking?",
    "role": "USER",
    "created_at": "2024-06-14T10:30:00"
  },
  {
    "id": 2,
    "conversation_id": 1,
    "user_id": 1,
    "content": "Current team distribution shows...",
    "role": "ASSISTANT",
    "created_at": "2024-06-14T10:30:30"
  },
  ...
]
```

#### 3. Health Check
```http
GET /api/chat/health

Response:
Chat Service is running
```

## WebSocket (Real-Time) Endpoints

### Connection Setup
```javascript
// Connect to WebSocket endpoint
const socket = new SockJS('http://localhost:8080/ws/chat');
const stompClient = Stomp.over(socket);

stompClient.connect({}, (frame) => {
    console.log('Connected:', frame);
    
    // Subscribe to receive messages
    stompClient.subscribe('/topic/chat', (message) => {
        console.log('Received:', JSON.parse(message.body));
    });
});
```

### Send Message via WebSocket
```javascript
stompClient.send('/app/chat/send', {}, JSON.stringify({
    userId: 1,
    conversationId: 1,
    content: "Tell me about shift patterns",
    context: "schedule"
}));
```

### Message Broadcast Topics
- `/topic/chat` - General chat messages
- `/topic/chat/messages` - Typed messages with metadata

## Frontend Usage

### Accessing the Chat UI
```
http://localhost:8080/chat?userId=1
```

### Key Features in Frontend
1. **New Conversation Button** - Create new chat threads
2. **Conversation List** - All active conversations with message count
3. **Message Display** - Real-time message rendering
4. **Input Area** - Send messages with keyboard shortcut (Enter)
5. **Context Menu** - Select topic for better AI responses

### JavaScript Integration
The chat.ejs template includes:
- SockJS for WebSocket fallback
- STOMP protocol for messaging
- Auto-reconnection logic
- Message formatting and timestamps
- Responsive UI with Bootstrap

## User Guide

### Getting Started
1. **Navigate to Chat**
   ```
   Click "Chat" in the navigation menu
   ```

2. **Create New Conversation**
   ```
   Click "+ New Chat"
   Enter title and select topic (Schedule, Team, Shift Planning, etc.)
   Click "Create"
   ```

3. **Send Messages**
   ```
   Type your question or request
   Press Enter or click Send
   Wait for AI assistant response
   ```

4. **View History**
   ```
   All messages are saved automatically
   Reopen any conversation to see full history
   ```

### Example Questions
- "How is my team's workload distributed?"
- "Can you suggest optimal shift patterns?"
- "What are the scheduling conflicts this month?"
- "How can we improve team satisfaction?"
- "Show me team coverage analysis"
- "Plan shifts for next week"

## Configuration

### Application Properties
```properties
# Chat context is automatically added based on schedule data
# No additional configuration required beyond Spring AI setup
spring.ai.openai.api-key=${GROQ_API_KEY}
spring.ai.openai.base-url=https://api.groq.com/openai
spring.ai.openai.chat.options.model=llama-3.3-70b-versatile
```

### WebSocket Configuration
Configured in `WebSocketConfig.java`:
- Endpoint: `/ws/chat`
- Protocol: STOMP over WebSocket/SockJS
- Message Broker: Simple in-memory broker
- App Destination Prefix: `/app`

## Files Created

### Backend (Java)
1. **Models**
   - `ChatMessage.java` - Message entity with role (USER/ASSISTANT)
   - `ChatConversation.java` - Conversation entity

2. **Repositories**
   - `ChatMessageRepository.java` - Message persistence
   - `ChatConversationRepository.java` - Conversation persistence

3. **DTOs**
   - `ChatMessageRequest.java` - Request payload
   - `ChatMessageResponse.java` - Response payload
   - `ChatConversationResponse.java` - Conversation info

4. **Services**
   - `ChatService.java` - Main chat business logic
     - Message handling
     - Context building from schedules
     - AI integration with GROQ

5. **Controllers**
   - `ChatController.java` - REST endpoints
   - `WebSocketChatController.java` - WebSocket STOMP handlers

6. **Configuration**
   - `WebSocketConfig.java` - WebSocket/STOMP setup

### Frontend
1. **Views**
   - `chat.ejs` - Complete chat UI template

### Configuration Files Modified
1. **pom.xml** - Added WebSocket and STOMP dependencies

## Testing

### Using cURL (REST API)
```powershell
# Create conversation
$conv = curl.exe -X POST "http://localhost:8080/api/chat/conversations" `
  -d "userId=1&title=Test%20Chat&context=schedule" | ConvertFrom-Json

# Get conversation ID
$convId = $conv.id

# Send message
curl.exe -X POST "http://localhost:8080/api/chat/messages?userId=1" `
  -H "Content-Type: application/json" `
  -d "{\"conversation_id\": $convId, \"content\": \"How's the team?\", \"context\": \"schedule\"}"

# Get history
curl.exe "http://localhost:8080/api/chat/conversations/$convId/messages"
```

### Using JavaScript (Browser Console)
```javascript
// Create conversation
const response = await fetch(
  'http://localhost:8080/api/chat/conversations?userId=1&title=Test&context=schedule',
  { method: 'POST' }
);
const conv = await response.json();
console.log('Conversation:', conv);

// Send message
const msgResponse = await fetch('http://localhost:8080/api/chat/messages?userId=1', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    conversation_id: conv.id,
    content: 'Can you review my schedule?',
    context: 'schedule'
  })
});
const aiResponse = await msgResponse.json();
console.log('AI said:', aiResponse.content);
```

### Using Postman
1. **Create Conversation**
   - Method: POST
   - URL: `http://localhost:8080/api/chat/conversations?userId=1&title=Postman%20Test&context=schedule`
   - Click Send

2. **Send Message**
   - Method: POST
   - URL: `http://localhost:8080/api/chat/messages?userId=1`
   - Body (JSON):
     ```json
     {
       "conversation_id": 1,
       "content": "Analyze my schedule patterns",
       "context": "schedule"
     }
     ```

## Context Awareness

The AI chat system is context-aware and considers:

1. **Schedule Data**
   - User's current schedule entries
   - Workload distribution
   - Team coverage patterns

2. **Scheduling Best Practices**
   - Optimal shift patterns
   - Work-life balance recommendations
   - Team satisfaction improvements

3. **Conversation Context**
   - Topic selection (schedule, team, planning)
   - Message history
   - Previous recommendations

## Performance Considerations

1. **Message Caching**
   - Consider adding Redis caching for frequently accessed conversations
   - Cache schedule context to reduce database queries

2. **Token Limits**
   - Track token usage per conversation
   - Implement limits to prevent excessive API costs

3. **Database Indexing**
   - Add indexes on frequently queried columns:
     ```sql
     CREATE INDEX idx_chat_user_id ON chat_conversations(user_id);
     CREATE INDEX idx_chat_conv_id ON chat_messages(conversation_id);
     ```

## Troubleshooting

### WebSocket Connection Issues
```
Problem: WebSocket fails to connect
Solution: 
1. Ensure /ws/chat endpoint is accessible
2. Check browser console for SockJS connection errors
3. Verify @EnableWebSocketMessageBroker is in WebSocketConfig
```

### Messages Not Saving
```
Problem: Messages appear but don't persist
Solution:
1. Verify ChatMessage and ChatConversation repositories are created
2. Check database is accessible
3. View Spring logs for SQL errors
```

### AI Response Timeout
```
Problem: Long wait for AI response
Solution:
1. Check GROQ API key is valid
2. Verify internet connectivity
3. Check GROQ service status
4. Review Spring AI logs for API errors
```

### Frontend Not Loading
```
Problem: chat.ejs view returns 404
Solution:
1. Ensure view name "chat" is mapped in controller
2. Check EJS view files are in frontend/views directory
3. Verify Express.js config includes correct views path
```

## Integration with Existing Features

### Schedule Integration
The chat system integrates with existing `ScheduleService`:
```java
// Available in ChatService for context
scheduleService.getAllScheduleEntries()
scheduleService.getScheduleForMonth(userId, year, month)
scheduleService.getScheduleForUser(userId)
```

### User Integration
Leverages existing authentication:
- Uses `User` entity from UserRepository
- Respects Spring Security authentication
- User-specific conversation threads

### Schedule Analyzer Integration
Chat system complements `ScheduleAnalyzerService`:
- REST API for one-off analysis
- Chat for ongoing discussions
- Can quote analysis results in conversations

## Future Enhancements

1. **Rich Message Features**
   - Markdown support for better formatting
   - File/schedule attachments
   - Quick action buttons

2. **Advanced Analytics**
   - Conversation sentiment analysis
   - Common question extraction
   - Usage patterns

3. **AI Capabilities**
   - Multi-turn conversation memory
   - Schedule modification suggestions
   - Team dynamics analysis

4. **Collaboration**
   - Team chat channels
   - Shared conversations
   - Real-time collab

5. **Integration**
   - Slack integration
   - Email notifications
   - Calendar sync

## Quick Start

### Build & Run
```powershell
# Set GROQ API key
$env:GROQ_API_KEY = "your-key"

# Build project
mvn clean install

# Run application
mvn spring-boot:run

# Access chat UI
http://localhost:8080/chat?userId=1
```

### First Chat
1. Click "+ New Chat"
2. Title: "Getting Started"
3. Context: "Schedule"
4. Create
5. Ask: "What does my team's schedule look like?"
6. Enjoy AI recommendations!

## Support & Resources

- **Spring AI Docs**: https://docs.spring.io/spring-ai/
- **GROQ API**: https://console.groq.com
- **WebSocket Guide**: https://spring.io/guides/gs/messaging-stomp-websocket/
- **Project Docs**: See SPRING_AI_QUICK_REFERENCE.md

---

**Version**: 1.0.0  
**Date**: June 14, 2026  
**Status**: ✅ Production Ready

