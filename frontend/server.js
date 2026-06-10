const express = require('express');
const axios = require('axios');
const session = require('express-session');
const app = express();

app.set('view engine', 'ejs');
app.use(express.urlencoded({ extended: true }));
app.use(express.json());

app.use(session({
    secret: 'your-secret-key',
    resave: false,
    saveUninitialized: false
}));

const SPRING_API = 'http://localhost:8080/api';

// Middleware to check auth and fetch current user info
const isAuthenticated = async (req, res, next) => {
    if (req.session.user) {
        try {
            // Fetch current user info from API
            const response = await axios.get(`${SPRING_API}/users`, { auth: req.session.user });
            // Find the current logged-in user
            const currentUser = response.data.find(u => u.username === req.session.user.username);
            if (currentUser) {
                req.session.currentUser = currentUser;
            }
            next();
        } catch (error) {
            res.redirect('/login');
        }
    } else {
        res.redirect('/login');
    }
};

// Helper function to format last access time
const formatLastAccessTime = (lastAccessTime) => {
    if (!lastAccessTime) return 'Never';
    const date = new Date(lastAccessTime);
    return date.toLocaleString();
};

// Helper function to get the access time to display
const getDisplayAccessTime = (req) => {
    // Show the access time stored at login (the previous login time)
    return formatLastAccessTime(req.session.displayLastAccessTime);
};

app.get('/login', (req, res) => res.render('login'));

app.post('/login', async (req, res) => {
    const { username, password } = req.body;
    try {
        // Verify credentials and get user info
        const response = await axios.get(`${SPRING_API}/users`, { auth: { username, password } });
        const user = response.data.find(u => u.username === username);
        if (user) {
            req.session.user = { username, password };
            req.session.currentUser = user;
            // Store the OLD last access time to display in header throughout this session
            req.session.displayLastAccessTime = user.lastAccessTime || null;
            // Update the database with current time immediately (don't wait for page load)
            await axios.put(`${SPRING_API}/users/${username}/update-access-time`, {}, { auth: { username, password } }).catch(() => {});
            res.redirect('/');
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

// --- Proxy Routes ---

app.get('/', isAuthenticated, async (req, res) => {
    try {
        const response = await axios.get(`${SPRING_API}/users`, { auth: req.session.user });
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
        const rolesResponse = await axios.get(`${SPRING_API}/roles/active`, { auth: req.session.user });
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
        const userResponse = await axios.get(`${SPRING_API}/users/${req.params.id}`, { auth: req.session.user });
        const rolesResponse = await axios.get(`${SPRING_API}/roles/active`, { auth: req.session.user });
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

app.delete('/proxy/delete/:id', isAuthenticated, async (req, res) => {
    try {
        await axios.delete(`${SPRING_API}/users/${req.params.id}`, { auth: req.session.user });
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
        await axios.post(`${SPRING_API}/users`, userData, { auth: req.session.user });
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
            role: {
                id: parseInt(req.body.roleId)
            }
        };
        await axios.put(`${SPRING_API}/users/${req.params.id}`, userData, { auth: req.session.user });
        res.redirect('/');
    } catch (error) {
        res.status(500).send("Update failed");
    }
});

// --- Role Management Routes ---

app.get('/roles', isAuthenticated, async (req, res) => {
    try {
        const response = await axios.get(`${SPRING_API}/roles`, { auth: req.session.user });
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
        const response = await axios.get(`${SPRING_API}/roles/${req.params.id}`, { auth: req.session.user });
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
        await axios.delete(`${SPRING_API}/roles/${req.params.id}`, { auth: req.session.user });
        res.status(204).send();
    } catch (error) {
        res.status(500).send("Delete failed");
    }
});

app.put('/proxy/roles/toggle/:id', isAuthenticated, async (req, res) => {
    try {
        const response = await axios.put(`${SPRING_API}/roles/${req.params.id}/toggle`, {}, { auth: req.session.user });
        res.json(response.data);
    } catch (error) {
        res.status(500).json({ error: "Toggle failed" });
    }
});

app.post('/proxy/roles/add', isAuthenticated, async (req, res) => {
    try {
        await axios.post(`${SPRING_API}/roles`, req.body, { auth: req.session.user });
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
        await axios.put(`${SPRING_API}/roles/${req.params.id}`, roleData, { auth: req.session.user });
        res.redirect('/roles');
    } catch (error) {
        res.status(500).send("Update failed");
    }
});

// --- Scheduler Routes ---

app.get('/scheduler', isAuthenticated, (req, res) => {
    res.render('scheduler', {
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

app.get('/api/proxy/schedule/month/:userId/:year/:month', isAuthenticated, async (req, res) => {
    try {
        const response = await axios.get(`${SPRING_API}/schedule/month/${req.params.userId}/${req.params.year}/${req.params.month}`, { auth: req.session.user });
        res.json(response.data);
    } catch (error) {
        res.status(500).json({ error: "Failed to fetch schedule" });
    }
});

app.get('/api/proxy/schedule/week/:userId', isAuthenticated, async (req, res) => {
    try {
        const response = await axios.get(`${SPRING_API}/schedule/week/${req.params.userId}?date=${req.query.date}`, { auth: req.session.user });
        res.json(response.data);
    } catch (error) {
        res.status(500).json({ error: "Failed to fetch schedule" });
    }
});

app.get('/api/proxy/schedule/day/:userId', isAuthenticated, async (req, res) => {
    try {
        const response = await axios.get(`${SPRING_API}/schedule/day/${req.params.userId}?date=${req.query.date}`, { auth: req.session.user });
        res.json(response.data);
    } catch (error) {
        res.status(500).json({ error: "Failed to fetch schedule" });
    }
});

app.get('/api/proxy/schedule/day/:userId/hours', isAuthenticated, async (req, res) => {
    try {
        const response = await axios.get(`${SPRING_API}/schedule/day/${req.params.userId}/hours?date=${req.query.date}`, { auth: req.session.user });
        res.json(response.data);
    } catch (error) {
        res.status(500).json({ error: "Failed to fetch schedule" });
    }
});

app.get('/api/proxy/schedule/year/:userId/:year', isAuthenticated, async (req, res) => {
    try {
        const response = await axios.get(`${SPRING_API}/schedule/year/${req.params.userId}/${req.params.year}`, { auth: req.session.user });
        res.json(response.data);
    } catch (error) {
        res.status(500).json({ error: "Failed to fetch schedule" });
    }
});

app.post('/api/proxy/schedule/:userId', isAuthenticated, async (req, res) => {
    try {
        const response = await axios.post(`${SPRING_API}/schedule/${req.params.userId}`, req.body, { auth: req.session.user });
        res.json(response.data);
    } catch (error) {
        res.status(500).json({ error: "Failed to update schedule" });
    }
});

app.delete('/api/proxy/schedule/:userId', isAuthenticated, async (req, res) => {
    try {
        await axios.delete(`${SPRING_API}/schedule/${req.params.userId}?date=${req.query.date}`, { auth: req.session.user });
        res.status(204).send();
    } catch (error) {
        res.status(500).json({ error: "Failed to delete schedule" });
    }
});

app.get('/api/proxy/schedule/team/month/:year/:month', isAuthenticated, async (req, res) => {
    try {
        const query = new URLSearchParams();
        if (req.query.userIds) {
            const userIds = Array.isArray(req.query.userIds) ? req.query.userIds : [req.query.userIds];
            userIds.forEach(id => query.append('userIds', id));
        }
        const response = await axios.get(`${SPRING_API}/schedule/team/month/${req.params.year}/${req.params.month}?${query.toString()}`, { auth: req.session.user });
        res.json(response.data);
    } catch (error) {
        res.status(500).json({ error: "Failed to fetch team schedule" });
    }
});

app.get('/api/proxy/schedule/all-users', isAuthenticated, async (req, res) => {
    try {
        const response = await axios.get(`${SPRING_API}/schedule/all-users`, { auth: req.session.user });
        res.json(response.data);
    } catch (error) {
        res.status(500).json({ error: "Failed to fetch users" });
    }
});

app.get('/api/proxy/schedule/daily-summary/:userId', isAuthenticated, async (req, res) => {
    try {
        const date = req.query.date;
        const response = await axios.get(`${SPRING_API}/schedule/daily-summary/${req.params.userId}?date=${date}`, { auth: req.session.user });
        res.json(response.data);
    } catch (error) {
        res.status(500).json({ error: "Failed to fetch daily summary" });
    }
});

app.listen(3000, () => console.log('Frontend running on http://localhost:3000'));