/* =========================================================
   BILLING SOFTWARE - FRONTEND APPLICATION
   ========================================================= */

(() => {
    "use strict";

    /* =========================================================
       CONFIGURATION
       ========================================================= */

    const API_BASE = "";

    const ENDPOINTS = {
        login: "/api/auth/login",
        logout: "/api/auth/logout",

        users: "/api/users",

        meters: "/api/water-meters",
        myMeters: "/api/water-meters/my",

        assignments: "/api/water-meter-assignments",

        readings: "/api/water-meter-readings",

        plans: "/api/billing-plans",

        invoices: "/api/invoices",
        generateInvoice: "/api/invoices/generate",

        adminGenerateInvoice: "/api/admin/invoices/generate",

        health: "/actuator/health",
        info: "/actuator/info",
        metrics: "/actuator/metrics"
    };

    const STORAGE_KEYS = {
        sessionId: "billing.sessionId",
        username: "billing.username",
        role: "billing.role",
        lastJobId: "billing.lastJobId"
    };

    const state = {
        sessionId: null,
        username: null,
        role: null,

        currentSection: null,

        myMeters: [],
        adminMeters: [],
        plans: [],
        invoices: [],

        currentUser: null,
        currentMeter: null,
        currentPlan: null,

        lastJobId: null,
        jobPollingTimer: null
    };


    /* =========================================================
       DOM HELPERS
       ========================================================= */

    const $ = (id) => document.getElementById(id);

    const $$ = (selector) =>
        Array.from(document.querySelectorAll(selector));

    function show(element) {
        if (!element) return;
        element.classList.remove("hidden");
    }

    function hide(element) {
        if (!element) return;
        element.classList.add("hidden");
    }

    function setText(id, value) {
        const element = $(id);

        if (!element) return;

        element.textContent =
            value === null ||
            value === undefined ||
            value === ""
                ? "—"
                : String(value);
    }

    function escapeHtml(value) {
        if (value === null || value === undefined) {
            return "";
        }

        return String(value)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }

    function safeJson(value) {
        try {
            return JSON.stringify(value, null, 2);
        } catch (error) {
            return String(value);
        }
    }

    function firstDefined(...values) {
        return values.find(
            (value) =>
                value !== undefined &&
                value !== null &&
                value !== ""
        );
    }

    function arrayFrom(value) {
        if (Array.isArray(value)) {
            return value;
        }

        if (!value || typeof value !== "object") {
            return [];
        }

        if (Array.isArray(value.content)) {
            return value.content;
        }

        if (Array.isArray(value.items)) {
            return value.items;
        }

        if (Array.isArray(value.data)) {
            return value.data;
        }

        return [];
    }


    /* =========================================================
       STORAGE / SESSION
       ========================================================= */

    function loadSession() {
        state.sessionId =
            sessionStorage.getItem(STORAGE_KEYS.sessionId);

        state.username =
            sessionStorage.getItem(STORAGE_KEYS.username);

        state.role =
            sessionStorage.getItem(STORAGE_KEYS.role);

        state.lastJobId =
            sessionStorage.getItem(STORAGE_KEYS.lastJobId);
    }

    function saveSession(sessionId, username, role) {
        state.sessionId = sessionId;
        state.username = username;
        state.role = role;

        sessionStorage.setItem(
            STORAGE_KEYS.sessionId,
            sessionId
        );

        sessionStorage.setItem(
            STORAGE_KEYS.username,
            username
        );

        sessionStorage.setItem(
            STORAGE_KEYS.role,
            role
        );
    }

    function clearSession() {
        state.sessionId = null;
        state.username = null;
        state.role = null;

        state.myMeters = [];
        state.adminMeters = [];
        state.plans = [];
        state.invoices = [];

        state.currentUser = null;
        state.currentMeter = null;
        state.currentPlan = null;

        state.lastJobId = null;

        if (state.jobPollingTimer) {
            clearInterval(state.jobPollingTimer);
            state.jobPollingTimer = null;
        }

        sessionStorage.removeItem(STORAGE_KEYS.sessionId);
        sessionStorage.removeItem(STORAGE_KEYS.username);
        sessionStorage.removeItem(STORAGE_KEYS.role);
        sessionStorage.removeItem(STORAGE_KEYS.lastJobId);
    }

    function isAuthenticated() {
        return Boolean(state.sessionId);
    }

    function isAdmin() {
        return state.role === "ADMIN";
    }

    function isCustomer() {
        return state.role === "CUSTOMER";
    }


    /* =========================================================
       API CLIENT
       ========================================================= */

    async function apiRequest(
        path,
        options = {},
        requestOptions = {}
    ) {
        const {
            skipAuth = false,
            silent = false
        } = requestOptions;

        const headers = new Headers(
            options.headers || {}
        );

        if (
            options.body &&
            !(options.body instanceof FormData)
        ) {
            headers.set(
                "Content-Type",
                "application/json"
            );
        }

        if (!skipAuth && state.sessionId) {
            headers.set(
                "Authorization",
                `Bearer ${state.sessionId}`
            );
        }

        let response;

        try {
            response = await fetch(
                API_BASE + path,
                {
                    ...options,
                    headers
                }
            );
        } catch (error) {
            if (!silent) {
                showError(
                    "Unable to connect to the server."
                );
            }

            throw {
                type: "NETWORK",
                message:
                    "Unable to connect to the server.",
                original: error
            };
        }

        let body = null;
        const contentType =
            response.headers.get("content-type") || "";

        try {
            if (
                contentType.includes("application/json")
            ) {
                body = await response.json();
            } else {
                const text = await response.text();

                if (text) {
                    try {
                        body = JSON.parse(text);
                    } catch {
                        body = {
                            raw: text
                        };
                    }
                }
            }
        } catch {
            body = null;
        }

        /*
         * Backend contract:
         *
         * {
         *   success: false,
         *   errorMessage: "...",
         *   httpStatusCode: "...",
         *   data: null
         * }
         */

        if (!response.ok) {
            const backendMessage =
                body &&
                typeof body === "object" &&
                body.errorMessage;

            const message =
                backendMessage ||
                `Request failed with HTTP ${response.status}.`;

            if (
                response.status === 401 ||
                response.status === 403
            ) {
                if (!silent) {
                    showError(message);
                }

                /*
                 * Give the user a moment to see the backend
                 * message before returning to login.
                 */
                setTimeout(() => {
                    clearSession();
                    showLoginView();
                }, 800);
            } else if (!silent) {
                showError(message);
            }

            throw {
                type: "HTTP",
                status: response.status,
                body,
                message
            };
        }

        if (
            body &&
            typeof body === "object" &&
            Object.prototype.hasOwnProperty.call(
                body,
                "success"
            )
        ) {
            if (body.success !== true) {
                const message =
                    body.errorMessage ||
                    "The request was rejected by the server.";

                if (!silent) {
                    showError(message);
                }

                throw {
                    type: "BACKEND",
                    status: response.status,
                    body,
                    message
                };
            }
        }

        return {
            response,
            body
        };
    }


    /* =========================================================
       RESPONSE HELPERS
       ========================================================= */

    function responseData(result) {
        if (!result) return null;

        const body = result.body;

        if (
            body &&
            typeof body === "object" &&
            Object.prototype.hasOwnProperty.call(
                body,
                "data"
            )
        ) {
            return body.data;
        }

        return body;
    }

    function responseSuccessMessage(result) {
        const body = result?.body;

        if (
            body &&
            typeof body === "object" &&
            body.success === true &&
            body.errorMessage
        ) {
            return body.errorMessage;
        }

        return "Operation completed successfully.";
    }


    /* =========================================================
       TOAST / ALERTS
       ========================================================= */

    function showToast(
        message,
        type = "success",
        duration = 4000
    ) {
        const container =
            $("toast-container");

        if (!container) return;

        const toast =
            document.createElement("div");

        toast.className =
            `toast toast-${type}`;

        toast.textContent = message;

        container.appendChild(toast);

        requestAnimationFrame(() => {
            toast.classList.add("show");
        });

        setTimeout(() => {
            toast.classList.remove("show");

            setTimeout(() => {
                toast.remove();
            }, 250);
        }, duration);
    }

    function showSuccess(message) {
        showToast(message, "success");
    }

    function showError(message) {
        showToast(message, "error", 6000);

        const loginError = $("login-error");

        if (
            loginError &&
            $("login-view") &&
            !$("login-view").classList.contains("hidden")
        ) {
            loginError.textContent = message;
            show(loginError);
        }
    }

    function showInfo(message) {
        showToast(message, "info");
    }

    function setGlobalAlert(
        message,
        type = "info"
    ) {
        const element =
            $("global-alert");

        if (!element) return;

        element.textContent = message;
        element.className =
            `global-alert ${type}`;

        show(element);
    }

    function clearGlobalAlert() {
        hide($("global-alert"));
    }


    /* =========================================================
       LOADING BUTTONS
       ========================================================= */

    function setButtonLoading(
        button,
        loading,
        loadingText = "Working..."
    ) {
        if (!button) return;

        if (loading) {
            if (!button.dataset.originalText) {
                button.dataset.originalText =
                    button.textContent.trim();
            }

            button.disabled = true;
            button.textContent = loadingText;
        } else {
            button.disabled = false;

            if (button.dataset.originalText) {
                button.textContent =
                    button.dataset.originalText;

                delete button.dataset.originalText;
            }
        }
    }


    /* =========================================================
       VIEW MANAGEMENT
       ========================================================= */

    function showLoginView() {
        show($("login-view"));
        hide($("app-view"));

        const error = $("login-error");

        if (error) {
            error.textContent = "";
            hide(error);
        }

        document.title =
            "Billing Software - Login";
    }

    function showAppView() {
        hide($("login-view"));
        show($("app-view"));

        updateSessionUI();

        if (isAdmin()) {
            show($("admin-nav"));
            hide($("customer-nav"));

            navigateTo("admin-overview");
        } else {
            show($("customer-nav"));
            hide($("admin-nav"));

            navigateTo("customer-overview");
        }
    }

    function navigateTo(sectionName) {
        const section =
            $(`section-${sectionName}`);

        if (!section) {
            return;
        }

        const sections =
            $$(".page-section");

        sections.forEach((item) => {
            item.classList.remove("active");
        });

        section.classList.add("active");

        state.currentSection =
            sectionName;

        updateNavigationState(sectionName);
        updatePageHeader(sectionName);
        closeMobileSidebar();

        loadSectionData(sectionName);
    }

    function updateNavigationState(sectionName) {
        $$(".nav-item[data-section]").forEach(
            (button) => {
                button.classList.toggle(
                    "active",
                    button.dataset.section ===
                    sectionName
                );
            }
        );
    }

    function updatePageHeader(sectionName) {
        const titles = {
            "customer-overview":
                ["Customer", "Overview"],

            "customer-meters":
                ["Customer", "My Water Meters"],

            "customer-reading":
                ["Customer", "Meter Reading"],

            "customer-invoices":
                ["Customer", "My Invoices"],

            "customer-generate":
                ["Customer", "Generate Invoice"],

            "customer-account":
                ["Customer", "Account"],

            "admin-overview":
                ["Administrator", "Overview"],

            "admin-users":
                ["Administrator", "Users"],

            "admin-meters":
                ["Administrator", "Water Meters"],

            "admin-assignments":
                ["Administrator", "Assignments"],

            "admin-readings":
                ["Administrator", "Meter Readings"],

            "admin-plans":
                ["Administrator", "Billing Plans"],

            "admin-invoices":
                ["Administrator", "Invoices"],

            "admin-billing":
                ["Administrator", "Billing Jobs"],

            "admin-monitoring":
                ["Administrator", "Monitoring"]
        };

        const title =
            titles[sectionName] ||
            ["Billing", "Dashboard"];

        setText(
            "breadcrumb-role",
            title[0]
        );

        setText(
            "page-title",
            title[1]
        );

        document.title =
            `${title[1]} - Billing Software`;
    }

    function updateSessionUI() {
        const username =
            state.username || "User";

        const role =
            state.role || "CUSTOMER";

        const initial =
            username.charAt(0).toUpperCase();

        setText(
            "session-username",
            username
        );

        setText(
            "session-role",
            role
        );

        setText(
            "session-avatar",
            initial
        );

        setText(
            "topbar-avatar",
            initial
        );

        setText(
            "account-username",
            username
        );

        setText(
            "account-role",
            role
        );

        setText(
            "account-username-detail",
            username
        );

        setText(
            "account-role-detail",
            role
        );

        setText(
            "account-avatar",
            initial
        );

        setText(
            "breadcrumb-role",
            role === "ADMIN"
                ? "Administrator"
                : "Customer"
        );
    }


    /* =========================================================
       SECTION DATA LOADING
       ========================================================= */

    async function loadSectionData(sectionName) {
        if (!isAuthenticated()) {
            return;
        }

        try {
            switch (sectionName) {

                case "customer-overview":
                    await loadCustomerOverview();
                    break;

                case "customer-meters":
                    await loadMyMeters();
                    break;

                case "customer-reading":
                    await loadMyMeters();
                    break;

                case "customer-invoices":
                    await loadCustomerInvoices();
                    break;

                case "customer-generate":
                    initialiseBillingPeriod(
                        "customer-billing-year",
                        "customer-billing-month"
                    );
                    await loadMyMeters();
                    break;

                case "customer-account":
                    updateSessionUI();
                    break;

                case "admin-overview":
                    await loadAdminOverview();
                    break;

                case "admin-users":
                    break;

                case "admin-meters":
                    await loadAdminMeters();
                    await loadAdminPlansIntoSelects();
                    break;

                case "admin-assignments":
                    break;

                case "admin-readings":
                    break;

                case "admin-plans":
                    await loadAdminPlans();
                    break;

                case "admin-invoices":
                    await loadAdminInvoices();
                    break;

                case "admin-billing":
                    initialiseBillingPeriod(
                        "admin-billing-year",
                        "admin-billing-month"
                    );
                    restoreLastJob();
                    break;

                case "admin-monitoring":
                    await loadMonitoring();
                    break;
            }
        } catch (error) {
            /*
             * apiRequest already displays backend errors.
             * Nothing else is needed here.
             */
        }
    }


    /* =========================================================
       LOGIN
       ========================================================= */

    async function handleLogin(event) {
        event.preventDefault();

        const form =
            event.currentTarget;

        const button =
            form.querySelector(
                'button[type="submit"]'
            );

        const username =
            $("login-username").value.trim();

        const password =
            $("login-password").value;

        const selectedRole =
            $("login-role").value;

        hide($("login-error"));
        clearGlobalAlert();

        if (!username || !password) {
            showError(
                "Username and password are required."
            );
            return;
        }

        setButtonLoading(
            button,
            true,
            "Signing in..."
        );

        try {
            const result =
                await apiRequest(
                    ENDPOINTS.login,
                    {
                        method: "POST",
                        body: JSON.stringify({
                            username,
                            password
                        })
                    },
                    {
                        skipAuth: true
                    }
                );

            const data =
                responseData(result);

            const sessionId =
                data?.sessionId;

            if (!sessionId) {
                throw {
                    type: "CLIENT",
                    message:
                        "Login succeeded but no session ID was returned by the backend."
                };
            }

            /*
             * The selected dashboard is only a UI preference.
             * The actual role comes from the backend only if
             * the login response provides it.
             *
             * If it does not, the selected role is used.
             */
            const backendRole =
                firstDefined(
                    data?.role,
                    data?.userRole,
                    data?.user?.role
                );

            const actualRole =
                backendRole ||
                selectedRole;

            saveSession(
                sessionId,
                username,
                actualRole
            );

            showSuccess(
                responseSuccessMessage(result)
            );

            form.reset();

            showAppView();

        } catch (error) {
            /*
             * apiRequest already handles backend errors.
             */
            if (
                error?.type === "CLIENT"
            ) {
                showError(error.message);
            }
        } finally {
            setButtonLoading(
                button,
                false
            );
        }
    }


    /* =========================================================
       LOGOUT
       ========================================================= */

    async function logout() {
        const sessionId =
            state.sessionId;

        try {
            if (sessionId) {
                const result =
                    await apiRequest(
                        ENDPOINTS.logout,
                        {
                            method: "POST"
                        },
                        {
                            silent: true
                        }
                    );

                if (
                    result.body &&
                    result.body.success === false
                ) {
                    showError(
                        result.body.errorMessage ||
                        "Logout failed."
                    );

                    return;
                }

                if (
                    result.body &&
                    result.body.success === true
                ) {
                    showSuccess(
                        responseSuccessMessage(
                            result
                        )
                    );
                }
            }
        } catch (error) {
            /*
             * A local logout is still performed below.
             */
        } finally {
            clearSession();
            showLoginView();
        }
    }


    /* =========================================================
       CUSTOMER OVERVIEW
       ========================================================= */

    async function loadCustomerOverview() {
        const results =
            await Promise.allSettled([
                loadMyMeters(true),
                loadCustomerInvoices(true)
            ]);

        /*
         * Errors are already displayed by apiRequest.
         * Promise.allSettled prevents one failed widget
         * from preventing the other widget from loading.
         */
        return results;
    }


    /* =========================================================
       CUSTOMER METERS
       ========================================================= */

    async function loadMyMeters(
        overviewOnly = false
    ) {
        const result =
            await apiRequest(
                ENDPOINTS.myMeters,
                {
                    method: "GET"
                }
            );

        const data =
            responseData(result);

        const meters =
            arrayFrom(data);

        state.myMeters =
            meters;

        renderCustomerMeters(
            meters,
            overviewOnly
        );

        populateCustomerReadingMeters(
            meters
        );

        setText(
            "customer-meter-count",
            meters.length
        );

        return meters;
    }

    function renderCustomerMeters(
        meters,
        overviewOnly = false
    ) {
        const overviewBody =
            $("customer-overview-meters");

        const tableBody =
            $("customer-meters-table");

        if (overviewBody) {
            if (!meters.length) {
                overviewBody.innerHTML =
                    emptyRow(
                        3,
                        "No water meters are assigned to your account."
                    );
            } else {
                overviewBody.innerHTML =
                    meters
                        .slice(0, 5)
                        .map((meter) => `
                            <tr>
                                <td>
                                    <strong>
                                        ${escapeHtml(
                            meterNumber(meter)
                        )}
                                    </strong>
                                </td>
                                <td>
                                    ${escapeHtml(
                            planName(meter)
                        )}
                                </td>
                                <td>
                                    <span class="badge badge-success">
                                        Assigned
                                    </span>
                                </td>
                            </tr>
                        `)
                        .join("");
            }
        }

        if (
            tableBody &&
            !overviewOnly
        ) {
            if (!meters.length) {
                tableBody.innerHTML =
                    emptyRow(
                        5,
                        "No water meters are assigned to your account."
                    );
            } else {
                tableBody.innerHTML =
                    meters
                        .map((meter) => `
                            <tr>
                                <td>
                                    ${escapeHtml(
                            meterId(meter)
                        )}
                                </td>

                                <td>
                                    <strong>
                                        ${escapeHtml(
                            meterNumber(meter)
                        )}
                                    </strong>
                                </td>

                                <td>
                                    ${escapeHtml(
                            planName(meter)
                        )}
                                </td>

                                <td>
                                    <span class="badge badge-success">
                                        Assigned
                                    </span>
                                </td>

                                <td>
                                    <button
                                        type="button"
                                        class="btn btn-ghost btn-sm"
                                        data-action="use-meter-reading"
                                        data-meter-id="${escapeHtml(
                            meterId(meter)
                        )}">
                                        Submit reading
                                    </button>
                                </td>
                            </tr>
                        `)
                        .join("");
            }
        }
    }

    function populateCustomerReadingMeters(
        meters
    ) {
        const select =
            $("customer-reading-meter");

        if (!select) return;

        const current =
            select.value;

        select.innerHTML =
            `<option value="">Select meter</option>`;

        meters.forEach((meter) => {
            const id =
                meterId(meter);

            if (!id) return;

            const option =
                document.createElement("option");

            option.value = id;

            option.textContent =
                `${meterNumber(meter)} (${id})`;

            select.appendChild(option);
        });

        if (current) {
            select.value = current;
        }
    }


    /* =========================================================
       CUSTOMER INVOICES
       ========================================================= */

    async function loadCustomerInvoices(
        overviewOnly = false
    ) {
        const result =
            await apiRequest(
                ENDPOINTS.invoices,
                {
                    method: "GET"
                }
            );

        const data =
            responseData(result);

        const invoices =
            arrayFrom(data);

        state.invoices =
            invoices;

        renderCustomerInvoices(
            invoices,
            overviewOnly
        );

        setText(
            "customer-invoice-count",
            invoices.length
        );

        if (invoices.length) {
            const latest =
                invoices[0];

            const amount =
                invoiceAmount(latest);

            setText(
                "customer-latest-invoice",
                formatCurrency(amount)
            );
        } else {
            setText(
                "customer-latest-invoice",
                "—"
            );
        }

        return invoices;
    }

    function renderCustomerInvoices(
        invoices,
        overviewOnly = false
    ) {
        const overviewBody =
            $("customer-overview-invoices");

        const tableBody =
            $("customer-invoices-table");

        const sorted =
            [...invoices].sort(
                compareInvoicesDescending
            );

        if (overviewBody) {
            if (!sorted.length) {
                overviewBody.innerHTML =
                    emptyRow(
                        3,
                        "No invoices found."
                    );
            } else {
                overviewBody.innerHTML =
                    sorted
                        .slice(0, 5)
                        .map((invoice) => `
                            <tr>
                                <td>
                                    <strong>
                                        ${escapeHtml(
                            invoiceId(invoice)
                        )}
                                    </strong>
                                </td>

                                <td>
                                    ${escapeHtml(
                            invoicePeriod(invoice)
                        )}
                                </td>

                                <td>
                                    ${escapeHtml(
                            formatCurrency(
                                invoiceAmount(
                                    invoice
                                )
                            )
                        )}
                                </td>
                            </tr>
                        `)
                        .join("");
            }
        }

        if (
            tableBody &&
            !overviewOnly
        ) {
            if (!sorted.length) {
                tableBody.innerHTML =
                    emptyRow(
                        6,
                        "No invoices found."
                    );
            } else {
                tableBody.innerHTML =
                    sorted
                        .map((invoice) => `
                            <tr>
                                <td>
                                    <strong>
                                        ${escapeHtml(
                            invoiceId(invoice)
                        )}
                                    </strong>
                                </td>

                                <td>
                                    ${escapeHtml(
                            customerName(invoice)
                        )}
                                </td>

                                <td>
                                    ${escapeHtml(
                            invoiceMeter(invoice)
                        )}
                                </td>

                                <td>
                                    ${escapeHtml(
                            invoicePeriod(invoice)
                        )}
                                </td>

                                <td>
                                    <strong>
                                        ${escapeHtml(
                            formatCurrency(
                                invoiceAmount(
                                    invoice
                                )
                            )
                        )}
                                    </strong>
                                </td>

                                <td>
                                    <span class="badge">
                                        ${escapeHtml(
                            invoiceStatus(
                                invoice
                            )
                        )}
                                    </span>
                                </td>

                                <td>
                                    <button
                                        type="button"
                                        class="btn btn-ghost btn-sm"
                                        data-action="view-invoice"
                                        data-invoice-id="${escapeHtml(
                            invoiceId(invoice)
                        )}">
                                        View
                                    </button>
                                </td>
                            </tr>
                        `)
                        .join("");
            }
        }
    }


    /* =========================================================
       CUSTOMER METER READING
       ========================================================= */

    async function submitCustomerReading(
        event
    ) {
        event.preventDefault();

        const form =
            event.currentTarget;

        const button =
            form.querySelector(
                'button[type="submit"]'
            );

        const payload =
            buildReadingPayload(
                "customer"
            );

        setButtonLoading(
            button,
            true,
            "Submitting..."
        );

        try {
            const result =
                await apiRequest(
                    ENDPOINTS.readings,
                    {
                        method: "POST",
                        body: JSON.stringify(
                            payload
                        )
                    }
                );

            showSuccess(
                responseSuccessMessage(result)
            );

            form.reset();

            setDefaultDateTime(
                "customer-reading-at"
            );

        } catch (error) {
            // Error already displayed.
        } finally {
            setButtonLoading(
                button,
                false
            );
        }
    }

    function buildReadingPayload(
        prefix
    ) {
        const meterIdValue =
            $(
                `${prefix}-reading-meter`
            )?.value.trim();

        const type =
            $(
                `${prefix}-reading-type`
            )?.value;

        const readingValue =
            parseFloat(
                $(
                    `${prefix}-reading-value`
                )?.value
            );

        const dateValue =
            $(
                `${prefix}-reading-at`
            )?.value;

        const ingestionKey =
            $(
                `${prefix}-ingestion-key`
            )?.value.trim();

        return {
            waterMeterId: meterIdValue,
            readingType: type,
            readingValue,
            readingAt:
                localDateTimeToIso(
                    dateValue
                ),
            ingestionKey
        };
    }

    async function submitAdminReading(
        event
    ) {
        event.preventDefault();

        const form =
            event.currentTarget;

        const button =
            form.querySelector(
                'button[type="submit"]'
            );

        const payload = {
            waterMeterId:
                $("admin-reading-meter")
                    .value.trim(),

            readingType:
            $("admin-reading-type")
                .value,

            readingValue:
                parseFloat(
                    $("admin-reading-value")
                        .value
                ),

            readingAt:
                localDateTimeToIso(
                    $("admin-reading-at")
                        .value
                ),

            ingestionKey:
                $("admin-ingestion-key")
                    .value.trim()
        };

        setButtonLoading(
            button,
            true,
            "Submitting..."
        );

        try {
            const result =
                await apiRequest(
                    ENDPOINTS.readings,
                    {
                        method: "POST",
                        body: JSON.stringify(
                            payload
                        )
                    }
                );

            showSuccess(
                responseSuccessMessage(result)
            );

            form.reset();

            setDefaultDateTime(
                "admin-reading-at"
            );

        } catch (error) {
            // Error already displayed.
        } finally {
            setButtonLoading(
                button,
                false
            );
        }
    }


    /* =========================================================
       CUSTOMER INVOICE GENERATION
       ========================================================= */

    async function generateCustomerInvoice(
        event
    ) {
        event.preventDefault();

        const form =
            event.currentTarget;

        const button =
            form.querySelector(
                'button[type="submit"]'
            );

        const year =
            parseInt(
                $("customer-billing-year")
                    .value,
                10
            );

        const month =
            parseInt(
                $("customer-billing-month")
                    .value,
                10
            );

        setButtonLoading(
            button,
            true,
            "Generating..."
        );

        try {
            const result =
                await apiRequest(
                    ENDPOINTS.generateInvoice,
                    {
                        method: "POST",
                        body: JSON.stringify({
                            year,
                            month
                        })
                    }
                );

            showSuccess(
                responseSuccessMessage(result)
            );

            await loadCustomerInvoices();

            navigateTo(
                "customer-invoices"
            );

        } catch (error) {
            // Error already displayed.
        } finally {
            setButtonLoading(
                button,
                false
            );
        }
    }


    /* =========================================================
       ADMIN OVERVIEW
       ========================================================= */

    async function loadAdminOverview() {
        const results =
            await Promise.allSettled([
                loadAdminMeters(true),
                loadAdminPlans(true),
                loadAdminInvoices(true),
                loadHealth(true)
            ]);

        return results;
    }


    /* =========================================================
       ADMIN USERS
       ========================================================= */

    async function createUser(event) {
        event.preventDefault();

        const form =
            event.currentTarget;

        const button =
            form.querySelector(
                'button[type="submit"]'
            );

        const payload = {
            username:
                $("create-user-username")
                    .value.trim(),

            password:
            $("create-user-password")
                .value,

            role:
            $("create-user-role")
                .value
        };

        setButtonLoading(
            button,
            true,
            "Creating..."
        );

        try {
            const result =
                await apiRequest(
                    ENDPOINTS.users,
                    {
                        method: "POST",
                        body: JSON.stringify(
                            payload
                        )
                    }
                );

            showSuccess(
                responseSuccessMessage(result)
            );

            form.reset();

        } catch (error) {
            // Error already displayed.
        } finally {
            setButtonLoading(
                button,
                false
            );
        }
    }

    async function lookupUser(event) {
        event.preventDefault();

        const id =
            $("user-lookup-id")
                .value.trim();

        if (!id) {
            showError(
                "User ID is required."
            );
            return;
        }

        try {
            const result =
                await apiRequest(
                    `${ENDPOINTS.users}/${encodeURIComponent(id)}`,
                    {
                        method: "GET"
                    }
                );

            const user =
                responseData(result);

            state.currentUser =
                user;

            renderUserDetail(user);

            showSuccess(
                responseSuccessMessage(result)
            );

        } catch (error) {
            hide(
                $("user-detail-card")
            );
        }
    }

    function renderUserDetail(user) {
        if (!user) return;

        show($("user-detail-card"));

        setText(
            "user-detail-title",
            firstDefined(
                user.username,
                user.name,
                "User"
            )
        );

        setText(
            "user-detail-id",
            firstDefined(
                user.id,
                user.userId
            )
        );

        setText(
            "user-detail-username",
            user.username
        );

        setText(
            "user-detail-role",
            user.role
        );

        setText(
            "user-detail-role-value",
            user.role
        );
    }

    function editCurrentUser() {
        const user =
            state.currentUser;

        if (!user) {
            showError(
                "Look up a user before editing."
            );
            return;
        }

        $("edit-user-id").value =
            firstDefined(
                user.id,
                user.userId,
                ""
            );

        $("edit-user-username").value =
            firstDefined(
                user.username,
                ""
            );

        $("edit-user-role").value =
            firstDefined(
                user.role,
                "CUSTOMER"
            );

        $("edit-user-password").value =
            "";

        show(
            $("admin-user-edit-panel")
        );

        $("admin-user-edit-panel")
            .scrollIntoView({
                behavior: "smooth",
                block: "start"
            });
    }

    async function updateUser(event) {
        event.preventDefault();

        const form =
            event.currentTarget;

        const button =
            form.querySelector(
                'button[type="submit"]'
            );

        const id =
            $("edit-user-id")
                .value.trim();

        const username =
            $("edit-user-username")
                .value.trim();

        const password =
            $("edit-user-password")
                .value;

        const role =
            $("edit-user-role")
                .value;

        const payload = {
            username,
            role
        };

        if (password) {
            payload.password =
                password;
        }

        setButtonLoading(
            button,
            true,
            "Updating..."
        );

        try {
            const result =
                await apiRequest(
                    `${ENDPOINTS.users}/${encodeURIComponent(id)}`,
                    {
                        method: "PUT",
                        body: JSON.stringify(
                            payload
                        )
                    }
                );

            showSuccess(
                responseSuccessMessage(result)
            );

            hide(
                $("admin-user-edit-panel")
            );

            await lookupUserById(id);

        } catch (error) {
            // Error already displayed.
        } finally {
            setButtonLoading(
                button,
                false
            );
        }
    }

    async function lookupUserById(id) {
        const result =
            await apiRequest(
                `${ENDPOINTS.users}/${encodeURIComponent(id)}`,
                {
                    method: "GET"
                }
            );

        const user =
            responseData(result);

        state.currentUser =
            user;

        renderUserDetail(user);

        return user;
    }

    async function deleteCurrentUser() {
        const user =
            state.currentUser;

        if (!user) {
            showError(
                "Look up a user before deleting."
            );
            return;
        }

        const id =
            firstDefined(
                user.id,
                user.userId
            );

        const username =
            user.username || id;

        openConfirmModal({
            title: "Delete user?",
            eyebrow: "User management",
            body:
                `This will permanently delete user "${username}".`,
            confirmText: "Delete user",
            async onConfirm() {
                const result =
                    await apiRequest(
                        `${ENDPOINTS.users}/${encodeURIComponent(id)}`,
                        {
                            method: "DELETE"
                        }
                    );

                showSuccess(
                    responseSuccessMessage(result)
                );

                state.currentUser = null;

                hide(
                    $("user-detail-card")
                );

                hide(
                    $("admin-user-edit-panel")
                );
            }
        });
    }


    /* =========================================================
       ADMIN METERS
       ========================================================= */

    async function loadAdminMeters(
        overviewOnly = false
    ) {
        const result =
            await apiRequest(
                ENDPOINTS.meters,
                {
                    method: "GET"
                }
            );

        const data =
            responseData(result);

        const meters =
            arrayFrom(data);

        state.adminMeters =
            meters;

        renderAdminMeters(
            meters,
            overviewOnly
        );

        setText(
            "admin-meter-count",
            meters.length
        );

        if (!overviewOnly) {
            await loadAdminPlansIntoSelects();
        }

        return meters;
    }

    function renderAdminMeters(
        meters,
        overviewOnly = false
    ) {
        const overviewBody =
            $("admin-overview-meters");

        const tableBody =
            $("admin-meters-table");

        if (overviewBody) {
            if (!meters.length) {
                overviewBody.innerHTML =
                    emptyRow(
                        3,
                        "No water meters found."
                    );
            } else {
                overviewBody.innerHTML =
                    meters
                        .slice(0, 5)
                        .map((meter) => `
                            <tr>
                                <td>
                                    <strong>
                                        ${escapeHtml(
                            meterNumber(meter)
                        )}
                                    </strong>
                                </td>

                                <td>
                                    ${escapeHtml(
                            planName(meter)
                        )}
                                </td>

                                <td>
                                    ${escapeHtml(
                            meterId(meter)
                        )}
                                </td>
                            </tr>
                        `)
                        .join("");
            }
        }

        if (
            tableBody &&
            !overviewOnly
        ) {
            if (!meters.length) {
                tableBody.innerHTML =
                    emptyRow(
                        4,
                        "No water meters found."
                    );
            } else {
                tableBody.innerHTML =
                    meters
                        .map((meter) => `
                            <tr>
                                <td>
                                    ${escapeHtml(
                            meterId(meter)
                        )}
                                </td>

                                <td>
                                    <strong>
                                        ${escapeHtml(
                            meterNumber(meter)
                        )}
                                    </strong>
                                </td>

                                <td>
                                    ${escapeHtml(
                            planName(meter)
                        )}
                                </td>

                                <td>
                                    <div class="table-actions">

                                        <button
                                            type="button"
                                            class="btn btn-ghost btn-sm"
                                            data-action="lookup-meter"
                                            data-meter-id="${escapeHtml(
                            meterId(meter)
                        )}">
                                            View
                                        </button>

                                        <button
                                            type="button"
                                            class="btn btn-ghost btn-sm"
                                            data-action="edit-meter"
                                            data-meter-id="${escapeHtml(
                            meterId(meter)
                        )}">
                                            Edit
                                        </button>

                                        <button
                                            type="button"
                                            class="btn btn-danger btn-sm"
                                            data-action="delete-meter"
                                            data-meter-id="${escapeHtml(
                            meterId(meter)
                        )}">
                                            Delete
                                        </button>

                                    </div>
                                </td>
                            </tr>
                        `)
                        .join("");
            }
        }
    }

    async function createMeter(event) {
        event.preventDefault();

        const form =
            event.currentTarget;

        const button =
            form.querySelector(
                'button[type="submit"]'
            );

        const payload = {
            meterNumber:
                $("create-meter-number")
                    .value.trim(),

            billingPlanId:
            $("create-meter-plan")
                .value
        };

        setButtonLoading(
            button,
            true,
            "Creating..."
        );

        try {
            const result =
                await apiRequest(
                    ENDPOINTS.meters,
                    {
                        method: "POST",
                        body: JSON.stringify(
                            payload
                        )
                    }
                );

            showSuccess(
                responseSuccessMessage(result)
            );

            form.reset();

            await loadAdminMeters();

        } catch (error) {
            // Error already displayed.
        } finally {
            setButtonLoading(
                button,
                false
            );
        }
    }

    async function lookupMeter(event) {
        event.preventDefault();

        const id =
            $("meter-lookup-id")
                .value.trim();

        if (!id) {
            showError(
                "Meter ID is required."
            );
            return;
        }

        await lookupMeterById(id);
    }

    async function lookupMeterById(id) {
        try {
            const result =
                await apiRequest(
                    `${ENDPOINTS.meters}/${encodeURIComponent(id)}`,
                    {
                        method: "GET"
                    }
                );

            const meter =
                responseData(result);

            state.currentMeter =
                meter;

            renderMeterDetail(meter);

            showSuccess(
                responseSuccessMessage(result)
            );

            return meter;

        } catch (error) {
            hide(
                $("meter-detail-card")
            );
        }
    }

    function renderMeterDetail(meter) {
        if (!meter) return;

        show($("meter-detail-card"));

        setText(
            "meter-detail-title",
            firstDefined(
                meterNumber(meter),
                "Water Meter"
            )
        );

        setText(
            "meter-detail-id",
            meterId(meter)
        );

        setText(
            "meter-detail-number",
            meterNumber(meter)
        );

        setText(
            "meter-detail-plan",
            planName(meter)
        );
    }

    async function editMeterById(id) {
        const meter =
            state.adminMeters.find(
                (item) =>
                    String(meterId(item)) ===
                    String(id)
            );

        let selected =
            meter;

        if (!selected) {
            selected =
                await lookupMeterById(
                    id
                );
        }

        if (!selected) return;

        state.currentMeter =
            selected;

        $("edit-meter-id").value =
            meterId(selected);

        $("edit-meter-number").value =
            meterNumber(selected);

        const planId =
            billingPlanId(selected);

        $("edit-meter-plan").value =
            planId || "";

        show(
            $("admin-meter-edit-panel")
        );

        $("admin-meter-edit-panel")
            .scrollIntoView({
                behavior: "smooth",
                block: "start"
            });
    }

    async function updateMeter(event) {
        event.preventDefault();

        const form =
            event.currentTarget;

        const button =
            form.querySelector(
                'button[type="submit"]'
            );

        const id =
            $("edit-meter-id")
                .value.trim();

        const payload = {
            meterNumber:
                $("edit-meter-number")
                    .value.trim(),

            billingPlanId:
            $("edit-meter-plan")
                .value
        };

        setButtonLoading(
            button,
            true,
            "Updating..."
        );

        try {
            const result =
                await apiRequest(
                    `${ENDPOINTS.meters}/${encodeURIComponent(id)}`,
                    {
                        method: "PUT",
                        body: JSON.stringify(
                            payload
                        )
                    }
                );

            showSuccess(
                responseSuccessMessage(result)
            );

            hide(
                $("admin-meter-edit-panel")
            );

            await loadAdminMeters();

        } catch (error) {
            // Error already displayed.
        } finally {
            setButtonLoading(
                button,
                false
            );
        }
    }

    async function deleteMeterById(id) {
        openConfirmModal({
            title: "Delete water meter?",
            eyebrow: "Meter management",
            body:
                `This will permanently delete meter "${id}".`,
            confirmText: "Delete meter",
            async onConfirm() {
                const result =
                    await apiRequest(
                        `${ENDPOINTS.meters}/${encodeURIComponent(id)}`,
                        {
                            method: "DELETE"
                        }
                    );

                showSuccess(
                    responseSuccessMessage(result)
                );

                hide(
                    $("meter-detail-card")
                );

                await loadAdminMeters();
            }
        });
    }


    /* =========================================================
       ADMIN PLAN SELECTS
       ========================================================= */

    async function loadAdminPlansIntoSelects() {
        if (!isAdmin()) return;

        let plans =
            state.plans;

        if (!plans.length) {
            try {
                const result =
                    await apiRequest(
                        ENDPOINTS.plans,
                        {
                            method: "GET"
                        }
                    );

                plans =
                    arrayFrom(
                        responseData(result)
                    );

                state.plans =
                    plans;
            } catch {
                return;
            }
        }

        populatePlanSelect(
            $("create-meter-plan"),
            plans
        );

        populatePlanSelect(
            $("edit-meter-plan"),
            plans
        );
    }

    function populatePlanSelect(
        select,
        plans
    ) {
        if (!select) return;

        const selected =
            select.value;

        const placeholder =
            select.id === "edit-meter-plan"
                ? "Select billing plan"
                : "Select billing plan";

        select.innerHTML =
            `<option value="">${placeholder}</option>`;

        plans.forEach((plan) => {
            const id =
                planId(plan);

            if (!id) return;

            const option =
                document.createElement("option");

            option.value = id;

            option.textContent =
                `${plan.name || "Plan"} (${plan.code || id})`;

            select.appendChild(option);
        });

        if (selected) {
            select.value = selected;
        }
    }


    /* =========================================================
       ADMIN ASSIGNMENTS
       ========================================================= */

    async function assignMeter(event) {
        event.preventDefault();

        const form =
            event.currentTarget;

        const button =
            form.querySelector(
                'button[type="submit"]'
            );

        const payload = {
            waterMeterId:
                $("assignment-meter-id")
                    .value.trim(),

            userId:
                $("assignment-user-id")
                    .value.trim()
        };

        setButtonLoading(
            button,
            true,
            "Assigning..."
        );

        try {
            const result =
                await apiRequest(
                    ENDPOINTS.assignments,
                    {
                        method: "POST",
                        body: JSON.stringify(
                            payload
                        )
                    }
                );

            showSuccess(
                responseSuccessMessage(result)
            );

            form.reset();

            await loadAdminMeters();

        } catch (error) {
            // Error already displayed.
        } finally {
            setButtonLoading(
                button,
                false
            );
        }
    }

    async function unassignMeter(event) {
        event.preventDefault();

        const form =
            event.currentTarget;

        const button =
            form.querySelector(
                'button[type="submit"]'
            );

        const meterIdValue =
            $("unassignment-meter-id")
                .value.trim();

        const userIdValue =
            $("unassignment-user-id")
                .value.trim();

        setButtonLoading(
            button,
            true,
            "Unassigning..."
        );

        try {
            const result =
                await apiRequest(
                    `${ENDPOINTS.assignments}?waterMeterId=${encodeURIComponent(
                        meterIdValue
                    )}&userId=${encodeURIComponent(
                        userIdValue
                    )}`,
                    {
                        method: "DELETE"
                    }
                );

            showSuccess(
                responseSuccessMessage(result)
            );

            form.reset();

            await loadAdminMeters();

        } catch (error) {
            // Error already displayed.
        } finally {
            setButtonLoading(
                button,
                false
            );
        }
    }


    /* =========================================================
       ADMIN BILLING PLANS
       ========================================================= */

    async function loadAdminPlans(
        overviewOnly = false
    ) {
        const result =
            await apiRequest(
                ENDPOINTS.plans,
                {
                    method: "GET"
                }
            );

        const data =
            responseData(result);

        const plans =
            arrayFrom(data);

        state.plans =
            plans;

        renderAdminPlans(
            plans,
            overviewOnly
        );

        setText(
            "admin-plan-count",
            plans.length
        );

        await loadAdminPlansIntoSelects();

        return plans;
    }

    function renderAdminPlans(
        plans,
        overviewOnly = false
    ) {
        const overviewBody =
            $("admin-overview-plans");

        const tableBody =
            $("admin-plans-table");

        if (overviewBody) {
            if (!plans.length) {
                overviewBody.innerHTML =
                    emptyRow(
                        3,
                        "No billing plans found."
                    );
            } else {
                overviewBody.innerHTML =
                    plans
                        .slice(0, 5)
                        .map((plan) => `
                            <tr>
                                <td>
                                    <strong>
                                        ${escapeHtml(
                            plan.name
                        )}
                                    </strong>
                                </td>

                                <td>
                                    ${escapeHtml(
                            plan.code
                        )}
                                </td>

                                <td>
                                    <span class="badge">
                                        ${escapeHtml(
                            plan.planType
                        )}
                                    </span>
                                </td>
                            </tr>
                        `)
                        .join("");
            }
        }

        if (
            tableBody &&
            !overviewOnly
        ) {
            if (!plans.length) {
                tableBody.innerHTML =
                    emptyRow(
                        7,
                        "No billing plans found."
                    );
            } else {
                tableBody.innerHTML =
                    plans
                        .map((plan) => `
                            <tr>
                                <td>
                                    ${escapeHtml(
                            planId(plan)
                        )}
                                </td>

                                <td>
                                    <strong>
                                        ${escapeHtml(
                            plan.name
                        )}
                                    </strong>
                                </td>

                                <td>
                                    ${escapeHtml(
                            plan.code
                        )}
                                </td>

                                <td>
                                    <span class="badge">
                                        ${escapeHtml(
                            plan.planType
                        )}
                                    </span>
                                </td>

                                <td>
                                    ${
                            plan.planType === "FIXED"
                                ? escapeHtml(
                                    formatCurrency(
                                        plan.pricePerUnit
                                    )
                                )
                                : "—"
                        }
                                </td>

                                <td>
                                    ${
                            plan.planType === "SLAB"
                                ? escapeHtml(
                                    String(
                                        Array.isArray(
                                            plan.slabs
                                        )
                                            ? plan.slabs.length
                                            : 0
                                    )
                                )
                                : "—"
                        }
                                </td>

                                <td>
                                    <div class="table-actions">

                                        <button
                                            type="button"
                                            class="btn btn-ghost btn-sm"
                                            data-action="edit-plan"
                                            data-plan-id="${escapeHtml(
                            planId(plan)
                        )}">
                                            Edit
                                        </button>

                                        <button
                                            type="button"
                                            class="btn btn-danger btn-sm"
                                            data-action="delete-plan"
                                            data-plan-id="${escapeHtml(
                            planId(plan)
                        )}">
                                            Delete
                                        </button>

                                    </div>
                                </td>
                            </tr>
                        `)
                        .join("");
            }
        }
    }

    function updatePlanTypeUI(
        prefix = ""
    ) {
        const type =
            $(
                prefix === "edit"
                    ? "edit-plan-type"
                    : "plan-type"
            )?.value;

        const fixedField =
            $(
                prefix === "edit"
                    ? "edit-fixed-price-field"
                    : "fixed-price-field"
            );

        const slabBuilder =
            $(
                prefix === "edit"
                    ? "edit-slab-builder"
                    : "slab-builder"
            );

        if (type === "SLAB") {
            hide(fixedField);
            show(slabBuilder);

            const rows =
                $(
                    prefix === "edit"
                        ? "edit-slab-rows"
                        : "slab-rows"
                );

            if (
                rows &&
                !rows.children.length
            ) {
                addSlabRow(
                    prefix === "edit"
                        ? "edit"
                        : ""
                );
            }
        } else {
            show(fixedField);
            hide(slabBuilder);
        }
    }

    function addSlabRow(
        prefix = ""
    ) {
        const container =
            $(
                prefix === "edit"
                    ? "edit-slab-rows"
                    : "slab-rows"
            );

        if (!container) return;

        const row =
            document.createElement("div");

        row.className =
            "slab-row";

        row.innerHTML = `
            <div class="field">
                <label>Lower bound</label>
                <input
                    type="number"
                    step="0.01"
                    min="0"
                    class="slab-lower"
                    placeholder="0">
            </div>

            <div class="field">
                <label>Upper bound</label>
                <input
                    type="number"
                    step="0.01"
                    min="0"
                    class="slab-upper"
                    placeholder="Open-ended">
            </div>

            <div class="field">
                <label>Price per unit</label>
                <input
                    type="number"
                    step="0.01"
                    min="0"
                    class="slab-price"
                    placeholder="10.00"
                    required>
            </div>

            <div class="slab-remove">
                <button
                    type="button"
                    class="btn btn-danger btn-sm"
                    data-action="remove-slab">
                    Remove
                </button>
            </div>
        `;

        container.appendChild(row);
    }

    function collectSlabs(
        prefix = ""
    ) {
        const container =
            $(
                prefix === "edit"
                    ? "edit-slab-rows"
                    : "slab-rows"
            );

        if (!container) return [];

        return Array.from(
            container.querySelectorAll(
                ".slab-row"
            )
        ).map((row) => {
            const lower =
                row.querySelector(
                    ".slab-lower"
                )?.value;

            const upper =
                row.querySelector(
                    ".slab-upper"
                )?.value;

            const price =
                row.querySelector(
                    ".slab-price"
                )?.value;

            return {
                lowerBound:
                    lower === ""
                        ? 0
                        : Number(lower),

                upperBound:
                    upper === ""
                        ? null
                        : Number(upper),

                pricePerUnit:
                    Number(price)
            };
        });
    }

    async function createPlan(event) {
        event.preventDefault();

        const form =
            event.currentTarget;

        const button =
            form.querySelector(
                'button[type="submit"]'
            );

        const type =
            $("plan-type").value;

        const payload = {
            name:
                $("plan-name")
                    .value.trim(),

            code:
                $("plan-code")
                    .value.trim(),

            description:
                $("plan-description")
                    .value.trim(),

            pricePerUnit:
                type === "FIXED"
                    ? parseFloat(
                        $("plan-price")
                            .value
                    )
                    : null,

            planType:
            type,

            slabs:
                type === "SLAB"
                    ? collectSlabs()
                    : []
        };

        setButtonLoading(
            button,
            true,
            "Creating..."
        );

        try {
            const result =
                await apiRequest(
                    ENDPOINTS.plans,
                    {
                        method: "POST",
                        body: JSON.stringify(
                            payload
                        )
                    }
                );

            showSuccess(
                responseSuccessMessage(result)
            );

            form.reset();

            $("slab-rows").innerHTML =
                "";

            updatePlanTypeUI();

            await loadAdminPlans();

        } catch (error) {
            // Error already displayed.
        } finally {
            setButtonLoading(
                button,
                false
            );
        }
    }

    function editPlanById(id) {
        const plan =
            state.plans.find(
                (item) =>
                    String(planId(item)) ===
                    String(id)
            );

        if (!plan) {
            showError(
                "The selected billing plan could not be found."
            );
            return;
        }

        state.currentPlan =
            plan;

        $("edit-plan-id").value =
            planId(plan);

        $("edit-plan-name").value =
            plan.name || "";

        $("edit-plan-type").value =
            plan.planType || "FIXED";

        $("edit-plan-description").value =
            plan.description || "";

        $("edit-plan-price").value =
            plan.pricePerUnit ??
            "";

        const rows =
            $("edit-slab-rows");

        rows.innerHTML =
            "";

        if (
            plan.planType === "SLAB" &&
            Array.isArray(plan.slabs)
        ) {
            plan.slabs.forEach(
                (slab) => {
                    addSlabRow("edit");

                    const row =
                        rows.lastElementChild;

                    row.querySelector(
                        ".slab-lower"
                    ).value =
                        slab.lowerBound ?? 0;

                    row.querySelector(
                        ".slab-upper"
                    ).value =
                        slab.upperBound ?? "";

                    row.querySelector(
                        ".slab-price"
                    ).value =
                        slab.pricePerUnit ?? "";
                }
            );
        }

        show(
            $("admin-plan-edit-panel")
        );

        updatePlanTypeUI("edit");

        $("admin-plan-edit-panel")
            .scrollIntoView({
                behavior: "smooth",
                block: "start"
            });
    }

    async function updatePlan(event) {
        event.preventDefault();

        const form =
            event.currentTarget;

        const button =
            form.querySelector(
                'button[type="submit"]'
            );

        const id =
            $("edit-plan-id")
                .value.trim();

        const type =
            $("edit-plan-type")
                .value;

        const payload = {
            name:
                $("edit-plan-name")
                    .value.trim(),

            description:
                $("edit-plan-description")
                    .value.trim(),

            planType:
            type,

            pricePerUnit:
                type === "FIXED"
                    ? parseFloat(
                        $("edit-plan-price")
                            .value
                    )
                    : null,

            slabs:
                type === "SLAB"
                    ? collectSlabs("edit")
                    : []
        };

        setButtonLoading(
            button,
            true,
            "Updating..."
        );

        try {
            const result =
                await apiRequest(
                    `${ENDPOINTS.plans}/${encodeURIComponent(id)}`,
                    {
                        method: "PUT",
                        body: JSON.stringify(
                            payload
                        )
                    }
                );

            showSuccess(
                responseSuccessMessage(result)
            );

            hide(
                $("admin-plan-edit-panel")
            );

            await loadAdminPlans();

        } catch (error) {
            // Error already displayed.
        } finally {
            setButtonLoading(
                button,
                false
            );
        }
    }

    async function deletePlanById(id) {
        const plan =
            state.plans.find(
                (item) =>
                    String(planId(item)) ===
                    String(id)
            );

        const name =
            plan?.name ||
            id;

        openConfirmModal({
            title: "Delete billing plan?",
            eyebrow: "Billing plans",
            body:
                `This will permanently delete "${name}".`,
            confirmText: "Delete plan",
            async onConfirm() {
                const result =
                    await apiRequest(
                        `${ENDPOINTS.plans}/${encodeURIComponent(id)}`,
                        {
                            method: "DELETE"
                        }
                    );

                showSuccess(
                    responseSuccessMessage(result)
                );

                await loadAdminPlans();
            }
        });
    }


    /* =========================================================
       ADMIN INVOICES
       ========================================================= */

    async function loadAdminInvoices(
        overviewOnly = false
    ) {
        const result =
            await apiRequest(
                ENDPOINTS.invoices,
                {
                    method: "GET"
                }
            );

        const data =
            responseData(result);

        const invoices =
            arrayFrom(data);

        state.invoices =
            invoices;

        renderAdminInvoices(
            invoices,
            overviewOnly
        );

        setText(
            "admin-invoice-count",
            invoices.length
        );

        return invoices;
    }

    function renderAdminInvoices(
        invoices,
        overviewOnly = false
    ) {
        const tableBody =
            $("admin-invoices-table");

        if (!tableBody) return;

        if (!invoices.length) {
            tableBody.innerHTML =
                emptyRow(
                    6,
                    "No invoices found."
                );
            return;
        }

        const sorted =
            [...invoices].sort(
                compareInvoicesDescending
            );

        tableBody.innerHTML =
            sorted
                .map((invoice) => `
                    <tr>
                        <td>
                            <strong>
                                ${escapeHtml(
                    invoiceId(invoice)
                )}
                            </strong>
                        </td>

                        <td>
                            ${escapeHtml(
                    customerName(invoice)
                )}
                        </td>

                        <td>
                            ${escapeHtml(
                    invoiceMeter(invoice)
                )}
                        </td>

                        <td>
                            ${escapeHtml(
                    invoicePeriod(invoice)
                )}
                        </td>

                        <td>
                            <strong>
                                ${escapeHtml(
                    formatCurrency(
                        invoiceAmount(
                            invoice
                        )
                    )
                )}
                            </strong>
                        </td>

                        <td>
                            <span class="badge">
                                ${escapeHtml(
                    invoiceStatus(
                        invoice
                    )
                )}
                            </span>
                        </td>
                    </tr>
                `)
                .join("");
    }


    /* =========================================================
       ADMIN BILLING JOB
       ========================================================= */

    async function startAdminBillingJob(
        event
    ) {
        event.preventDefault();

        const form =
            event.currentTarget;

        const button =
            form.querySelector(
                'button[type="submit"]'
            );

        const year =
            parseInt(
                $("admin-billing-year")
                    .value,
                10
            );

        const month =
            parseInt(
                $("admin-billing-month")
                    .value,
                10
            );

        setButtonLoading(
            button,
            true,
            "Starting..."
        );

        try {
            const result =
                await apiRequest(
                    ENDPOINTS.adminGenerateInvoice,
                    {
                        method: "POST",
                        body: JSON.stringify({
                            year,
                            month
                        })
                    }
                );

            const data =
                responseData(result);

            const jobId =
                firstDefined(
                    data?.jobId,
                    data?.id,
                    data?.generationJobId
                );

            showSuccess(
                responseSuccessMessage(result)
            );

            if (jobId) {
                state.lastJobId =
                    String(jobId);

                sessionStorage.setItem(
                    STORAGE_KEYS.lastJobId,
                    state.lastJobId
                );

                $("job-id").value =
                    state.lastJobId;

                await checkJobStatus(
                    state.lastJobId
                );

                startJobPolling(
                    state.lastJobId
                );
            }

            await loadAdminInvoices();

        } catch (error) {
            // Error already displayed.
        } finally {
            setButtonLoading(
                button,
                false
            );
        }
    }

    async function checkJobStatus(
        jobId = null
    ) {
        const id =
            jobId ||
            $("job-id")
                ?.value.trim();

        if (!id) {
            showError(
                "Job ID is required."
            );
            return;
        }

        try {
            const result =
                await apiRequest(
                    `${ENDPOINTS.adminGenerateInvoice.replace(
                        "/generate",
                        ""
                    )}/generation-jobs/${encodeURIComponent(id)}`,
                    {
                        method: "GET"
                    }
                );

            const data =
                responseData(result);

            renderJobStatus(
                id,
                data
            );

            if (!jobId) {
                showSuccess(
                    responseSuccessMessage(result)
                );
            }

            return data;

        } catch (error) {
            // Error already displayed.
        }
    }

    function renderJobStatus(
        jobId,
        data
    ) {
        show(
            $("job-status-card")
        );

        setText(
            "job-status-id",
            jobId
        );

        const status =
            firstDefined(
                data?.status,
                data?.state,
                data?.jobStatus,
                "UNKNOWN"
            );

        setText(
            "job-status-badge",
            status
        );

        $("job-status-json").textContent =
            safeJson(data);
    }

    function startJobPolling(
        jobId
    ) {
        if (state.jobPollingTimer) {
            clearInterval(
                state.jobPollingTimer
            );
        }

        let attempts = 0;

        state.jobPollingTimer =
            setInterval(
                async () => {
                    attempts++;

                    try {
                        const data =
                            await checkJobStatus(
                                jobId
                            );

                        const status =
                            String(
                                firstDefined(
                                    data?.status,
                                    data?.state,
                                    data?.jobStatus,
                                    ""
                                )
                            ).toUpperCase();

                        if (
                            [
                                "COMPLETED",
                                "SUCCESS",
                                "FAILED",
                                "ERROR",
                                "CANCELLED"
                            ].includes(status)
                        ) {
                            clearInterval(
                                state.jobPollingTimer
                            );

                            state.jobPollingTimer =
                                null;

                            await loadAdminInvoices();
                        }
                    } catch {
                        /*
                         * Keep polling until the maximum number
                         * of attempts is reached.
                         */
                    }

                    if (attempts >= 30) {
                        clearInterval(
                            state.jobPollingTimer
                        );

                        state.jobPollingTimer =
                            null;
                    }
                },
                3000
            );
    }

    function restoreLastJob() {
        if (
            state.lastJobId &&
            $("job-id")
        ) {
            $("job-id").value =
                state.lastJobId;
        }
    }


    /* =========================================================
       MONITORING
       ========================================================= */

    async function loadMonitoring() {
        await Promise.allSettled([
            loadHealth(),
            loadInfo(),
            loadMetrics()
        ]);
    }

    async function loadHealth(
        silent = false
    ) {
        const result =
            await apiRequest(
                ENDPOINTS.health,
                {
                    method: "GET"
                },
                {
                    silent
                }
            );

        const data =
            responseData(result);

        renderHealth(
            data
        );

        return data;
    }

    function renderHealth(data) {
        const status =
            firstDefined(
                data?.status,
                "UNKNOWN"
            );

        setText(
            "monitor-health-status",
            status
        );

        setText(
            "admin-health-status",
            status
        );

        const dot =
            $("monitor-health-dot");

        if (dot) {
            dot.className =
                "health-dot " +
                (
                    String(status)
                        .toUpperCase() ===
                    "UP"
                        ? "healthy"
                        : "unhealthy"
                );
        }

        const json =
            $("monitor-health-json");

        if (json) {
            json.textContent =
                safeJson(data);
        }
    }

    async function loadInfo() {
        const result =
            await apiRequest(
                ENDPOINTS.info,
                {
                    method: "GET"
                }
            );

        const data =
            responseData(result);

        const element =
            $("monitor-info-json");

        if (element) {
            element.textContent =
                safeJson(data);
        }

        return data;
    }

    async function loadMetrics() {
        const result =
            await apiRequest(
                ENDPOINTS.metrics,
                {
                    method: "GET"
                }
            );

        const data =
            responseData(result);

        const element =
            $("monitor-metrics-json");

        if (element) {
            element.textContent =
                safeJson(data);
        }

        return data;
    }


    /* =========================================================
       INVOICE DETAIL
       ========================================================= */

    function viewInvoice(id) {
        const invoice =
            state.invoices.find(
                (item) =>
                    String(
                        invoiceId(item)
                    ) === String(id)
            );

        if (!invoice) {
            showError(
                "Invoice details are not available."
            );
            return;
        }

        openConfirmModal({
            title: "Invoice details",
            eyebrow: "Invoice",
            body: `
                <div class="detail-list">
                    <div>
                        <span>Invoice ID</span>
                        <strong>${escapeHtml(
                invoiceId(invoice)
            )}</strong>
                    </div>

                    <div>
                        <span>Customer</span>
                        <strong>${escapeHtml(
                customerName(invoice)
            )}</strong>
                    </div>

                    <div>
                        <span>Meter</span>
                        <strong>${escapeHtml(
                invoiceMeter(invoice)
            )}</strong>
                    </div>

                    <div>
                        <span>Period</span>
                        <strong>${escapeHtml(
                invoicePeriod(invoice)
            )}</strong>
                    </div>

                    <div>
                        <span>Amount</span>
                        <strong>${escapeHtml(
                formatCurrency(
                    invoiceAmount(
                        invoice
                    )
                )
            )}</strong>
                    </div>

                    <div>
                        <span>Status</span>
                        <strong>${escapeHtml(
                invoiceStatus(
                    invoice
                )
            )}</strong>
                    </div>
                </div>
            `,
            confirmText: "Close",
            confirmClass: "btn-primary",
            hideCancel: true,
            onConfirm() {}
        });
    }


    /* =========================================================
       CONFIRMATION MODAL
       ========================================================= */

    let modalConfirmHandler =
        null;

    function openConfirmModal({
                                  title,
                                  eyebrow = "Confirmation",
                                  body,
                                  confirmText = "Confirm",
                                  confirmClass = "btn-danger",
                                  hideCancel = false,
                                  onConfirm
                              }) {
        setText(
            "modal-eyebrow",
            eyebrow
        );

        setText(
            "modal-title",
            title
        );

        const bodyElement =
            $("modal-body");

        if (bodyElement) {
            bodyElement.innerHTML =
                body;
        }

        const confirmButton =
            $("modal-confirm");

        confirmButton.textContent =
            confirmText;

        confirmButton.className =
            `btn ${confirmClass}`;

        if (hideCancel) {
            hide($("modal-cancel"));
        } else {
            show($("modal-cancel"));
        }

        modalConfirmHandler =
            onConfirm;

        show(
            $("modal-backdrop")
        );
    }

    function closeModal() {
        hide(
            $("modal-backdrop")
        );

        modalConfirmHandler =
            null;
    }

    async function confirmModalAction() {
        const handler =
            modalConfirmHandler;

        if (!handler) {
            closeModal();
            return;
        }

        const button =
            $("modal-confirm");

        setButtonLoading(
            button,
            true,
            "Working..."
        );

        try {
            await handler();
            closeModal();
        } catch (error) {
            /*
             * apiRequest already displays the
             * backend error.
             */
        } finally {
            setButtonLoading(
                button,
                false
            );
        }
    }


    /* =========================================================
       RESPONSIVE SIDEBAR
       ========================================================= */

    function openMobileSidebar() {
        $("sidebar")?.classList.add(
            "open"
        );

        show(
            $("sidebar-overlay")
        );
    }

    function closeMobileSidebar() {
        $("sidebar")?.classList.remove(
            "open"
        );

        hide(
            $("sidebar-overlay")
        );
    }


    /* =========================================================
       DATE / BILLING HELPERS
       ========================================================= */

    function initialiseBillingPeriod(
        yearId,
        monthId
    ) {
        const now =
            new Date();

        /*
         * Backend generates previous calendar month.
         */
        let year =
            now.getFullYear();

        let month =
            now.getMonth();

        if (month === 0) {
            year -= 1;
            month = 12;
        }

        const yearInput =
            $(yearId);

        const monthInput =
            $(monthId);

        if (yearInput) {
            yearInput.value =
                year;
        }

        if (monthInput) {
            monthInput.value =
                month;
        }
    }

    function setDefaultDateTime(
        id
    ) {
        const input =
            $(id);

        if (!input) return;

        const now =
            new Date();

        now.setMinutes(
            now.getMinutes() -
            now.getTimezoneOffset()
        );

        input.value =
            now.toISOString()
                .slice(0, 16);
    }

    function localDateTimeToIso(
        value
    ) {
        if (!value) {
            return null;
        }

        const date =
            new Date(value);

        return date.toISOString();
    }


    /* =========================================================
       ENTITY EXTRACTION HELPERS
       ========================================================= */

    function meterId(meter) {
        return firstDefined(
            meter?.id,
            meter?.waterMeterId
        );
    }

    function meterNumber(meter) {
        return firstDefined(
            meter?.meterNumber,
            meter?.number,
            meter?.meterNo,
            meterId(meter),
            "Meter"
        );
    }

    function billingPlanId(meter) {
        return firstDefined(
            meter?.billingPlanId,
            meter?.planId,
            meter?.billingPlan?.id,
            meter?.billingPlan?.billingPlanId
        );
    }

    function planName(meter) {
        return firstDefined(
            meter?.billingPlan?.name,
            meter?.plan?.name,
            meter?.billingPlanName,
            billingPlanId(meter),
            "—"
        );
    }

    function planId(plan) {
        return firstDefined(
            plan?.id,
            plan?.billingPlanId
        );
    }

    function invoiceId(invoice) {
        return firstDefined(
            invoice?.id,
            invoice?.invoiceId
        );
    }

    function invoiceAmount(invoice) {
        return firstDefined(
            invoice?.totalAmount,
            invoice?.amount,
            invoice?.invoiceAmount,
            invoice?.grandTotal,
            invoice?.total,
            0
        );
    }

    function invoiceStatus(invoice) {
        return firstDefined(
            invoice?.status,
            invoice?.invoiceStatus,
            "—"
        );
    }

    function customerName(invoice) {
        return firstDefined(
            invoice?.customer?.username,
            invoice?.customerUsername,
            invoice?.username,
            invoice?.user?.username,
            invoice?.userId,
            "—"
        );
    }

    function invoiceMeter(invoice) {
        return firstDefined(
            invoice?.waterMeter?.meterNumber,
            invoice?.meterNumber,
            invoice?.waterMeterNumber,
            invoice?.waterMeterId,
            invoice?.meterId,
            "—"
        );
    }

    function invoicePeriod(invoice) {
        const year =
            firstDefined(
                invoice?.year,
                invoice?.billingYear
            );

        const month =
            firstDefined(
                invoice?.month,
                invoice?.billingMonth
            );

        if (
            year !== undefined &&
            month !== undefined
        ) {
            return `${year}-${String(month).padStart(
                2,
                "0"
            )}`;
        }

        return firstDefined(
            invoice?.billingPeriod,
            invoice?.period,
            invoice?.billingMonth,
            "—"
        );
    }

    function compareInvoicesDescending(
        a,
        b
    ) {
        const aYear =
            Number(
                firstDefined(
                    a?.year,
                    a?.billingYear,
                    0
                )
            );

        const bYear =
            Number(
                firstDefined(
                    b?.year,
                    b?.billingYear,
                    0
                )
            );

        if (aYear !== bYear) {
            return bYear - aYear;
        }

        const aMonth =
            Number(
                firstDefined(
                    a?.month,
                    a?.billingMonth,
                    0
                )
            );

        const bMonth =
            Number(
                firstDefined(
                    b?.month,
                    b?.billingMonth,
                    0
                )
            );

        return bMonth - aMonth;
    }


    /* =========================================================
       FORMATTERS
       ========================================================= */

    function formatCurrency(value) {
        const number =
            Number(value);

        if (
            Number.isNaN(number)
        ) {
            return "₹0.00";
        }

        return new Intl.NumberFormat(
            "en-IN",
            {
                style: "currency",
                currency: "INR",
                minimumFractionDigits: 2
            }
        ).format(number);
    }

    function emptyRow(
        colspan,
        message
    ) {
        return `
            <tr>
                <td
                    colspan="${colspan}"
                    class="empty-state">
                    ${escapeHtml(message)}
                </td>
            </tr>
        `;
    }


    /* =========================================================
       EVENT DELEGATION
       ========================================================= */

    async function handleActionClick(
        event
    ) {
        const target =
            event.target.closest(
                "[data-action]"
            );

        if (!target) return;

        const action =
            target.dataset.action;

        switch (action) {

            case "load-my-meters":
                await loadMyMeters();
                break;

            case "load-customer-invoices":
                await loadCustomerInvoices();
                break;

            case "load-admin-meters":
                await loadAdminMeters();
                break;

            case "load-admin-plans":
                await loadAdminPlans();
                break;

            case "load-admin-invoices":
                await loadAdminInvoices();
                break;

            case "load-monitoring":
                await loadMonitoring();
                break;

            case "edit-current-user":
                editCurrentUser();
                break;

            case "delete-current-user":
                await deleteCurrentUser();
                break;

            case "close-user-edit":
                hide(
                    $("admin-user-edit-panel")
                );
                break;

            case "lookup-meter":
                await lookupMeterById(
                    target.dataset.meterId
                );
                break;

            case "edit-current-meter":
                if (
                    state.currentMeter
                ) {
                    await editMeterById(
                        meterId(
                            state.currentMeter
                        )
                    );
                }
                break;

            case "edit-meter":
                await editMeterById(
                    target.dataset.meterId
                );
                break;

            case "delete-current-meter":
                if (
                    state.currentMeter
                ) {
                    await deleteMeterById(
                        meterId(
                            state.currentMeter
                        )
                    );
                }
                break;

            case "delete-meter":
                await deleteMeterById(
                    target.dataset.meterId
                );
                break;

            case "close-meter-edit":
                hide(
                    $("admin-meter-edit-panel")
                );
                break;

            case "edit-plan":
                editPlanById(
                    target.dataset.planId
                );
                break;

            case "delete-plan":
                await deletePlanById(
                    target.dataset.planId
                );
                break;

            case "close-plan-edit":
                hide(
                    $("admin-plan-edit-panel")
                );
                break;

            case "add-slab":
                addSlabRow();
                break;

            case "add-edit-slab":
                addSlabRow("edit");
                break;

            case "remove-slab":
                target
                    .closest(".slab-row")
                    ?.remove();
                break;

            case "use-meter-reading":
                $("customer-reading-meter")
                    .value =
                    target.dataset.meterId;

                navigateTo(
                    "customer-reading"
                );
                break;

            case "view-invoice":
                viewInvoice(
                    target.dataset.invoiceId
                );
                break;
        }
    }


    /* =========================================================
       NAVIGATION EVENTS
       ========================================================= */

    function handleSectionTargetClick(
        event
    ) {
        const target =
            event.target.closest(
                "[data-section-target]"
            );

        if (!target) return;

        const section =
            target.dataset.sectionTarget;

        if (!section) return;

        navigateTo(section);
    }

    function handleNavClick(event) {
        const button =
            event.target.closest(
                ".nav-item[data-section]"
            );

        if (!button) return;

        navigateTo(
            button.dataset.section
        );
    }


    /* =========================================================
       FORM RESET HANDLING
       ========================================================= */

    function handleFormReset(event) {
        const form =
            event.target;

        setTimeout(() => {

            if (
                form.id ===
                "customer-reading-form"
            ) {
                setDefaultDateTime(
                    "customer-reading-at"
                );
            }

            if (
                form.id ===
                "admin-reading-form"
            ) {
                setDefaultDateTime(
                    "admin-reading-at"
                );
            }

            if (
                form.id ===
                "admin-create-plan-form"
            ) {
                $("slab-rows").innerHTML =
                    "";

                updatePlanTypeUI();
            }

        }, 0);
    }


    /* =========================================================
       INITIAL EVENT BINDING
       ========================================================= */

    function bindEvents() {

        $("login-form")
            ?.addEventListener(
                "submit",
                handleLogin
            );

        $("logout-btn")
            ?.addEventListener(
                "click",
                logout
            );

        $("account-logout-btn")
            ?.addEventListener(
                "click",
                logout
            );

        $("sidebar-open")
            ?.addEventListener(
                "click",
                openMobileSidebar
            );

        $("sidebar-close")
            ?.addEventListener(
                "click",
                closeMobileSidebar
            );

        $("sidebar-overlay")
            ?.addEventListener(
                "click",
                closeMobileSidebar
            );

        $("refresh-current")
            ?.addEventListener(
                "click",
                () => {
                    if (
                        state.currentSection
                    ) {
                        loadSectionData(
                            state.currentSection
                        );
                    }
                }
            );


        /* Navigation */

        document.addEventListener(
            "click",
            handleNavClick
        );

        document.addEventListener(
            "click",
            handleSectionTargetClick
        );

        document.addEventListener(
            "click",
            handleActionClick
        );


        /* Customer */

        $("customer-reading-form")
            ?.addEventListener(
                "submit",
                submitCustomerReading
            );

        $("customer-generate-form")
            ?.addEventListener(
                "submit",
                generateCustomerInvoice
            );


        /* Admin users */

        $("admin-create-user-form")
            ?.addEventListener(
                "submit",
                createUser
            );

        $("admin-user-lookup-form")
            ?.addEventListener(
                "submit",
                lookupUser
            );

        $("admin-edit-user-form")
            ?.addEventListener(
                "submit",
                updateUser
            );


        /* Admin meters */

        $("admin-create-meter-form")
            ?.addEventListener(
                "submit",
                createMeter
            );

        $("admin-meter-lookup-form")
            ?.addEventListener(
                "submit",
                lookupMeter
            );

        $("admin-edit-meter-form")
            ?.addEventListener(
                "submit",
                updateMeter
            );


        /* Assignments */

        $("admin-assignment-form")
            ?.addEventListener(
                "submit",
                assignMeter
            );

        $("admin-unassignment-form")
            ?.addEventListener(
                "submit",
                unassignMeter
            );


        /* Admin readings */

        $("admin-reading-form")
            ?.addEventListener(
                "submit",
                submitAdminReading
            );


        /* Plans */

        $("admin-create-plan-form")
            ?.addEventListener(
                "submit",
                createPlan
            );

        $("admin-edit-plan-form")
            ?.addEventListener(
                "submit",
                updatePlan
            );

        $("plan-type")
            ?.addEventListener(
                "change",
                () => updatePlanTypeUI()
            );

        $("edit-plan-type")
            ?.addEventListener(
                "change",
                () => updatePlanTypeUI("edit")
            );


        /* Billing */

        $("admin-generate-form")
            ?.addEventListener(
                "submit",
                startAdminBillingJob
            );

        $("admin-job-status-form")
            ?.addEventListener(
                "submit",
                async (event) => {
                    event.preventDefault();

                    await checkJobStatus();
                }
            );


        /* Modal */

        $("modal-close")
            ?.addEventListener(
                "click",
                closeModal
            );

        $("modal-cancel")
            ?.addEventListener(
                "click",
                closeModal
            );

        $("modal-confirm")
            ?.addEventListener(
                "click",
                confirmModalAction
            );

        $("modal-backdrop")
            ?.addEventListener(
                "click",
                (event) => {
                    if (
                        event.target ===
                        $("modal-backdrop")
                    ) {
                        closeModal();
                    }
                }
            );


        /* Reset */

        document.addEventListener(
            "reset",
            handleFormReset
        );


        /* Keyboard */

        document.addEventListener(
            "keydown",
            (event) => {
                if (
                    event.key === "Escape"
                ) {
                    closeModal();
                    closeMobileSidebar();
                }
            }
        );
    }


    /* =========================================================
       APPLICATION INITIALIZATION
       ========================================================= */

    async function initialise() {
        loadSession();

        bindEvents();

        setDefaultDateTime(
            "customer-reading-at"
        );

        setDefaultDateTime(
            "admin-reading-at"
        );

        initialiseBillingPeriod(
            "customer-billing-year",
            "customer-billing-month"
        );

        initialiseBillingPeriod(
            "admin-billing-year",
            "admin-billing-month"
        );

        updatePlanTypeUI();
        updatePlanTypeUI("edit");

        if (isAuthenticated()) {
            showAppView();
        } else {
            showLoginView();
        }
    }


    /* =========================================================
       START APPLICATION
       ========================================================= */

    if (
        document.readyState ===
        "loading"
    ) {
        document.addEventListener(
            "DOMContentLoaded",
            initialise
        );
    } else {
        initialise();
    }

})();
