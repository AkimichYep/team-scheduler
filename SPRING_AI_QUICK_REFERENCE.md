# Spring-AI Quick Reference

## Quick Start

### 1. Set API Key (PowerShell)
```powershell
$env:GROQ_API_KEY = "gsk_xxxxxxxxxxxxx"
```

### 2. Build & Run
```powershell
mvn clean install
mvn spring-boot:run
```

### 3. Test Endpoints (PowerShell)
```powershell
# Analyze team schedule
Invoke-WebRequest http://localhost:8080/api/schedule-analyzer/analyze

# Analyze specific user (User ID: 1)
Invoke-WebRequest http://localhost:8080/api/schedule-analyzer/analyze/user/1

# Get scheduling suggestions
Invoke-WebRequest -Method POST `
  -Uri "http://localhost:8080/api/schedule-analyzer/suggest?context=10-person team"

# Health check
Invoke-WebRequest http://localhost:8080/api/schedule-analyzer/health
```

## Available Models

The following GROQ models are available and can be configured in `application.properties`:

| Model | Tokens | Speed | Best For |
|-------|--------|-------|----------|
| `mixtral-8x7b-32768` (default) | 32.8k | ⚡⚡⚡ | Balanced, general use |
| `llama2-70b-4096` | 4k | ⚡⚡ | Simpler tasks |
| `llama-3-70b-8192` | 8k | ⚡⚡⚡ | Latest, most capable |

To change model, edit `application.properties`:
```properties
spring.ai.groq.chat.options.model=llama-3-70b-8192
```

## Temperature Settings

- **0.0-0.3:** Deterministic, focused (best for analysis)
- **0.4-0.7:** Balanced (current setting)
- **0.8-1.0:** Creative, varied

Current setting: `0.7` (good for suggestions)

## Service Architecture

```
ScheduleAnalyzerController
    ↓
ScheduleAnalyzerService
    ↓
ChatClient (Spring AI)
    ↓
GROQ API
```

## Environment Setup Across Platforms

### Windows (Permanent)
1. Windows Logo + X → System → Advanced System Settings
2. Environment Variables → New
3. Variable name: `GROQ_API_KEY`
4. Variable value: `gsk_xxxxx...`
5. Restart IDE/Terminal

### Verify Setup
```powershell
echo $env:GROQ_API_KEY
```

## Common Issues & Solutions

| Issue | Solution |
|-------|----------|
| "401 Unauthorized" | Check API key validity in GROQ console |
| "Cannot resolve symbol ChatClient" | Run `mvn clean install` |
| No response from AI | Check internet connectivity |
| Slow responses | Try shorter context in requests |
| Build fails | Clear Maven cache: `mvn clean` |

## Integration Points

### Currently Used
- Team schedule analysis
- User-specific recommendations
- Schedule optimization suggestions

### Potential Additions
- Real-time shift recommendations
- Conflict detection and resolution
- Team performance analysis
- Resource optimization
- Predictive scheduling

## File Structure

```
team-scheduler/
├── src/main/java/com/scheduler/
│   ├── service/
│   │   └── ScheduleAnalyzerService.java (NEW)
│   ├── controller/
│   │   └── ScheduleAnalyzerController.java (NEW)
│   ├── config/
│   │   └── AiConfig.java (NEW)
│   └── dto/
│       └── AiAnalysisResponse.java (NEW)
├── src/main/resources/
│   └── application.properties (MODIFIED)
├── pom.xml (MODIFIED)
└── SPRING_AI_SETUP.md (NEW - Full guide)
```

## Testing in Browser

Open browser and navigate to:
```
http://localhost:8080/api/schedule-analyzer/health
```

Should return:
```
Schedule Analyzer Service is running
```

## Curl Examples

### Windows PowerShell
```powershell
# Simple analyze
curl.exe http://localhost:8080/api/schedule-analyzer/analyze

# With output to file
curl.exe http://localhost:8080/api/schedule-analyzer/analyze | Out-File response.txt

# POST request with parameters
curl.exe -X POST -G http://localhost:8080/api/schedule-analyzer/suggest `
  -d "context=retail scheduling with 24/7 coverage"
```

### Windows CMD
```cmd
curl http://localhost:8080/api/schedule-analyzer/analyze
```

## Next: Frontend Integration

To use from frontend (Node.js/React):

```javascript
// Analyze schedule
const response = await fetch('http://localhost:8080/api/schedule-analyzer/analyze')
  .then(r => r.text())
  .then(text => console.log(text));

// With parameters
const response = await fetch(
  'http://localhost:8080/api/schedule-analyzer/suggest',
  {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: 'context=team size=12'
  }
);
```

## Resources

- 📚 [Spring AI Docs](https://docs.spring.io/spring-ai/reference/)
- 🔑 [GROQ API Console](https://console.groq.com)
- 📖 [GROQ Models Docs](https://console.groq.com/docs/models)
- 🚀 [Spring Boot 3.4.0](https://spring.io/projects/spring-boot)

---
**Version:** 1.0 | **Date:** June 14, 2026

