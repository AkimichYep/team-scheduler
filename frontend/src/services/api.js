const axios = require('axios');
const SPRING_API = process.env.SPRING_API || 'http://localhost:8080/api';

/**
 * ============================================================================
 * CORS (Cross-Origin Resource Sharing) Explanation
 * ============================================================================
 *
 * WHY THIS WORKS:
 * ===============
 * This frontend (Node.js) runs at:  http://localhost:3000
 * The backend (Spring Boot) runs at: http://localhost:8080
 *
 * These are DIFFERENT ORIGINS (different ports!)
 * - Origin = Protocol + Domain + Port
 * - Port 3000 ≠ Port 8080 = CROSS-ORIGIN REQUEST
 *
 * Browser BLOCKS cross-origin requests by default (security feature)
 * This is the "Same-Origin Policy"
 *
 * How CORS fixes this:
 * 1. Browser detects cross-origin request
 * 2. Browser sends Origin header: "Origin: http://localhost:3000"
 * 3. Spring Backend receives request
 * 4. Spring checks: "Is http://localhost:3000 in my allowed origins?"
 * 5. If YES: Spring sends "Access-Control-Allow-Origin: http://localhost:3000"
 * 6. Browser receives this header and allows JavaScript to access response ✅
 * 7. If NO: Browser blocks with CORS error ❌
 *
 * THE `auth` PARAMETER:
 * ====================
 * Every function below receives an `auth` object:
 *
 *   axios.get(url, { auth })
 *
 * Axios automatically converts this:
 *   { auth: { username: 'john', password: 'pass123' } }
 *
 * Into this header:
 *   Authorization: "Basic base64(john:pass123)"
 *
 * This is HTTP Basic Authentication. Spring receives it and knows who you are.
 *
 * CORS + AUTHENTICATION TOGETHER:
 * ===============================
 * When you make a cross-origin request with credentials:
 *
 * Request Flow:
 * ├─ Browser detects cross-origin
 * ├─ Axios adds Authorization header (credentials)
 * ├─ Browser sends request with Origin header
 * └─ For PUT/DELETE, browser first sends OPTIONS preflight request
 *
 * Spring Response:
 * ├─ Spring receives request
 * ├─ Checks CORS: Is http://localhost:3000 allowed? YES
 * ├─ Checks Auth: Valid username/password? YES
 * ├─ Sends response with Access-Control-Allow-Origin header
 * └─ Browser allows access ✅
 *
 * PREFLIGHT REQUESTS (PUT, DELETE):
 * =================================
 * When browser needs to send PUT or DELETE:
 *
 * Step 1: Browser asks permission with OPTIONS:
 *   OPTIONS /api/users/1
 *   Origin: http://localhost:3000
 *   Access-Control-Request-Method: PUT
 *
 * Step 2: Spring responds (OPTIONS doesn't need authentication):
 *   HTTP 200 OK
 *   Access-Control-Allow-Origin: http://localhost:3000
 *   Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS
 *
 * Step 3: Browser gets green light, sends actual PUT request:
 *   PUT /api/users/1
 *   Authorization: Basic ...
 *   (actual data)
 *
 * Step 4: Spring processes and responds normally
 *
 * WHAT'S CONFIGURED IN SPRING (SecurityConfig.java):
 * ===================================================
 * ✅ CORS enabled:
 *    .cors(cors -> cors.configurationSource(corsConfigurationSource()))
 *
 * ✅ OPTIONS allowed without authentication (for preflight):
 *    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
 *
 * ✅ Allowed origins configured:
 *    configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000"))
 *
 * ✅ Credentials allowed:
 *    configuration.setAllowCredentials(true)
 *
 * ✅ All HTTP methods allowed:
 *    configuration.setAllowedMethods(Arrays.asList(
 *        "GET", "POST", "PUT", "DELETE", "OPTIONS"
 *    ))
 *
 * THIS IS WHY EVERY FUNCTION HERE WORKS! ✅
 * ==========================================
 * Each call below works because:
 * 1. Axios automatically handles cross-origin with correct headers
 * 2. Spring is configured to accept requests from http://localhost:3000
 * 3. Spring allows all HTTP methods (GET, POST, PUT, DELETE)
 * 4. Basic Auth credentials are sent and accepted
 * 5. CORS headers are returned, browser allows access
 *
 * ============================================================================
 */

const api = {
    getUsers: (auth) => axios.get(`${SPRING_API}/users`, { auth }),
    getUser: (id, auth) => axios.get(`${SPRING_API}/users/${id}`, { auth }),
    addUser: (userData, auth) => axios.post(`${SPRING_API}/users`, userData, { auth }),
    updateUser: (id, userData, auth) => axios.put(`${SPRING_API}/users/${id}`, userData, { auth }),
    deleteUser: (id, auth) => axios.delete(`${SPRING_API}/users/${id}`, { auth }),
    updateAccessTime: (username, auth) => axios.put(`${SPRING_API}/users/${username}/update-access-time`, {}, { auth }),
    
    getRoles: (auth) => axios.get(`${SPRING_API}/roles`, { auth }),
    getActiveRoles: (auth) => axios.get(`${SPRING_API}/roles/active`, { auth }),
    getRole: (id, auth) => axios.get(`${SPRING_API}/roles/${id}`, { auth }),
    addRole: (roleData, auth) => axios.post(`${SPRING_API}/roles`, roleData, { auth }),
    updateRole: (id, roleData, auth) => axios.put(`${SPRING_API}/roles/${id}`, roleData, { auth }),
    deleteRole: (id, auth) => axios.delete(`${SPRING_API}/roles/${id}`, { auth }),
    
    getSchedules: (params, auth) => axios.get(`${SPRING_API}/schedules`, { params, auth }),
    addSchedule: (scheduleData, auth) => axios.post(`${SPRING_API}/schedules`, scheduleData, { auth }),
    deleteSchedule: (id, auth) => axios.delete(`${SPRING_API}/schedules/${id}`, { auth }),
    
    getTemplates: (auth) => axios.get(`${SPRING_API}/templates`, { auth }),
    addTemplate: (templateData, auth) => axios.post(`${SPRING_API}/templates`, templateData, { auth }),
    deleteTemplate: (id, auth) => axios.delete(`${SPRING_API}/templates/${id}`, { auth }),
    applyTemplate: (id, params, auth) => axios.post(`${SPRING_API}/templates/${id}/apply`, {}, { params, auth }),
    
    getSummary: (params, auth) => axios.get(`${SPRING_API}/schedules/summary`, { params, auth }),
    getDailySummary: (params, auth) => axios.get(`${SPRING_API}/schedules/daily-summary`, { params, auth }),
    getSummaryByDay: (params, auth) => axios.get(`${SPRING_API}/schedules/summary-by-day`, { params, auth }),

    // Schedule specific routes (Legacy/Custom structure)
    getScheduleMonth: (userId, year, month, auth) => axios.get(`${SPRING_API}/schedule/month/${userId}/${year}/${month}`, { auth }),
    getScheduleWeek: (userId, date, auth) => axios.get(`${SPRING_API}/schedule/week/${userId}`, { params: { date }, auth }),
    getScheduleDay: (userId, date, auth) => axios.get(`${SPRING_API}/schedule/day/${userId}`, { params: { date }, auth }),
    getScheduleDayHours: (userId, date, auth) => axios.get(`${SPRING_API}/schedule/day/${userId}/hours`, { params: { date }, auth }),
    getScheduleYear: (userId, year, auth) => axios.get(`${SPRING_API}/schedule/year/${userId}/${year}`, { auth }),
    updateSchedule: (userId, data, auth) => axios.post(`${SPRING_API}/schedule/${userId}`, data, { auth }),
    deleteScheduleByDate: (userId, date, auth) => axios.delete(`${SPRING_API}/schedule/${userId}`, { params: { date }, auth }),
    
    getTeamScheduleMonth: (year, month, userIds, auth) => {
        const query = new URLSearchParams();
        if (userIds) {
            const ids = Array.isArray(userIds) ? userIds : [userIds];
            ids.forEach(id => query.append('userIds', id));
        }
        return axios.get(`${SPRING_API}/schedule/team/month/${year}/${month}?${query.toString()}`, { auth });
    },
    
    getTeamScheduleWeek: (startDate, userIds, auth) => {
        const query = new URLSearchParams();
        if (userIds) {
            const ids = Array.isArray(userIds) ? userIds : [userIds];
            ids.forEach(id => query.append('userIds', id));
        }
        return axios.get(`${SPRING_API}/schedule/team/week/${startDate}?${query.toString()}`, { auth });
    },
    
    getTeamSummaryByDay: (year, month, userIds, auth) => {
        const query = new URLSearchParams();
        if (userIds) {
            const ids = Array.isArray(userIds) ? userIds : [userIds];
            ids.forEach(id => query.append('userIds', id));
        }
        return axios.get(`${SPRING_API}/schedule/team/summary-by-day/${year}/${month}?${query.toString()}`, { auth });
    },
    
    getAllUsers: (auth) => axios.get(`${SPRING_API}/schedule/all-users`, { auth }),
    
    getDailySummaryDirect: (userId, date, auth) => axios.get(`${SPRING_API}/schedule/daily-summary/${userId}`, { params: { date }, auth }),
    
    // Templates
    getTemplates: (auth) => axios.get(`${SPRING_API}/templates`, { auth }),
    getTemplate: (id, auth) => axios.get(`${SPRING_API}/templates/${id}`, { auth }),
    getDefaultTemplate: (auth) => axios.get(`${SPRING_API}/templates/default/template`, { auth }),
    getUserDefaultTemplate: (userId, auth) => axios.get(`${SPRING_API}/templates/user/${userId}/default`, { auth }),
    addTemplate: (data, auth) => axios.post(`${SPRING_API}/templates`, data, { auth }),
    applyTemplateToDate: (userId, templateId, date, auth) => axios.post(`${SPRING_API}/templates/apply-to-date/${userId}/${templateId}`, {}, { params: { date }, auth }),
    applyTemplateToRange: (userId, templateId, startDate, endDate, auth) => axios.post(`${SPRING_API}/templates/apply-to-range/${userId}/${templateId}`, {}, { params: { startDate, endDate }, auth }),
    setDefaultTemplate: (userId, templateId, auth) => axios.post(`${SPRING_API}/templates/set-default/${userId}/${templateId}`, {}, { auth }),
    addOnCall: (userId, data, auth) => axios.post(`${SPRING_API}/templates/oncall/add/${userId}`, data, { auth }),
    removeOnCall: (userId, data, auth) => axios.post(`${SPRING_API}/templates/oncall/remove/${userId}`, data, { auth }),
    deleteTemplate: (id, auth) => axios.delete(`${SPRING_API}/templates/${id}`, { auth }),
    
    getTeamSummaryByDayDirectV2: (year, month, userIds, auth) => {
        const query = new URLSearchParams();
        if (userIds) {
            const ids = Array.isArray(userIds) ? userIds : [userIds];
            ids.forEach(id => query.append('userIds', id));
        }
        return axios.get(`${SPRING_API}/schedule/team/summary-by-day/${year}/${month}?${query.toString()}`, { auth });
    },

    // ============ CHAT API ============

    /**
     * Creates a new chat conversation
     */
    createChatConversation(userId, title, context, auth) {
        const params = new URLSearchParams({ userId, title, context });
        return axios.post(`${SPRING_API}/chat/conversations?${params.toString()}`, {}, { auth });
    },

    /**
     * Gets all conversations for a user
     */
    getChatConversations(userId, auth) {
        return axios.get(`${SPRING_API}/chat/conversations?userId=${userId}`, { auth });
    },

    /**
     * Gets a specific conversation
     */
    getChatConversation(conversationId, auth) {
        return axios.get(`${SPRING_API}/chat/conversations/${conversationId}`, { auth });
    },

    /**
     * Gets conversation message history
     */
    getChatMessages(conversationId, auth) {
        return axios.get(`${SPRING_API}/chat/conversations/${conversationId}/messages`, { auth });
    },

    /**
     * Sends a message and gets AI response
     */
    sendChatMessage(userId, conversationId, content, context, auth) {
        return axios.post(`${SPRING_API}/chat/messages?userId=${userId}`, {
            conversation_id: conversationId,
            content: content,
            context: context
        }, { auth });
    },

    /**
     * Archives a conversation
     */
    archiveChatConversation(conversationId, auth) {
        return axios.post(`${SPRING_API}/chat/conversations/${conversationId}/archive`, {}, { auth });
    },

    /**
     * Chat health check
     */
    checkChatHealth() {
        return axios.get(`${SPRING_API}/chat/health`);
    }
};

module.exports = api;
