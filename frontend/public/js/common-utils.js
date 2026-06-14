/**
 * Common utility functions shared across views
 */

/**
 * Show a confirmation dialog and execute a delete action via fetch
 */
function deleteWithConfirmation(id, endpoint, confirmMessage = 'Are you sure?', isJson = false) {
    if (confirm(confirmMessage)) {
        fetch(endpoint, { 
            method: 'DELETE',
            credentials: 'include',
            headers: isJson ? { 'Content-Type': 'application/json' } : {}
        })
        .then(response => {
            if (response.ok) {
                location.reload();
            } else {
                alert("Delete failed. Please try again.");
            }
        })
        .catch(error => {
            console.error("Error:", error);
            alert("An error occurred during delete.");
        });
    }
}

/**
 * Generic delete handler for users
 */
function deleteUser(id) {
    deleteWithConfirmation(
        id, 
        `/proxy/delete/${id}`,
        'Are you sure you want to delete this user?'
    );
}

/**
 * Generic delete handler for roles
 */
function deleteRole(id) {
    deleteWithConfirmation(
        id,
        `/proxy/roles/delete/${id}`,
        'Are you sure you want to delete this role? This action cannot be undone and may fail if the role is still in use.'
    );
}

/**
 * Toggle role status
 */
function toggleRole(id) {
    fetch(`/proxy/roles/toggle/${id}`, { method: 'PUT', credentials: 'include' })
    .then(response => response.ok ? location.reload() : alert("Failed to toggle role status."))
    .catch(error => {
        console.error("Error:", error);
        alert("An error occurred.");
    });
}

/**
 * Show a toast notification
 */
function showToast(message, type = 'success') {
    const existingToast = document.querySelector('.toast-notification');
    if (existingToast) {
        existingToast.classList.add('hide');
        setTimeout(() => existingToast.remove(), 300);
    }

    const toast = document.createElement('div');
    toast.className = 'toast-notification';
    toast.textContent = message;
    
    if (type === 'error') {
        toast.style.backgroundColor = '#f44336';
    } else if (type === 'warning') {
        toast.style.backgroundColor = '#ff9800';
    }
    
    document.body.appendChild(toast);

    setTimeout(() => {
        toast.classList.add('hide');
        setTimeout(() => toast.remove(), 300);
    }, 3000);
}

/**
 * Show error message in a container
 */
function showError(message, containerId = 'errorContainer') {
    const container = document.getElementById(containerId);
    if (container) {
        container.innerHTML = `<div class="error-message">${message}</div>`;
    }
}

/**
 * Show loading spinner
 */
function showLoading(show = true, containerId = 'loadingContainer') {
    const container = document.getElementById(containerId);
    if (container) {
        container.style.display = show ? 'block' : 'none';
    }
}

// Toast notification CSS (add to page if not present)
if (!document.getElementById('toast-styles')) {
    const styleSheet = document.createElement('style');
    styleSheet.id = 'toast-styles';
    styleSheet.textContent = `
        .toast-notification {
            position: fixed;
            bottom: 20px;
            right: 20px;
            background-color: #4CAF50;
            color: white;
            padding: 16px;
            border-radius: 4px;
            box-shadow: 0 2px 8px rgba(0,0,0,0.2);
            z-index: 9999;
            animation: slideInRight 0.3s ease-out;
            max-width: 400px;
        }
        
        .toast-notification.hide {
            animation: slideOutRight 0.3s ease-out forwards;
        }
        
        @keyframes slideInRight {
            from {
                transform: translateX(100%);
                opacity: 0;
            }
            to {
                transform: translateX(0);
                opacity: 1;
            }
        }
        
        @keyframes slideOutRight {
            from {
                transform: translateX(0);
                opacity: 1;
            }
            to {
                transform: translateX(100%);
                opacity: 0;
            }
        }

        .error-message {
            background-color: #ffebee;
            color: #c62828;
            padding: 12px;
            border-radius: 4px;
            border-left: 4px solid #c62828;
            margin-bottom: 20px;
        }

        .loading-overlay {
            text-align: center;
            padding: 40px;
        }

        .spinner {
            width: 40px;
            height: 40px;
            border: 4px solid #f3f3f3;
            border-top: 4px solid var(--primary-color);
            border-radius: 50%;
            margin: 0 auto 15px;
            animation: spin 1s linear infinite;
        }

        @keyframes spin {
            0% { transform: rotate(0deg); }
            100% { transform: rotate(360deg); }
        }
    `;
    document.head.appendChild(styleSheet);
}

