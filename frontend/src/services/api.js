const axios = require('axios');
const SPRING_API = process.env.SPRING_API || 'http://localhost:8080/api';

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
    }
};

module.exports = api;
