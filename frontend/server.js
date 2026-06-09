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

// Middleware to check auth
const isAuthenticated = (req, res, next) => {
    if (req.session.user) next();
    else res.redirect('/login');
};

app.get('/login', (req, res) => res.render('login'));

app.post('/login', (req, res) => {
    const { username, password } = req.body;
    req.session.user = { username, password };
    res.redirect('/');
});

app.get('/logout', (req, res) => {
    req.session.destroy();
    res.redirect('/login');
});

// --- Proxy Routes ---

app.get('/', isAuthenticated, async (req, res) => {
    try {
        const response = await axios.get(`${SPRING_API}/users`, { auth: req.session.user });
        res.render('users', { users: response.data });
    } catch (error) {
        res.status(500).send("Error fetching users");
    }
});

app.get('/add', isAuthenticated, async (req, res) => {
    try {
        const rolesResponse = await axios.get(`${SPRING_API}/roles/active`, { auth: req.session.user });
        res.render('add', { roles: rolesResponse.data });
    } catch (error) {
        res.status(500).send("Error fetching roles");
    }
});

app.get('/edit/:id', isAuthenticated, async (req, res) => {
    try {
        const userResponse = await axios.get(`${SPRING_API}/users/${req.params.id}`, { auth: req.session.user });
        const rolesResponse = await axios.get(`${SPRING_API}/roles/active`, { auth: req.session.user });
        res.render('edit', { user: userResponse.data, roles: rolesResponse.data });
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
        res.render('roles', { roles: response.data });
    } catch (error) {
        res.status(500).send("Error fetching roles");
    }
});

app.get('/roles/add', isAuthenticated, (req, res) => {
    res.render('roles-add');
});

app.get('/roles/edit/:id', isAuthenticated, async (req, res) => {
    try {
        const response = await axios.get(`${SPRING_API}/roles/${req.params.id}`, { auth: req.session.user });
        res.render('roles-edit', { role: response.data });
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

app.listen(3000, () => console.log('Frontend running on http://localhost:3000'));