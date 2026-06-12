const api = require('../services/api');

const isAuthenticated = async (req, res, next) => {
    if (req.session.user) {
        try {
            const response = await api.getUsers(req.session.user);
            const currentUser = response.data.find(u => u.username === req.session.user.username);
            if (currentUser) {
                req.session.currentUser = currentUser;
                return next();
            }
            res.redirect('/login');
        } catch (error) {
            res.redirect('/login');
        }
    } else {
        res.redirect('/login');
    }
};

module.exports = { isAuthenticated };
