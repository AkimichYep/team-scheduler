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

app.get('/add', isAuthenticated, (req, res) => {
    res.render('add');
});

app.get('/edit/:id', isAuthenticated, async (req, res) => {
    try {
        const response = await axios.get(`${SPRING_API}/users/${req.params.id}`, { auth: req.session.user });
        res.render('edit', { user: response.data });
    } catch (error) {
        res.status(500).send("Error fetching user");
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
        await axios.post(`${SPRING_API}/users`, req.body, { auth: req.session.user });
        res.redirect('/');
    } catch (error) {
        res.status(500).send("Create failed");
    }
});

app.post('/proxy/edit/:id', isAuthenticated, async (req, res) => {
    try {
        await axios.put(`${SPRING_API}/users/${req.params.id}`, req.body, { auth: req.session.user });
        res.redirect('/');
    } catch (error) {
        res.status(500).send("Update failed");
    }
});

app.listen(3000, () => console.log('Frontend running on http://localhost:3000'));