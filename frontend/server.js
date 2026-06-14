const express = require('express');
const path = require('path');
const session = require('express-session');
const api = require('./src/services/api');
const { isAuthenticated } = require('./src/middleware/auth');

const app = express();

app.set('view engine', 'ejs');
app.set('views', path.join(__dirname, 'views'));

app.use(express.static(path.join(__dirname, 'public')));
app.use(express.urlencoded({ extended: true }));
app.use(express.json());

app.use(session({
    secret: 'your-secret-key',
    resave: false,
    saveUninitialized: false
}));

// Helper function to format last access time
const formatLastAccessTime = (lastAccessTime) => {
    if (!lastAccessTime) return 'Never';
    const date = new Date(lastAccessTime);
    return date.toLocaleString();
};

// Helper function to get the access time to display
const getDisplayAccessTime = (req) => {
    return formatLastAccessTime(req.session.displayLastAccessTime);
};

app.get('/login', (req, res) => res.render('login'));

app.post('/login', async (req, res) => {
    const { username, password } = req.body;
    try {
        const response = await api.getUsers({ username, password });
        const user = response.data.find(u => u.username === username);
        if (user) {
            req.session.user = { username, password };
            req.session.currentUser = user;
            req.session.userId = user.id; // Store userId in session
            req.session.displayLastAccessTime = user.lastAccessTime || null;
            
            await api.updateAccessTime(username, { username, password }).catch(() => {});
            res.redirect('/summary');
        } else {
            res.status(401).send("Invalid credentials");
        }
    } catch (error) {
        res.status(401).send("Invalid credentials");
    }
});

app.get('/logout', (req, res) => {
    req.session.destroy();
    res.redirect('/login');
});

app.use((req, res, next) => {
    res.locals.userId = req.session.userId || null;
    res.locals.currentUser = req.session.currentUser || null;
    next();
});

// --- Main Views ---

app.get('/', isAuthenticated, async (req, res) => {
    try {
        const response = await api.getUsers(req.session.user);
        res.render('users', {
            users: response.data,
            currentUser: req.session.currentUser,
            lastAccessTime: getDisplayAccessTime(req)
        });
    } catch (error) {
        res.status(500).send("Error fetching users");
    }
});

app.get('/add', isAuthenticated, async (req, res) => {
    try {
        const rolesResponse = await api.getActiveRoles(req.session.user);
        res.render('add', {
            roles: rolesResponse.data,
            currentUser: req.session.currentUser,
            lastAccessTime: getDisplayAccessTime(req)
        });
    } catch (error) {
        res.status(500).send("Error fetching roles");
    }
});

app.get('/edit/:id', isAuthenticated, async (req, res) => {
    try {
        const userResponse = await api.getUser(req.params.id, req.session.user);
        const rolesResponse = await api.getActiveRoles(req.session.user);
        res.render('edit', {
            user: userResponse.data,
            roles: rolesResponse.data,
            currentUser: req.session.currentUser,
            lastAccessTime: getDisplayAccessTime(req)
        });
    } catch (error) {
        res.status(500).send("Error fetching data");
    }
});

// --- User Management API Proxy ---

app.delete('/proxy/delete/:id', isAuthenticated, async (req, res) => {
    try {
        await api.deleteUser(req.params.id, req.session.user);
        res.status(204).send();
    } catch (error) {
        res.status(500).send("Delete failed");
    }
});

app.post('/proxy/add', isAuthenticated, async (req, res) => {
    try {
        const userData = {
            username: req.body.username,
            password: req.body.password,
            roleId: parseInt(req.body.roleId),
            project: req.body.project
        };
        await api.addUser(userData, req.session.user);
        res.redirect('/');
    } catch (error) {
        res.status(500).send("Create failed");
    }
});

app.post('/proxy/edit/:id', isAuthenticated, async (req, res) => {
    try {
        const userData = {
            username: req.body.username,
            active: req.body.active === 'on',
            project: req.body.project,
            role: { id: parseInt(req.body.roleId) }
        };
        await api.updateUser(req.params.id, userData, req.session.user);
        res.redirect('/');
    } catch (error) {
        res.status(500).send("Update failed");
    }
});

// --- Role Management ---

app.get('/roles', isAuthenticated, async (req, res) => {
    try {
        const response = await api.getRoles(req.session.user);
        res.render('roles', {
            roles: response.data,
            currentUser: req.session.currentUser,
            lastAccessTime: getDisplayAccessTime(req)
        });
    } catch (error) {
        res.status(500).send("Error fetching roles");
    }
});

app.get('/roles/add', isAuthenticated, (req, res) => {
    res.render('roles-add', {
        currentUser: req.session.currentUser,
        lastAccessTime: getDisplayAccessTime(req)
    });
});

app.get('/roles/edit/:id', isAuthenticated, async (req, res) => {
    try {
        const response = await api.getRole(req.params.id, req.session.user);
        res.render('roles-edit', {
            role: response.data,
            currentUser: req.session.currentUser,
            lastAccessTime: getDisplayAccessTime(req)
        });
    } catch (error) {
        res.status(500).send("Error fetching role");
    }
});

app.delete('/proxy/roles/delete/:id', isAuthenticated, async (req, res) => {
    try {
        await api.deleteRole(req.params.id, req.session.user);
        res.status(204).send();
    } catch (error) {
        res.status(500).send("Delete failed");
    }
});

app.put('/proxy/roles/toggle/:id', isAuthenticated, async (req, res) => {
    try {
        const response = await api.getRole(req.params.id, req.session.user);
        const role = response.data;
        role.active = !role.active;
        await api.updateRole(req.params.id, role, req.session.user);
        res.json(role);
    } catch (error) {
        res.status(500).json({ error: "Toggle failed" });
    }
});

app.post('/proxy/roles/add', isAuthenticated, async (req, res) => {
    try {
        await api.addRole(req.body, req.session.user);
        res.redirect('/roles');
    } catch (error) {
        res.status(500).send("Create failed");
    }
});

app.post('/proxy/roles/edit/:id', isAuthenticated, async (req, res) => {
    try {
        const roleData = {
            name: req.body.name,
            description: req.body.description,
            active: req.body.active === 'on'
        };
        await api.updateRole(req.params.id, roleData, req.session.user);
        res.redirect('/roles');
    } catch (error) {
        res.status(500).send("Update failed");
    }
});

// --- Scheduler Views ---

app.get('/scheduler', isAuthenticated, (req, res) => {
    res.render('scheduler', {
        currentUser: req.session.currentUser,
        lastAccessTime: getDisplayAccessTime(req),
        userId: req.session.currentUser.id
    });
});

app.get('/schedule-templates', isAuthenticated, (req, res) => {
    res.render('schedule-templates', {
        currentUser: req.session.currentUser,
        lastAccessTime: getDisplayAccessTime(req),
        userId: req.session.currentUser.id
    });
});

app.get('/summary', isAuthenticated, (req, res) => {
    res.render('summary', {
        currentUser: req.session.currentUser,
        lastAccessTime: getDisplayAccessTime(req)
    });
});

app.get('/daily-summary', isAuthenticated, (req, res) => {
    res.render('daily-summary', {
        currentUser: req.session.currentUser,
        lastAccessTime: getDisplayAccessTime(req),
        userId: req.session.currentUser.id
    });
});

app.get('/summary-by-day', isAuthenticated, (req, res) => {
    res.render('summary-by-day', {
        currentUser: req.session.currentUser,
        lastAccessTime: getDisplayAccessTime(req)
    });
});

// --- AI Chat ---

app.get('/chat', isAuthenticated, (req, res) => {
    res.render('chat', {
        currentUser: req.session.currentUser,
        lastAccessTime: getDisplayAccessTime(req),
        userId: req.session.currentUser.id
    });
});

// --- Scheduler & Team API Proxy ---

app.get('/api/proxy/schedule/team/summary-by-day/:year/:month', isAuthenticated, async (req, res) => {
    try {
        const response = await api.getTeamSummaryByDay(req.params.year, req.params.month, req.query.userIds, req.session.user);
        res.json(response.data);
    } catch (error) {
        res.status(500).json({ error: "Failed to fetch team summary" });
    }
});

app.get('/api/proxy/schedule/month/:userId/:year/:month', isAuthenticated, async (req, res) => {
    try {
        const response = await api.getScheduleMonth(req.params.userId, req.params.year, req.params.month, req.session.user);
        res.json(response.data);
    } catch (error) {
        res.status(500).json({ error: "Failed to fetch schedule" });
    }
});

app.get('/api/proxy/schedule/week/:userId', isAuthenticated, async (req, res) => {
    try {
        const response = await api.getScheduleWeek(req.params.userId, req.query.date, req.session.user);
        res.json(response.data);
    } catch (error) {
        res.status(500).json({ error: "Failed to fetch schedule" });
    }
});

app.get('/api/proxy/schedule/day/:userId', isAuthenticated, async (req, res) => {
    try {
        const response = await api.getScheduleDay(req.params.userId, req.query.date, req.session.user);
        res.json(response.data);
    } catch (error) {
        res.status(500).json({ error: "Failed to fetch schedule" });
    }
});

app.get('/api/proxy/schedule/day/:userId/hours', isAuthenticated, async (req, res) => {
    try {
        const response = await api.getScheduleDayHours(req.params.userId, req.query.date, req.session.user);
        res.json(response.data);
    } catch (error) {
        res.status(500).json({ error: "Failed to fetch schedule" });
    }
});

app.get('/api/proxy/schedule/year/:userId/:year', isAuthenticated, async (req, res) => {
    try {
        const response = await api.getScheduleYear(req.params.userId, req.params.year, req.session.user);
        res.json(response.data);
    } catch (error) {
        res.status(500).json({ error: "Failed to fetch schedule" });
    }
});

app.post('/api/proxy/schedule/:userId', isAuthenticated, async (req, res) => {
    try {
        const response = await api.updateSchedule(req.params.userId, req.body, req.session.user);
        res.json(response.data);
    } catch (error) {
        res.status(500).json({ error: "Failed to update schedule" });
    }
});

app.delete('/api/proxy/schedule/:userId', isAuthenticated, async (req, res) => {
    try {
        await api.deleteScheduleByDate(req.params.userId, req.query.date, req.session.user);
        res.status(204).send();
    } catch (error) {
        res.status(500).json({ error: "Failed to delete schedule" });
    }
});

app.get('/api/proxy/schedule/team/month/:year/:month', isAuthenticated, async (req, res) => {
    try {
        const response = await api.getTeamScheduleMonth(req.params.year, req.params.month, req.query.userIds, req.session.user);
        res.json(response.data);
    } catch (error) {
        res.status(500).json({ error: "Failed to fetch team schedule" });
    }
});

app.get('/api/proxy/schedule/team/week/:startDate', isAuthenticated, async (req, res) => {
    try {
        const response = await api.getTeamScheduleWeek(req.params.startDate, req.query.userIds, req.session.user);
        res.json(response.data);
    } catch (error) {
        res.status(500).json({ error: "Failed to fetch team schedule" });
    }
});

app.get('/api/proxy/schedule/all-users', isAuthenticated, async (req, res) => {
    try {
        const response = await api.getAllUsers(req.session.user);
        res.json(response.data);
    } catch (error) {
        res.status(500).json({ error: "Failed to fetch users" });
    }
});

app.get('/api/proxy/schedule/daily-summary/:userId', isAuthenticated, async (req, res) => {
    try {
        const response = await api.getDailySummaryDirect(req.params.userId, req.query.date, req.session.user);
        res.json(response.data);
    } catch (error) {
        res.status(500).json({ error: "Failed to fetch daily summary" });
    }
});

// --- Schedule Templates API Proxy ---

app.get('/api/proxy/templates', isAuthenticated, async (req, res) => {
    try {
        const response = await api.getTemplates(req.session.user);
        res.json(response.data);
    } catch (error) {
        res.status(500).json({ error: "Failed to fetch templates" });
    }
});

app.get('/api/proxy/templates/:id', isAuthenticated, async (req, res) => {
    try {
        const response = await api.getTemplate(req.params.id, req.session.user);
        res.json(response.data);
    } catch (error) {
        res.status(500).json({ error: "Failed to fetch template" });
    }
});

app.get('/api/proxy/templates/default/template', isAuthenticated, async (req, res) => {
    try {
        const response = await api.getDefaultTemplate(req.session.user);
        res.json(response.data);
    } catch (error) {
        res.status(404).json({ error: "No default template found" });
    }
});

app.get('/api/proxy/templates/user/:userId/default', isAuthenticated, async (req, res) => {
    try {
        const response = await api.getUserDefaultTemplate(req.params.userId, req.session.user);
        res.json(response.data);
    } catch (error) {
        res.status(404).json({ error: "No default template set for user" });
    }
});

app.post('/api/proxy/templates', isAuthenticated, async (req, res) => {
    try {
        const response = await api.addTemplate(req.body, req.session.user);
        res.json(response.data);
    } catch (error) {
        res.status(500).json({ error: "Failed to create template" });
    }
});

app.post('/api/proxy/templates/apply-to-date/:userId/:templateId', isAuthenticated, async (req, res) => {
    try {
        const response = await api.applyTemplateToDate(req.params.userId, req.params.templateId, req.query.date, req.session.user);
        res.json(response.data);
    } catch (error) {
        res.status(500).json({ error: "Failed to apply template" });
    }
});

app.post('/api/proxy/templates/apply-to-range/:userId/:templateId', isAuthenticated, async (req, res) => {
    try {
        const response = await api.applyTemplateToRange(req.params.userId, req.params.templateId, req.query.startDate, req.query.endDate, req.session.user);
        res.json(response.data);
    } catch (error) {
        res.status(500).json({ error: "Failed to apply template" });
    }
});

app.post('/api/proxy/templates/set-default/:userId/:templateId', isAuthenticated, async (req, res) => {
    try {
        const response = await api.setDefaultTemplate(req.params.userId, req.params.templateId, req.session.user);
        res.json(response.data);
    } catch (error) {
        res.status(500).json({ error: "Failed to set default template" });
    }
});

app.post('/api/proxy/templates/oncall/add/:userId', isAuthenticated, async (req, res) => {
    try {
        const response = await api.addOnCall(req.params.userId, req.body, req.session.user);
        res.json(response.data);
    } catch (error) {
        res.status(500).json({ error: "Failed to add OnCall" });
    }
});

app.post('/api/proxy/templates/oncall/remove/:userId', isAuthenticated, async (req, res) => {
    try {
        const response = await api.removeOnCall(req.params.userId, req.body, req.session.user);
        res.json(response.data);
    } catch (error) {
        res.status(500).json({ error: "Failed to remove OnCall" });
    }
});

// --- Chat API Proxy ---

app.post('/api/proxy/chat/conversations', isAuthenticated, async (req, res) => {
    try {
        const { title, context } = req.body;
        console.log('[CHAT] Creating conversation:', { title, context, userId: req.session.currentUser.id });
        console.log('[CHAT] Using auth:', req.session.user);
        const response = await api.createChatConversation(req.session.currentUser.id, title, context, req.session.user);
        console.log('[CHAT] Response:', response.status, response.data);
        res.json(response.data);
    } catch (error) {
        console.error('[CHAT] Error creating conversation:');
        console.error('  Status:', error.response?.status);
        console.error('  Data:', error.response?.data);
        console.error('  Message:', error.message);
        res.status(500).json({
            error: "Failed to create conversation",
            details: error.response?.data || error.message,
            backendStatus: error.response?.status
        });
    }
});

app.get('/api/proxy/chat/conversations', isAuthenticated, async (req, res) => {
    try {
        const userId = req.query.userId || req.session.currentUser.id;
        console.log('[CHAT] Getting conversations for user:', userId);
        const response = await api.getChatConversations(userId, req.session.user);
        console.log('[CHAT] Response:', response.status, 'conversations:', response.data?.length);
        res.json(response.data);
    } catch (error) {
        console.error('[CHAT] Error getting conversations:');
        console.error('  Status:', error.response?.status);
        console.error('  Data:', error.response?.data);
        console.error('  Message:', error.message);
        res.status(500).json({
            error: "Failed to get conversations",
            details: error.response?.data || error.message,
            backendStatus: error.response?.status
        });
    }
});

app.get('/api/proxy/chat/conversations/:conversationId', isAuthenticated, async (req, res) => {
    try {
        console.log('[CHAT] Getting conversation:', req.params.conversationId);
        const response = await api.getChatConversation(req.params.conversationId, req.session.user);
        res.json(response.data);
    } catch (error) {
        console.error('[CHAT] Error getting conversation:', error.message);
        res.status(500).json({
            error: "Failed to get conversation",
            details: error.response?.data || error.message
        });
    }
});

app.get('/api/proxy/chat/conversations/:conversationId/messages', isAuthenticated, async (req, res) => {
    try {
        console.log('[CHAT] Getting messages for conversation:', req.params.conversationId);
        const response = await api.getChatMessages(req.params.conversationId, req.session.user);
        res.json(response.data);
    } catch (error) {
        console.error('[CHAT] Error getting messages:', error.message);
        res.status(500).json({
            error: "Failed to get messages",
            details: error.response?.data || error.message
        });
    }
});

app.post('/api/proxy/chat/messages', isAuthenticated, async (req, res) => {
    try {
        const { conversationId, content, context } = req.body;
        console.log('[CHAT] Sending message:', { conversationId, content: content.substring(0, 50) + '...' });
        const response = await api.sendChatMessage(req.session.currentUser.id, conversationId, content, context, req.session.user);
        res.json(response.data);
    } catch (error) {
        console.error('[CHAT] Error sending message:', error.message);
        res.status(500).json({
            error: "Failed to send message",
            details: error.response?.data || error.message
        });
    }
});

app.post('/api/proxy/chat/conversations/:conversationId/archive', isAuthenticated, async (req, res) => {
    try {
        console.log('[CHAT] Archiving conversation:', req.params.conversationId);
        const response = await api.archiveChatConversation(req.params.conversationId, req.session.user);
        res.json(response.data);
    } catch (error) {
        console.error('[CHAT] Error archiving conversation:', error.message);
        res.status(500).json({
            error: "Failed to archive conversation",
            details: error.response?.data || error.message
        });
    }
});

app.listen(3000, () => console.log('Frontend running on http://localhost:3000'));
