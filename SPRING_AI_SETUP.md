# Spring-AI Integration Guide for Team Scheduler

## Overview
Spring-AI with GROQ has been successfully integrated into your Team Scheduler application. This enables AI-powered schedule analysis and optimization recommendations.

## Setup Instructions

### 1. Set Environment Variable
You need to set your GROQ API key as an environment variable:

**Windows PowerShell:**
```powershell
$env:GROQ_API_KEY = "your-groq-api-key-here"
```

**Windows Command Prompt:**
```cmd
set GROQ_API_KEY=your-groq-api-key-here
```

**Permanent (Windows System Environment Variable):**
- Right-click "This PC" → Properties → Advanced system settings
- Click "Environment Variables"
- Add new User/System variable: `GROQ_API_KEY` with your API key

### 2. Build the Project
```powershell
cd C:\Users\Maksym_Yepaneshnikov\spring\team-scheduler
mvn clean install
```

### 3. Run the Application
```powershell
mvn spring-boot:run
```

## Available Endpoints

### 1. Analyze Team Schedule
**Endpoint:** `GET /api/schedule-analyzer/analyze`

**Description:** Analyzes the entire team's schedule and provides optimization recommendations.

**Example:**
```bash
curl http://localhost:8080/api/schedule-analyzer/analyze
```

**Response:**
```
Analysis of team schedule with insights about:
- Current schedule overview
- Potential issues or conflicts
- Optimization recommendations
- Team workload distribution
```

### 2. Analyze User Schedule
**Endpoint:** `GET /api/schedule-analyzer/analyze/user/{userId}`

**Description:** Provides personalized analysis for a specific user's schedule.

**Example:**
```bash
curl http://localhost:8080/api/schedule-analyzer/analyze/user/1
```

**Response:**
```
Personalized analysis for the user including:
- Summary of shift patterns
- Workload analysis
- Work-life balance recommendations
- Schedule improvement suggestions
```

### 3. Get Scheduling Suggestions
**Endpoint:** `POST /api/schedule-analyzer/suggest`

**Query Parameters:**
- `context` (optional): Additional context for scheduling (e.g., team size, constraints)

**Example:**
```bash
curl -X POST "http://localhost:8080/api/schedule-analyzer/suggest?context=10-person team with shift preferences"
```

**Response:**
```
AI-generated scheduling suggestions with:
- Recommended shift patterns
- Optimal team distribution
- Best practices
- Implementation roadmap
```

### 4. Health Check
**Endpoint:** `GET /api/schedule-analyzer/health`

**Description:** Checks if the analyzer service is running.

**Example:**
```bash
curl http://localhost:8080/api/schedule-analyzer/health
```

**Response:**
```
Schedule Analyzer Service is running
```

## File Changes Summary

### Created Files:
1. **ScheduleAnalyzerService.java** - Core service for AI-powered analysis
2. **ScheduleAnalyzerController.java** - REST endpoints for schedule analysis
3. **AiConfig.java** - Spring AI configuration
4. **AiAnalysisResponse.java** - DTO for structured responses

### Modified Files:
1. **pom.xml** - Added Spring-AI dependencies for GROQ
2. **application.properties** - Added GROQ configuration

## Configuration Details

### GROQ Model Configuration
**File:** `src/main/resources/application.properties`

```properties
spring.ai.groq.api-key=${GROQ_API_KEY}
spring.ai.groq.chat.options.model=mixtral-8x7b-32768
spring.ai.groq.chat.options.temperature=0.7
```

- **Model:** mixtral-8x7b-32768 (can be changed to llama2-70b-4096, etc.)
- **Temperature:** 0.7 (ranges 0-1, affects response creativity)

## Service Features

### ScheduleAnalyzerService
The main service providing three core methods:

1. **analyzeSchedule()** - Full team schedule analysis
2. **analyzeUserSchedule(Long userId)** - Individual user analysis
3. **getSuggestedSchedule(String context)** - Generate optimization suggestions

## Usage Examples

### Spring Boot Application
```java
@Autowired
private ScheduleAnalyzerService analyzerService;

// Get team schedule analysis
String analysis = analyzerService.analyzeSchedule();

// Get user-specific analysis
String userAnalysis = analyzerService.analyzeUserSchedule(1L);

// Get scheduling suggestions
String suggestions = analyzerService.getSuggestedSchedule("15-person team, retail shifts");
```

### Using cURL
```bash
# Windows PowerShell
$env:GROQ_API_KEY = "your-key"
curl -X GET http://localhost:8080/api/schedule-analyzer/analyze

# Get suggestions
curl -X POST -G http://localhost:8080/api/schedule-analyzer/suggest `
  -d "context=Team working 24/7 shifts"
```

## Troubleshooting

### API Key Not Recognized
- Verify the environment variable is set: `echo $env:GROQ_API_KEY`
- Restart the IDE or terminal after setting the variable
- Check that the key is valid in GROQ console

### Build Failures
```powershell
# Clear Maven cache
mvn clean
# Rebuild
mvn install
```

### Connection Issues
- Verify internet connection (GROQ API requires external connectivity)
- Check firewall settings
- Verify GROQ API key has correct permissions

## Next Steps

1. **Integrate into Frontend:** Add buttons/forms to call the analyzer endpoints
2. **Add Caching:** Cache analysis results for frequently accessed data
3. **Schedule Analysis:** Run analysis on a schedule using @Scheduled
4. **Export Results:** Add CSV/PDF export for analysis reports
5. **Real-time Updates:** Add WebSocket support for live analysis

## Dependencies Added

- `spring-ai-groq-spring-boot-starter` - GROQ LLM integration
- `spring-ai-core-spring-boot-starter` - Core Spring AI framework
- `jackson-databind` - JSON processing

## API Documentation

Access Swagger/OpenAPI documentation (if enabled):
```
http://localhost:8080/swagger-ui.html
```

## Support

For issues with Spring-AI, visit:
- [Spring AI Documentation](https://docs.spring.io/spring-ai/docs/current/reference/html/)
- [GROQ Documentation](https://console.groq.com/docs)

---

**Last Updated:** June 14, 2026
**Spring Boot Version:** 3.4.0
**Spring AI Version:** 1.0.0-M1

