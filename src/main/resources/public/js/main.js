// main.js
// Shared functions for login, register, rules, accounts, admin

// ---------------- AUTH ----------------
function register() {
    const username = document.getElementById("username").value;
    const password = document.getElementById("password").value;

    fetch("/register", {
        method: "POST",
        headers: {"Content-Type": "application/json"},
        body: JSON.stringify({username, password})
    }).then(res => res.text())
        .then(alert);
}

function login() {
    const username = document.getElementById("username").value;
    const password = document.getElementById("password").value;

    fetch("/login", {
        method: "POST",
        headers: {"Content-Type": "application/json"},
        body: JSON.stringify({username, password})
    })
        .then(res => res.json())
        .then(data => {
            localStorage.setItem("currentToken", data.token);
            localStorage.setItem("currentUser", username);
            localStorage.setItem("isAdmin", data.isAdmin);

            // save token in accounts list
            let accounts = JSON.parse(localStorage.getItem("accounts") || "[]");
            if (!accounts.some(acc => acc.username === username)) {
                accounts.push({username, token: data.token, isAdmin: data.isAdmin});
                localStorage.setItem("accounts", JSON.stringify(accounts));
            }

            if(data.isAdmin) window.location.href = "admin.html";
            else window.location.href = "dashboard.html";
        })
        .catch(err => alert("Login failed"));
}

function logout() {
    localStorage.removeItem("currentToken");
    localStorage.removeItem("currentUser");
    localStorage.removeItem("isAdmin");
    window.location.href = "index.html";
}

// ---------------- ACCOUNTS ----------------
function loadAccounts() {
    const accounts = JSON.parse(localStorage.getItem("accounts") || "[]");
    const ul = document.getElementById("accounts-list");
    if (!ul) return;

    ul.innerHTML = "";
    accounts.forEach(acc => {
        const li = document.createElement("li");
        li.innerHTML = `
            ${acc.username} ${acc.isAdmin ? '(Admin)' : ''}
            <button onclick="switchAccount('${acc.token}')">Switch</button>
            <button onclick="removeAccount('${acc.token}')">Remove</button>
        `;
        ul.appendChild(li);
    });
}

function switchAccount(token) {
    const accounts = JSON.parse(localStorage.getItem("accounts") || "[]");
    const account = accounts.find(acc => acc.token === token);
    if(account) {
        localStorage.setItem("currentToken", account.token);
        localStorage.setItem("currentUser", account.username);
        localStorage.setItem("isAdmin", account.isAdmin);
        if(account.isAdmin) window.location.href = "admin.html";
        else window.location.href = "dashboard.html";
    }
}

function removeAccount(token) {
    let accounts = JSON.parse(localStorage.getItem("accounts") || "[]");
    accounts = accounts.filter(acc => acc.token !== token);
    localStorage.setItem("accounts", JSON.stringify(accounts));

    // if current account removed, log out
    if(localStorage.getItem("currentToken") === token) logout();

    loadAccounts();
}

function addAccount(token) {
    let accounts = JSON.parse(localStorage.getItem("accounts") || "[]")

}

// ---------------- RULES ----------------
function loadRules() {
    const token = localStorage.getItem("currentToken");
    if (!token) return;

    const isAdmin = localStorage.getItem("isAdmin") === "true";
    let url = isAdmin ? "/admin/rules" : "/rules";

    fetch(url, {headers: {Authorization: token}})
        .then(res => res.json())
        .then(data => {
            // optional filter for admin
            const filterInput = document.getElementById("user-filter");
            if(filterInput && filterInput.value) {
                data = data.filter(r => r.username.toLowerCase().includes(filterInput.value.toLowerCase()));
            }

            const tbody = document.getElementById("rules-body");
            if(!tbody) return;

            tbody.innerHTML = "";
            data.forEach(rule => {
                const row = document.createElement("tr");

                row.innerHTML = `
                    ${isAdmin ? `<td>${rule.username}</td>` : ""}
                    <td><input value="${rule.name || ""}" id="name-${rule.id}"></td>
                    <td><input value="${rule.communityRegex || ""}" id="community-${rule.id}"></td>
                    <td><input value="${rule.packageRegex || ""}" id="package-${rule.id}"></td>
                    <td><input value="${rule.versionRegex || ""}" id="version-${rule.id}"></td>
                    <td><input value="${rule.descriptionRegex || ""}" id="desc-${rule.id}"></td>
                    <td><input value="${rule.generalRegex || ""}" id="general-${rule.id}"></td>
                    <td><input value="${rule.url || ""}" id="url-${rule.id}"></td>
                    <td>
                        <button onclick="updateRule(${rule.id})">Save</button>
                        <button onclick="deleteRule(${rule.id})">Delete</button>
                    </td>
                `;
                tbody.appendChild(row);
            });
        });
}

function addRule() {
    const token = localStorage.getItem("currentToken");
    if (!token) return;

    const rule = {
        name: document.getElementById("rule-name").value,
        communityRegex: document.getElementById("community-regex").value,
        packageRegex: document.getElementById("package-regex").value,
        versionRegex: document.getElementById("version-regex").value,
        descriptionRegex: document.getElementById("description-regex").value,
        generalRegex: document.getElementById("general-regex").value,
        url: document.getElementById("rule-url").value
    };

    fetch("/rules", {
        method: "POST",
        headers: { "Content-Type": "application/json", "Authorization": token },
        body: JSON.stringify(rule)
    }).then(res => {
        if(res.status === 201) {
            loadRules();
            alert("Rule added!");
        } else {
            res.text().then(alert);
        }
    });
}

function updateRule(id) {
    const token = localStorage.getItem("currentToken");
    if (!token) return;

    const rule = {
        name: document.getElementById(`name-${id}`).value,
        communityRegex: document.getElementById(`community-${id}`).value,
        packageRegex: document.getElementById(`package-${id}`).value,
        versionRegex: document.getElementById(`version-${id}`).value,
        descriptionRegex: document.getElementById(`desc-${id}`).value,
        generalRegex: document.getElementById(`general-${id}`).value,
        url: document.getElementById(`url-${id}`).value
    };

    fetch(`/rules/${id}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json", "Authorization": token },
        body: JSON.stringify(rule)
    }).then(res => {
        if(res.status === 200) {
            loadRules();
            alert("Rule updated!");
        } else {
            res.text().then(alert);
        }
    });
}

function deleteRule(id) {
    const token = localStorage.getItem("currentToken");
    if (!token) return;

    if(!confirm("Are you sure you want to delete this rule?")) return;

    fetch(`/rules/${id}`, {
        method: "DELETE",
        headers: { "Authorization": token }
    }).then(res => {
        if(res.status === 200) {
            loadRules();
            alert("Rule deleted!");
        } else {
            res.text().then(alert);
        }
    });
}

// ---------------- INIT ----------------
document.addEventListener("DOMContentLoaded", () => {
    if(document.getElementById("rules-body")) loadRules();
    if(document.getElementById("accounts-list")) loadAccounts();
});
