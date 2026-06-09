const express = require('express');
const session = require('express-session');
const axios = require('axios');
const app = express();

app.set('view engine', 'ejs');
app.use(express.urlencoded({ extended: true }));
app.use(session({ secret: 'my-secret-key', resave: false, saveUninitialized: true }));

// Login Page
app.get('/login', (req, res) => res.render('login'));

// Handle Login Form Submission
app.post('/login', (req, res) => {
    const { username, password } = req.body;
    // Store credentials in session
    req.session.user = { username, password };
    res.redirect('/');
});

// Logout
app.get('/logout', (req, res) => {
    req.session.destroy();
    res.redirect('/login');
});

// Dashboard
app.get('/', async (req, res) => {
    if (!req.session.user) return res.redirect('/login');

    try {
        const response = await axios.get('http://localhost:8080/api/users', {
            auth: req.session.user // Use session credentials
        });

        // In a real app, determine role from the user object or a separate API call
        res.render('users', {
            users: response.data,
            currentUserRole: 'ADMIN' // Replace with logic to identify current role
        });
    } catch (error) {
        res.status(401).send("Unauthorized or API error");
    }
});

app.listen(3000, () => console.log('Server running on http://localhost:3000'));