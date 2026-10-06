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

        plans: "/api/billing-plans",

        invoices: "/api/invoices",
        generateInvoice: "/api/invoices/generate",

        adminGenerateInvoice: "/api/admin/invoices/generate",

        health: "/actuator/health",
        info: "/actuator/info",
        metrics: "/actuator/metrics"
    };


    /* =========================================================
       APPLICATION STATE
       ========================================================= */

    /*
     * IMPORTANT:
     *
     * Authentication is intentionally kept only in JavaScript
     * memory.
     *
     * No localStorage.
     * No sessionStorage.
     *
     * Refreshing the page therefore logs the user out.
     */

    const state = {
        sessionId: null,
        username: null,
        role: null,

        currentSection: null,

        myMeters: [],
        adminMeters: [],
        customers: [],
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
        if (
            value === null ||
            value === undefined
        ) {
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
            return JSON.stringify(
                value,
                null,
                2
            );
        } catch {
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

        if (
            !value ||
            typeof value !== "object"
        ) {
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
       SESSION
       ========================================================= */

    function saveSession(
        sessionId,
        username,
        role
    ) {
        state.sessionId = sessionId;
        state.username = username;
        state.role = role;
    }

    function clearSession() {
        state.sessionId = null;
        state.username = null;
        state.role = null;
        state.currentSection = null;

        state.myMeters = [];
        state.adminMeters = [];
        state.customers = [];
        state.plans = [];
        state.invoices = [];

        state.currentUser = null;
        state.currentMeter = null;
        state.currentPlan = null;

        state.lastJobId = null;

        if (state.jobPollingTimer) {
            clearInterval(
                state.jobPollingTimer
            );

            state.jobPollingTimer = null;
        }
    }

    function isAuthenticated() {
        return Boolean(
            state.sessionId
        );
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

        if (
            !skipAuth &&
            state.sessionId
        ) {
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
            response.headers.get(
                "content-type"
            ) || "";

        try {
            if (
                contentType.includes(
                    "application/json"
                )
            ) {
                body = await response.json();
            } else {
                const text =
                    await response.text();

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

        if (!response.ok) {
            const backendMessage =
                body &&
                typeof body === "object" &&
                body.errorMessage;

            const message =
                backendMessage ||
                `Request failed with HTTP ${response.status}.`;

            if (!silent) {
                showError(message);
            }

            if (
                response.status === 401 ||
                response.status === 403
            ) {
                setTimeout(() => {
                    clearSession();
                    showLoginView();
                }, 800);
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
            ) &&
            body.success !== true
        ) {
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
       TOASTS / ALERTS
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
            toast.classList.remove(
                "show"
            );

            setTimeout(() => {
                toast.remove();
            }, 250);
        }, duration);
    }

    function showSuccess(message) {
        showToast(
            message,
            "success"
        );
    }

    function showError(message) {
        showToast(
            message,
            "error",
            6000
        );

        const loginError =
            $("login-error");

        if (
            loginError &&
            $("login-view") &&
            !$("login-view").classList.contains(
                "hidden"
            )
        ) {
            loginError.textContent =
                message;

            show(loginError);
        }
    }

    function showInfo(message) {
        showToast(
            message,
            "info"
        );
    }

    function setGlobalAlert(
        message,
        type = "info"
    ) {
        const element =
            $("global-alert");

        if (!element) return;

        element.textContent =
            message;

        element.className =
            `global-alert ${type}`;

        show(element);
    }

    function clearGlobalAlert() {
        hide(
            $("global-alert")
        );
    }


    /* =========================================================
       BUTTON LOADING
       ========================================================= */

    function setButtonLoading(
        button,
        loading,
        loadingText = "Working..."
    ) {
        if (!button) return;

        if (loading) {
            if (
                !button.dataset.originalText
            ) {
                button.dataset.originalText =
                    button.textContent.trim();
            }

            button.disabled = true;
            button.textContent =
                loadingText;
        } else {
            button.disabled = false;

            if (
                button.dataset.originalText
            ) {
                button.textContent =
                    button.dataset.originalText;

                delete button.dataset
                    .originalText;
            }
        }
    }


    /* =========================================================
       VIEW MANAGEMENT
       ========================================================= */

    function showLoginView() {
        show(
            $("login-view")
        );

        hide(
            $("app-view")
        );

        const error =
            $("login-error");

        if (error) {
            error.textContent = "";
            hide(error);
        }

        document.title =
            "Billing Software - Login";
    }

    function showAppView() {
        if (!isAuthenticated()) {
            showLoginView();
            return;
        }

        if (
            !isAdmin() &&
            !isCustomer()
        ) {
            clearSession();
            showLoginView();
            return;
        }

        hide(
            $("login-view")
        );

        show(
            $("app-view")
        );

        updateSessionUI();

        if (isAdmin()) {
            show(
                $("admin-nav")
            );

            hide(
                $("customer-nav")
            );

            navigateTo(
                "admin-overview"
            );

            return;
        }

        show(
            $("customer-nav")
        );

        hide(
            $("admin-nav")
        );

        navigateTo(
            "customer-overview"
        );
    }


    /* =========================================================
       ROLE / NAVIGATION GUARDS
       ========================================================= */

    function canAccessSection(
        sectionName
    ) {
        if (
            sectionName.startsWith(
                "admin-"
            )
        ) {
            return isAdmin();
        }

        if (
            sectionName.startsWith(
                "customer-"
            )
        ) {
            return isCustomer();
        }

        return false;
    }

    function navigateTo(
        sectionName
    ) {
        if (!isAuthenticated()) {
            showLoginView();
            return;
        }

        if (
            !canAccessSection(
                sectionName
            )
        ) {
            showError(
                "You are not authorized to access this section."
            );

            return;
        }

        const section =
            $(`section-${sectionName}`);

        if (!section) {
            return;
        }

        $$(".page-section")
            .forEach((item) => {
                item.classList.remove(
                    "active"
                );
            });

        section.classList.add(
            "active"
        );

        state.currentSection =
            sectionName;

        updateNavigationState(
            sectionName
        );

        updatePageHeader(
            sectionName
        );

        closeMobileSidebar();

        loadSectionData(
            sectionName
        );
    }

    function updateNavigationState(
        sectionName
    ) {
        $$(".nav-item[data-section]")
            .forEach((button) => {
                button.classList.toggle(
                    "active",
                    button.dataset.section ===
                        sectionName
                );
            });
    }

    function updatePageHeader(
        sectionName
    ) {
        const titles = {
            "customer-overview":
                [
                    "Customer",
                    "Overview"
                ],

            "customer-meters":
                [
                    "Customer",
                    "My Water Meters"
                ],

            "customer-invoices":
                [
                    "Customer",
                    "My Invoices"
                ],

            "customer-generate":
                [
                    "Customer",
                    "Generate Invoice"
                ],

            "customer-account":
                [
                    "Customer",
                    "Account"
                ],

            "admin-overview":
                [
                    "Administrator",
                    "Overview"
                ],

            "admin-users":
                [
                    "Administrator",
                    "Users"
                ],

            "admin-meters":
                [
                    "Administrator",
                    "Water Meters"
                ],

            "admin-assignments":
                [
                    "Administrator",
                    "Assignments"
                ],

            "admin-plans":
                [
                    "Administrator",
                    "Billing Plans"
                ],

            "admin-invoices":
                [
                    "Administrator",
                    "Invoices"
                ],

            "admin-billing":
                [
                    "Administrator",
                    "Billing Jobs"
                ],

            "admin-monitoring":
                [
                    "Administrator",
                    "Monitoring"
                ]
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
            username
                .charAt(0)
                .toUpperCase();

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
            isAdmin()
                ? "Administrator"
                : "Customer"
        );
    }


    /* =========================================================
       LOGIN
       ========================================================= */

    async function handleLogin(
        event
    ) {
        event.preventDefault();

        const form =
            event.currentTarget;

        const button =
            form.querySelector(
                'button[type="submit"]'
            );

        const username =
            $("login-username")
                ?.value
                .trim();

        const password =
            $("login-password")
                ?.value;

        hide(
            $("login-error")
        );

        clearGlobalAlert();

        if (
            !username ||
            !password
        ) {
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
                        body:
                            JSON.stringify({
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

            /*
             * The backend is authoritative.
             *
             * There is deliberately NO role
             * selector and NO frontend fallback.
             */
            const sessionId =
                data?.sessionId;

            const role =
                data?.role;

            if (
                !sessionId ||
                !role
            ) {
                throw {
                    type: "CLIENT",
                    message:
                        "Login succeeded but the backend did not return a session ID and role."
                };
            }

            if (
                ![
                    "ADMIN",
                    "CUSTOMER"
                ].includes(role)
            ) {
                throw {
                    type: "CLIENT",
                    message:
                        `Unsupported user role returned by backend: ${role}`
                };
            }

            saveSession(
                sessionId,
                username,
                role
            );

            form.reset();

            showSuccess(
                responseSuccessMessage(
                    result
                )
            );

            showAppView();

        } catch (error) {
            if (
                error?.type ===
                "CLIENT"
            ) {
                showError(
                    error.message
                );
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
                await apiRequest(
                    ENDPOINTS.logout,
                    {
                        method: "POST"
                    },
                    {
                        silent: true
                    }
                );
            }
        } catch {
            /*
             * Local session is cleared even if
             * backend logout fails.
             */
        } finally {
            clearSession();
            showLoginView();
        }
    }


    /* =========================================================
       SECTION LOADING
       ========================================================= */

    async function loadSectionData(
        sectionName
    ) {
        if (
            !isAuthenticated() ||
            !canAccessSection(sectionName)
        ) {
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

                case "customer-invoices":
                    await loadCustomerInvoices();
                    break;

                case "customer-generate":
                    initialiseBillingPeriod(
                        "customer-billing-year",
                        "customer-billing-month"
                    );
                    break;

                case "customer-account":
                    updateSessionUI();
                    break;

                case "admin-overview":
                    await loadAdminOverview();
                    break;

                case "admin-users":
                    await loadAdminCustomers();
                    break;

                case "admin-meters":
                    await Promise.all([
                        loadAdminMeters(),
                        loadAdminPlans()
                    ]);
                    break;

                case "admin-assignments":
                    await Promise.all([
                        loadAdminCustomers(),
                        loadAdminMeters()
                    ]);
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
                    break;

                case "admin-monitoring":
                    await loadMonitoring();
                    break;
            }
        } catch {
            /*
             * apiRequest already displays the
             * backend error.
             */
        }
    }


    /* =========================================================
       CUSTOMER OVERVIEW
       ========================================================= */

    async function loadCustomerOverview() {
        const [
            metersResult,
            invoicesResult
        ] = await Promise.all([
            apiRequest(
                ENDPOINTS.myMeters,
                { method: "GET" }
            ),
            apiRequest(
                ENDPOINTS.invoices,
                { method: "GET" }
            )
        ]);

        state.myMeters =
            arrayFrom(
                responseData(
                    metersResult
                )
            );

        state.invoices =
            arrayFrom(
                responseData(
                    invoicesResult
                )
            );

        renderCustomerMeters(
            state.myMeters
        );

        renderCustomerInvoices(
            state.invoices
        );

        setText(
            "customer-meter-count",
            state.myMeters.length
        );

        setText(
            "customer-invoice-count",
            state.invoices.length
        );

        const sortedInvoices =
            [...state.invoices]
                .sort(
                    compareInvoicesDescending
                );

        if (
            sortedInvoices.length
        ) {
            setText(
                "customer-latest-invoice",
                formatCurrency(
                    invoiceAmount(
                        sortedInvoices[0]
                    )
                )
            );
        } else {
            setText(
                "customer-latest-invoice",
                "—"
            );
        }
    }


    /* =========================================================
       CUSTOMER METERS
       ========================================================= */

    async function loadMyMeters() {
        const result =
            await apiRequest(
                ENDPOINTS.myMeters,
                {
                    method: "GET"
                }
            );

        state.myMeters =
            arrayFrom(
                responseData(result)
            );

        renderCustomerMeters(
            state.myMeters
        );

        setText(
            "customer-meter-count",
            state.myMeters.length
        );

        return state.myMeters;
    }

    function renderCustomerMeters(
        meters
    ) {
        const overviewBody =
            $("customer-overview-meters");

        const tableBody =
            $("customer-meters-table");

        const rows =
            meters.length
                ? meters
                    .map(
                        (meter) => `
                            <tr>
                                <td>
                                    <strong>
                                        ${escapeHtml(
                                            meterNumber(
                                                meter
                                            )
                                        )}
                                    </strong>
                                </td>

                                <td>
                                    ${escapeHtml(
                                        planName(
                                            meter
                                        )
                                    )}
                                </td>

                                <td>
                                    <span class="badge badge-success">
                                        Assigned
                                    </span>
                                </td>
                            </tr>
                        `
                    )
                    .join("")
                : emptyRow(
                    3,
                    "No water meters are assigned to your account."
                );

        if (overviewBody) {
            overviewBody.innerHTML =
                meters.length
                    ? meters
                        .slice(0, 5)
                        .map(
                            (meter) => `
                                <tr>
                                    <td>
                                        <strong>
                                            ${escapeHtml(
                                                meterNumber(
                                                    meter
                                                )
                                            )}
                                        </strong>
                                    </td>

                                    <td>
                                        ${escapeHtml(
                                            planName(
                                                meter
                                            )
                                        )}
                                    </td>

                                    <td>
                                        <span class="badge badge-success">
                                            Assigned
                                        </span>
                                    </td>
                                </tr>
                            `
                        )
                        .join("")
                    : emptyRow(
                        3,
                        "No water meters are assigned to your account."
                    );
        }

        if (tableBody) {
            tableBody.innerHTML =
                rows;
        }
    }


    /* =========================================================
       CUSTOMER INVOICES
       ========================================================= */

    async function loadCustomerInvoices() {
        const result =
            await apiRequest(
                ENDPOINTS.invoices,
                {
                    method: "GET"
                }
            );

        state.invoices =
            arrayFrom(
                responseData(result)
            );

        renderCustomerInvoices(
            state.invoices
        );

        setText(
            "customer-invoice-count",
            state.invoices.length
        );

        return state.invoices;
    }

    function renderCustomerInvoices(
        invoices
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
            overviewBody.innerHTML =
                sorted.length
                    ? sorted
                        .slice(0, 5)
                        .map(
                            (invoice) => `
                                <tr>
                                    <td>
                                        <strong>
                                            ${escapeHtml(
                                                invoicePeriod(
                                                    invoice
                                                )
                                            )}
                                        </strong>
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
                            `
                        )
                        .join("")
                    : emptyRow(
                        3,
                        "No invoices found."
                    );
        }

        if (tableBody) {
            tableBody.innerHTML =
                sorted.length
                    ? sorted
                        .map(
                            (invoice) => `
                                <tr>
                                    <td>
                                        <strong>
                                            Invoice
                                        </strong>
                                    </td>

                                    <td>
                                        ${escapeHtml(
                                            customerName(
                                                invoice
                                            )
                                        )}
                                    </td>

                                    <td>
                                        ${escapeHtml(
                                            invoiceMeter(
                                                invoice
                                            )
                                        )}
                                    </td>

                                    <td>
                                        ${escapeHtml(
                                            invoicePeriod(
                                                invoice
                                            )
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
                                                invoiceId(
                                                    invoice
                                                )
                                            )}">
                                            View
                                        </button>
                                    </td>
                                </tr>
                            `
                        )
                        .join("")
                    : emptyRow(
                        7,
                        "No invoices found."
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

        if (!isCustomer()) {
            showError(
                "You are not authorized to generate customer invoices."
            );
            return;
        }

        const form =
            event.currentTarget;

        const button =
            form.querySelector(
                'button[type="submit"]'
            );

        const year =
            Number(
                $("customer-billing-year")
                    ?.value
            );

        const month =
            Number(
                $("customer-billing-month")
                    ?.value
            );

        if (
            !year ||
            !month
        ) {
            showError(
                "Billing year and month are required."
            );
            return;
        }

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
                        body:
                            JSON.stringify({
                                year,
                                month
                            })
                    }
                );

            showSuccess(
                responseSuccessMessage(
                    result
                )
            );

            await loadCustomerInvoices();

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
                loadAdminMeters(),
                loadAdminPlans(),
                loadAdminInvoices(),
                loadHealth()
            ]);

        /*
         * Individual loaders already display their
         * backend errors. We intentionally do not
         * fabricate overview data.
         */

        return results;
    }


    /* =========================================================
       ADMIN CUSTOMERS
       ========================================================= */

    async function loadAdminCustomers() {
        if (!isAdmin()) {
            return [];
        }

        const result =
            await apiRequest(
                ENDPOINTS.users,
                {
                    method: "GET"
                }
            );

        state.customers =
            arrayFrom(
                responseData(result)
            );

        populateCustomerSelects(
            state.customers
        );

        return state.customers;
    }

    function populateCustomerSelects(
        customers
    ) {
        const selects = [
            $("assignment-user-id"),
            $("unassignment-user-id")
        ];

        selects.forEach(
            (select) => {
                if (!select) {
                    return;
                }

                const current =
                    select.value;

                select.innerHTML = `
                    <option value="">
                        Select customer
                    </option>
                `;

                customers.forEach(
                    (customer) => {
                        const id =
                            firstDefined(
                                customer?.id,
                                customer?.userId
                            );

                        const username =
                            firstDefined(
                                customer?.username,
                                customer?.name,
                                "Customer"
                            );

                        if (!id) {
                            return;
                        }

                        const option =
                            document.createElement(
                                "option"
                            );

                        /*
                         * UUID is used internally
                         * as the option value.
                         *
                         * The user sees ONLY username.
                         */
                        option.value =
                            String(id);

                        option.textContent =
                            username;

                        select.appendChild(
                            option
                        );
                    }
                );

                if (current) {
                    select.value =
                        current;
                }
            }
        );
    }


    /* =========================================================
       ADMIN USER MANAGEMENT
       ========================================================= */

    async function createUser(
        event
    ) {
        event.preventDefault();

        if (!isAdmin()) {
            showError(
                "Administrator access is required."
            );
            return;
        }

        const form =
            event.currentTarget;

        const button =
            form.querySelector(
                'button[type="submit"]'
            );

        const username =
            $("create-user-username")
                ?.value
                .trim();

        const password =
            $("create-user-password")
                ?.value;

        const role =
            $("create-user-role")
                ?.value;

        if (
            !username ||
            !password ||
            !role
        ) {
            showError(
                "Username, password and role are required."
            );
            return;
        }

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
                        body:
                            JSON.stringify({
                                username,
                                password,
                                role
                            })
                    }
                );

            showSuccess(
                responseSuccessMessage(
                    result
                )
            );

            form.reset();

            await loadAdminCustomers();

        } finally {
            setButtonLoading(
                button,
                false
            );
        }
    }

    async function lookupUser(
        event
    ) {
        event.preventDefault();

        if (!isAdmin()) {
            showError(
                "Administrator access is required."
            );
            return;
        }

        const id =
            $("user-lookup-id")
                ?.value
                .trim();

        if (!id) {
            showError(
                "User ID is required."
            );
            return;
        }

        try {
            const result =
                await apiRequest(
                    `${ENDPOINTS.users}/${encodeURIComponent(
                        id
                    )}`,
                    {
                        method: "GET"
                    }
                );

            const user =
                responseData(result);

            state.currentUser =
                user;

            renderUserDetail(
                user
            );

        } catch {
            /*
             * Backend error already shown.
             */
        }
    }

    function renderUserDetail(
        user
    ) {
        if (!user) {
            return;
        }

        show(
            $("user-detail-card")
        );

        setText(
            "user-detail-title",
            firstDefined(
                user.username,
                user.name,
                "User"
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

        /*
         * Deliberately do NOT render user.id.
         */
    }

    function editCurrentUser() {
        const user =
            state.currentUser;

        if (!user) {
            showInfo(
                "Look up a user first."
            );
            return;
        }

        const id =
            firstDefined(
                user.id,
                user.userId
            );

        if (!id) {
            showError(
                "The backend did not return the user identifier."
            );
            return;
        }

        $("edit-user-id").value =
            id;

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
    }

    async function updateUser(
        event
    ) {
        event.preventDefault();

        if (!isAdmin()) {
            showError(
                "Administrator access is required."
            );
            return;
        }

        const id =
            $("edit-user-id")
                ?.value
                .trim();

        const username =
            $("edit-user-username")
                ?.value
                .trim();

        const role =
            $("edit-user-role")
                ?.value;

        const password =
            $("edit-user-password")
                ?.value;

        if (
            !id ||
            !username ||
            !role
        ) {
            showError(
                "Username and role are required."
            );
            return;
        }

        const payload = {
            username,
            role
        };

        if (password) {
            payload.password =
                password;
        }

        const button =
            event.currentTarget
                .querySelector(
                    'button[type="submit"]'
                );

        setButtonLoading(
            button,
            true,
            "Updating..."
        );

        try {
            const result =
                await apiRequest(
                    `${ENDPOINTS.users}/${encodeURIComponent(
                        id
                    )}`,
                    {
                        method: "PUT",
                        body:
                            JSON.stringify(
                                payload
                            )
                    }
                );

            showSuccess(
                responseSuccessMessage(
                    result
                )
            );

            hide(
                $("admin-user-edit-panel")
            );

            await loadAdminCustomers();

            /*
             * Refresh lookup result if the
             * currently selected user is still active.
             */
            await lookupUserById(
                id
            );

        } finally {
            setButtonLoading(
                button,
                false
            );
        }
    }

    async function lookupUserById(
        id
    ) {
        if (!id) {
            return;
        }

        const result =
            await apiRequest(
                `${ENDPOINTS.users}/${encodeURIComponent(
                    id
                )}`,
                {
                    method: "GET"
                },
                {
                    silent: true
                }
            );

        const user =
            responseData(result);

        state.currentUser =
            user;

        renderUserDetail(
            user
        );
    }

    async function deleteCurrentUser() {
        if (!state.currentUser) {
            showInfo(
                "Look up a user first."
            );
            return;
        }

        const id =
            firstDefined(
                state.currentUser.id,
                state.currentUser.userId
            );

        if (!id) {
            showError(
                "The backend did not return the user identifier."
            );
            return;
        }

        await deleteUserById(
            id
        );
    }

    async function deleteUserById(
        id
    ) {
        await openConfirmModal(
            "Delete user",
            "This will deactivate the selected user.",
            async () => {
                const result =
                    await apiRequest(
                        `${ENDPOINTS.users}/${encodeURIComponent(
                            id
                        )}`,
                        {
                            method: "DELETE"
                        }
                    );

                showSuccess(
                    responseSuccessMessage(
                        result
                    )
                );

                state.currentUser =
                    null;

                hide(
                    $("user-detail-card")
                );

                hide(
                    $("admin-user-edit-panel")
                );

                await loadAdminCustomers();
            }
        );
    }


    /* =========================================================
       ADMIN METERS
       ========================================================= */

    async function loadAdminMeters() {
        if (!isAdmin()) {
            return [];
        }

        const result =
            await apiRequest(
                ENDPOINTS.meters,
                {
                    method: "GET"
                }
            );

        state.adminMeters =
            arrayFrom(
                responseData(result)
            );

        renderAdminMeters(
            state.adminMeters
        );

        populateAssignmentMeterSelects(
            state.adminMeters
        );

        setText(
            "admin-meter-count",
            state.adminMeters.length
        );

        return state.adminMeters;
    }

    function populateAssignmentMeterSelects(
        meters
    ) {
        const selects = [
            $("assignment-meter-id"),
            $("unassignment-meter-id")
        ];

        selects.forEach(
            (select) => {
                if (!select) {
                    return;
                }

                const current =
                    select.value;

                select.innerHTML = `
                    <option value="">
                        Select water meter
                    </option>
                `;

                meters.forEach(
                    (meter) => {
                        const id =
                            meterId(meter);

                        if (!id) {
                            return;
                        }

                        const option =
                            document.createElement(
                                "option"
                            );

                        /*
                         * UUID stays internal.
                         */
                        option.value =
                            String(id);

                        /*
                         * User sees meter number only.
                         */
                        option.textContent =
                            meterNumber(
                                meter
                            );

                        select.appendChild(
                            option
                        );
                    }
                );

                if (current) {
                    select.value =
                        current;
                }
            }
        );
    }

    function renderAdminMeters(
        meters
    ) {
        const body =
            $("admin-meters-table");

        const overview =
            $("admin-overview-meters");

        if (body) {
            body.innerHTML =
                meters.length
                    ? meters
                        .map(
                            (meter) => `
                                <tr>
                                    <td>
                                        <strong>
                                            ${escapeHtml(
                                                meterNumber(
                                                    meter
                                                )
                                            )}
                                        </strong>
                                    </td>

                                    <td>
                                        ${escapeHtml(
                                            planName(
                                                meter
                                            )
                                        )}
                                    </td>

                                    <td>
                                        <button
                                            type="button"
                                            class="btn btn-ghost btn-sm"
                                            data-action="edit-meter"
                                            data-meter-id="${escapeHtml(
                                                meterId(
                                                    meter
                                                )
                                            )}">
                                            Edit
                                        </button>

                                        <button
                                            type="button"
                                            class="btn btn-danger btn-sm"
                                            data-action="delete-meter"
                                            data-meter-id="${escapeHtml(
                                                meterId(
                                                    meter
                                                )
                                            )}">
                                            Delete
                                        </button>
                                    </td>
                                </tr>
                            `
                        )
                        .join("")
                    : emptyRow(
                        3,
                        "No water meters found."
                    );
        }

        if (overview) {
            overview.innerHTML =
                meters.length
                    ? meters
                        .slice(0, 5)
                        .map(
                            (meter) => `
                                <tr>
                                    <td>
                                        <strong>
                                            ${escapeHtml(
                                                meterNumber(
                                                    meter
                                                )
                                            )}
                                        </strong>
                                    </td>

                                    <td>
                                        ${escapeHtml(
                                            planName(
                                                meter
                                            )
                                        )}
                                    </td>

                                    <td>
                                        <span class="badge">
                                            ${escapeHtml(
                                                meterAssignmentStatus(
                                                    meter
                                                )
                                            )}
                                        </span>
                                    </td>
                                </tr>
                            `
                        )
                        .join("")
                    : emptyRow(
                        3,
                        "No water meters found."
                    );
        }
    }

    function meterAssignmentStatus(
        meter
    ) {
        return firstDefined(
            meter?.username,
            meter?.customerUsername,
            meter?.assignment?.username,
            meter?.assignedTo?.username,
            "Available"
        );
    }

    async function lookupMeter(
        event
    ) {
        event.preventDefault();

        if (!isAdmin()) {
            showError(
                "Administrator access is required."
            );
            return;
        }

        const id =
            $("meter-lookup-id")
                ?.value
                .trim();

        if (!id) {
            showError(
                "Meter ID is required."
            );
            return;
        }

        await lookupMeterById(
            id
        );
    }

    async function lookupMeterById(
        id
    ) {
        const result =
            await apiRequest(
                `${ENDPOINTS.meters}/${encodeURIComponent(
                    id
                )}`,
                {
                    method: "GET"
                }
            );

        const meter =
            responseData(result);

        state.currentMeter =
            meter;

        renderMeterDetail(
            meter
        );
    }

    function renderMeterDetail(
        meter
    ) {
        if (!meter) {
            return;
        }

        show(
            $("meter-detail-card")
        );

        setText(
            "meter-detail-title",
            meterNumber(meter)
        );

        setText(
            "meter-detail-number",
            meterNumber(meter)
        );

        setText(
            "meter-detail-plan",
            planName(meter)
        );

        /*
         * Deliberately do NOT render meter.id.
         */
    }

    async function createMeter(
        event
    ) {
        event.preventDefault();

        if (!isAdmin()) {
            showError(
                "Administrator access is required."
            );
            return;
        }

        const form =
            event.currentTarget;

        const button =
            form.querySelector(
                'button[type="submit"]'
            );

        const meterNumberValue =
            $("create-meter-number")
                ?.value
                .trim();

        const billingPlanIdValue =
            $("create-meter-plan")
                ?.value;

        if (
            !meterNumberValue ||
            !billingPlanIdValue
        ) {
            showError(
                "Meter number and billing plan are required."
            );
            return;
        }

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
                        body:
                            JSON.stringify({
                                meterNumber:
                                    meterNumberValue,
                                billingPlanId:
                                    billingPlanIdValue
                            })
                    }
                );

            showSuccess(
                responseSuccessMessage(
                    result
                )
            );

            form.reset();

            await loadAdminMeters();

        } finally {
            setButtonLoading(
                button,
                false
            );
        }
    }

    function editCurrentMeter() {
        if (!state.currentMeter) {
            showInfo(
                "Look up a meter first."
            );
            return;
        }

        editMeterById(
            meterId(
                state.currentMeter
            )
        );
    }

    async function editMeterById(
        id
    ) {
        if (!id) {
            showError(
                "The backend did not return the meter identifier."
            );
            return;
        }

        const meter =
            state.adminMeters.find(
                (item) =>
                    String(
                        meterId(item)
                    ) === String(id)
            );

        if (meter) {
            state.currentMeter =
                meter;

            $("edit-meter-id").value =
                id;

            $("edit-meter-number").value =
                meterNumber(meter);

            $("edit-meter-plan").value =
                billingPlanId(meter) ||
                "";
        } else {
            try {
                await lookupMeterById(
                    id
                );

                const current =
                    state.currentMeter;

                if (!current) {
                    return;
                }

                $("edit-meter-id").value =
                    id;

                $("edit-meter-number").value =
                    meterNumber(current);

                $("edit-meter-plan").value =
                    billingPlanId(current) ||
                    "";
            } catch {
                return;
            }
        }

        await loadAdminPlansIntoSelects();

        show(
            $("admin-meter-edit-panel")
        );
    }

    async function updateMeter(
        event
    ) {
        event.preventDefault();

        const id =
            $("edit-meter-id")
                ?.value
                .trim();

        const meterNumberValue =
            $("edit-meter-number")
                ?.value
                .trim();

        const billingPlanIdValue =
            $("edit-meter-plan")
                ?.value;

        if (
            !id ||
            !meterNumberValue ||
            !billingPlanIdValue
        ) {
            showError(
                "Meter number and billing plan are required."
            );
            return;
        }

        const button =
            event.currentTarget
                .querySelector(
                    'button[type="submit"]'
                );

        setButtonLoading(
            button,
            true,
            "Updating..."
        );

        try {
            const result =
                await apiRequest(
                    `${ENDPOINTS.meters}/${encodeURIComponent(
                        id
                    )}`,
                    {
                        method: "PUT",
                        body:
                            JSON.stringify({
                                meterNumber:
                                    meterNumberValue,
                                billingPlanId:
                                    billingPlanIdValue
                            })
                    }
                );

            showSuccess(
                responseSuccessMessage(
                    result
                )
            );

            hide(
                $("admin-meter-edit-panel")
            );

            await loadAdminMeters();

        } finally {
            setButtonLoading(
                button,
                false
            );
        }
    }

    async function deleteMeterById(
        id
    ) {
        if (!id) {
            return;
        }

        await openConfirmModal(
            "Delete water meter",
            "This will deactivate the selected water meter.",
            async () => {
                const result =
                    await apiRequest(
                        `${ENDPOINTS.meters}/${encodeURIComponent(
                            id
                        )}`,
                        {
                            method: "DELETE"
                        }
                    );

                showSuccess(
                    responseSuccessMessage(
                        result
                    )
                );

                state.currentMeter =
                    null;

                hide(
                    $("meter-detail-card")
                );

                hide(
                    $("admin-meter-edit-panel")
                );

                await loadAdminMeters();
            }
        );
    }


    /* =========================================================
       ADMIN BILLING PLANS
       ========================================================= */

    async function loadAdminPlans() {
        if (!isAdmin()) {
            return [];
        }

        const result =
            await apiRequest(
                ENDPOINTS.plans,
                {
                    method: "GET"
                }
            );

        state.plans =
            arrayFrom(
                responseData(result)
            );

        renderAdminPlans(
            state.plans
        );

        setText(
            "admin-plan-count",
            state.plans.length
        );

        populatePlanSelects(
            state.plans
        );

        return state.plans;
    }

    async function loadAdminPlansIntoSelects() {
        if (!state.plans.length) {
            await loadAdminPlans();
        } else {
            populatePlanSelects(
                state.plans
            );
        }
    }

    function populatePlanSelects(
        plans
    ) {
        const selects = [
            $("create-meter-plan"),
            $("edit-meter-plan")
        ];

        selects.forEach(
            (select) => {
                if (!select) {
                    return;
                }

                const current =
                    select.value;

                select.innerHTML = `
                    <option value="">
                        Select billing plan
                    </option>
                `;

                plans.forEach(
                    (plan) => {
                        const id =
                            planId(plan);

                        if (!id) {
                            return;
                        }

                        const option =
                            document.createElement(
                                "option"
                            );

                        option.value =
                            String(id);

                        option.textContent =
                            firstDefined(
                                plan?.name,
                                plan?.code,
                                "Billing Plan"
                            );

                        select.appendChild(
                            option
                        );
                    }
                );

                if (current) {
                    select.value =
                        current;
                }
            }
        );
    }

    function renderAdminPlans(
        plans
    ) {
        const body =
            $("admin-plans-table");

        const overview =
            $("admin-overview-plans");

        if (body) {
            body.innerHTML =
                plans.length
                    ? plans
                        .map(
                            (plan) => `
                                <tr>
                                    <td>
                                        <strong>
                                            ${escapeHtml(
                                                planNameFromPlan(
                                                    plan
                                                )
                                            )}
                                        </strong>
                                    </td>

                                    <td>
                                        ${escapeHtml(
                                            firstDefined(
                                                plan?.code,
                                                "—"
                                            )
                                        )}
                                    </td>

                                    <td>
                                        ${escapeHtml(
                                            firstDefined(
                                                plan?.type,
                                                plan?.planType,
                                                "—"
                                            )
                                        )}
                                    </td>

                                    <td>
                                        ${escapeHtml(
                                            planPriceDisplay(
                                                plan
                                            )
                                        )}
                                    </td>

                                    <td>
                                        ${escapeHtml(
                                            slabCount(
                                                plan
                                            )
                                        )}
                                    </td>

                                    <td>
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
                                    </td>
                                </tr>
                            `
                        )
                        .join("")
                    : emptyRow(
                        6,
                        "No billing plans found."
                    );
        }

        if (overview) {
            overview.innerHTML =
                plans.length
                    ? plans
                        .slice(0, 5)
                        .map(
                            (plan) => `
                                <tr>
                                    <td>
                                        <strong>
                                            ${escapeHtml(
                                                planNameFromPlan(
                                                    plan
                                                )
                                            )}
                                        </strong>
                                    </td>

                                    <td>
                                        ${escapeHtml(
                                            firstDefined(
                                                plan?.code,
                                                "—"
                                            )
                                        )}
                                    </td>

                                    <td>
                                        ${escapeHtml(
                                            firstDefined(
                                                plan?.type,
                                                plan?.planType,
                                                "—"
                                            )
                                        )}
                                    </td>
                                </tr>
                            `
                        )
                        .join("")
                    : emptyRow(
                        3,
                        "No billing plans found."
                    );
        }
    }

    function planNameFromPlan(
        plan
    ) {
        return firstDefined(
            plan?.name,
            plan?.planName,
            plan?.code,
            "Billing Plan"
        );
    }

    function planPriceDisplay(
        plan
    ) {
        const price =
            firstDefined(
                plan?.price,
                plan?.fixedPrice,
                plan?.pricePerUnit,
                plan?.amount
            );

        if (
            price !== undefined &&
            price !== null &&
            price !== ""
        ) {
            return formatCurrency(
                price
            );
        }

        return "Slab";
    }

    function slabCount(
        plan
    ) {
        const slabs =
            firstDefined(
                plan?.slabs,
                plan?.billingSlabs
            );

        return Array.isArray(
            slabs
        )
            ? slabs.length
            : 0;
    }

    async function createPlan(
        event
    ) {
        event.preventDefault();

        if (!isAdmin()) {
            showError(
                "Administrator access is required."
            );
            return;
        }

        const form =
            event.currentTarget;

        const button =
            form.querySelector(
                'button[type="submit"]'
            );

        const name =
            $("plan-name")
                ?.value
                .trim();

        const code =
            $("plan-code")
                ?.value
                .trim();

        const description =
            $("plan-description")
                ?.value
                .trim();

        const type =
            $("plan-type")
                ?.value;

        if (
            !name ||
            !code ||
            !type
        ) {
            showError(
                "Plan name, code and type are required."
            );
            return;
        }

        const payload = {
            name,
            code,
            description,
            type
        };

        if (type === "FIXED") {
            const price =
                Number(
                    $("plan-price")
                        ?.value
                );

            if (
                Number.isNaN(price) ||
                price < 0
            ) {
                showError(
                    "A valid fixed price is required."
                );
                return;
            }

            payload.price =
                price;
        } else {
            payload.slabs =
                collectSlabs();
        }

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
                        body:
                            JSON.stringify(
                                payload
                            )
                    }
                );

            showSuccess(
                responseSuccessMessage(
                    result
                )
            );

            form.reset();

            const rows =
                $("slab-rows");

            if (rows) {
                rows.innerHTML =
                    "";
            }

            updatePlanTypeUI();

            await loadAdminPlans();

        } finally {
            setButtonLoading(
                button,
                false
            );
        }
    }

    function editPlanById(
        id
    ) {
        const plan =
            state.plans.find(
                (item) =>
                    String(
                        planId(item)
                    ) === String(id)
            );

        if (!plan) {
            showError(
                "Billing plan could not be found."
            );
            return;
        }

        state.currentPlan =
            plan;

        $("edit-plan-id").value =
            planId(plan);

        $("edit-plan-name").value =
            firstDefined(
                plan?.name,
                plan?.planName,
                ""
            );

        $("edit-plan-type").value =
            firstDefined(
                plan?.type,
                plan?.planType,
                "FIXED"
            );

        $("edit-plan-description").value =
            firstDefined(
                plan?.description,
                ""
            );

        $("edit-plan-price").value =
            firstDefined(
                plan?.price,
                plan?.fixedPrice,
                plan?.pricePerUnit,
                ""
            );

        const rows =
            $("edit-slab-rows");

        if (rows) {
            rows.innerHTML =
                "";

            const slabs =
                firstDefined(
                    plan?.slabs,
                    plan?.billingSlabs
                );

            if (Array.isArray(slabs)) {
                slabs.forEach(
                    (slab) => {
                        addSlabRow(
                            "edit",
                            slab
                        );
                    }
                );
            }
        }

        updatePlanTypeUI(
            "edit"
        );

        show(
            $("admin-plan-edit-panel")
        );
    }

    async function updatePlan(
        event
    ) {
        event.preventDefault();

        const id =
            $("edit-plan-id")
                ?.value
                .trim();

        if (!id) {
            showError(
                "Billing plan identifier is missing."
            );
            return;
        }

        const name =
            $("edit-plan-name")
                ?.value
                .trim();

        const description =
            $("edit-plan-description")
                ?.value
                .trim();

        const type =
            $("edit-plan-type")
                ?.value;

        if (
            !name ||
            !type
        ) {
            showError(
                "Plan name and type are required."
            );
            return;
        }

        const payload = {
            name,
            description,
            type
        };

        if (type === "FIXED") {
            const price =
                Number(
                    $("edit-plan-price")
                        ?.value
                );

            if (
                Number.isNaN(price) ||
                price < 0
            ) {
                showError(
                    "A valid fixed price is required."
                );
                return;
            }

            payload.price =
                price;
        } else {
            payload.slabs =
                collectSlabs(
                    "edit"
                );
        }

        const button =
            event.currentTarget
                .querySelector(
                    'button[type="submit"]'
                );

        setButtonLoading(
            button,
            true,
            "Updating..."
        );

        try {
            const result =
                await apiRequest(
                    `${ENDPOINTS.plans}/${encodeURIComponent(
                        id
                    )}`,
                    {
                        method: "PUT",
                        body:
                            JSON.stringify(
                                payload
                            )
                    }
                );

            showSuccess(
                responseSuccessMessage(
                    result
                )
            );

            hide(
                $("admin-plan-edit-panel")
            );

            await loadAdminPlans();

        } finally {
            setButtonLoading(
                button,
                false
            );
        }
    }

    async function deletePlanById(
        id
    ) {
        if (!id) {
            return;
        }

        await openConfirmModal(
            "Delete billing plan",
            "This will deactivate the selected billing plan.",
            async () => {
                const result =
                    await apiRequest(
                        `${ENDPOINTS.plans}/${encodeURIComponent(
                            id
                        )}`,
                        {
                            method: "DELETE"
                        }
                    );

                showSuccess(
                    responseSuccessMessage(
                        result
                    )
                );

                await loadAdminPlans();
            }
        );
    }


    /* =========================================================
       SLAB UI
       ========================================================= */

    function updatePlanTypeUI(
        mode = ""
    ) {
        const suffix =
            mode === "edit"
                ? "edit-"
                : "";

        const typeId =
            `${suffix}plan-type`;

        const type =
            $(typeId)?.value;

        const priceField =
            $(
                `${suffix}fixed-price-field`
            );

        const slabBuilder =
            $(
                `${suffix}slab-builder`
            );

        if (type === "SLAB") {
            hide(priceField);
            show(slabBuilder);
        } else {
            show(priceField);
            hide(slabBuilder);
        }
    }

    function addSlabRow(
        mode = "",
        slab = null
    ) {
        const container =
            mode === "edit"
                ? $("edit-slab-rows")
                : $("slab-rows");

        if (!container) {
            return;
        }

        const row =
            document.createElement(
                "div"
            );

        row.className =
            "slab-row";

        row.innerHTML = `
            <div class="field">
                <label>
                    From
                </label>

                <input
                    type="number"
                    min="0"
                    step="0.01"
                    data-slab-from
                    value="${escapeHtml(
                        firstDefined(
                            slab?.from,
                            slab?.minUnits,
                            ""
                        )
                    )}"
                    required>
            </div>

            <div class="field">
                <label>
                    To
                </label>

                <input
                    type="number"
                    min="0"
                    step="0.01"
                    data-slab-to
                    value="${escapeHtml(
                        firstDefined(
                            slab?.to,
                            slab?.maxUnits,
                            ""
                        )
                    )}">
            </div>

            <div class="field">
                <label>
                    Rate
                </label>

                <input
                    type="number"
                    min="0"
                    step="0.01"
                    data-slab-rate
                    value="${escapeHtml(
                        firstDefined(
                            slab?.rate,
                            slab?.price,
                            slab?.pricePerUnit,
                            ""
                        )
                    )}"
                    required>
            </div>

            <button
                type="button"
                class="btn btn-danger btn-sm slab-remove"
                data-action="remove-slab">
                Remove
            </button>
        `;

        container.appendChild(
            row
        );
    }

    function collectSlabs(
        mode = ""
    ) {
        const container =
            mode === "edit"
                ? $("edit-slab-rows")
                : $("slab-rows");

        if (!container) {
            return [];
        }

        return $$(`
            #${container.id} .slab-row
        `).map(
            (row) => {
                const from =
                    Number(
                        row.querySelector(
                            "[data-slab-from]"
                        )?.value
                    );

                const toValue =
                    row.querySelector(
                        "[data-slab-to]"
                    )?.value;

                const rate =
                    Number(
                        row.querySelector(
                            "[data-slab-rate]"
                        )?.value
                    );

                return {
                    from,
                    to:
                        toValue === ""
                            ? null
                            : Number(
                                toValue
                            ),
                    rate
                };
            }
        );
    }


    /* =========================================================
       ADMIN ASSIGNMENTS
       ========================================================= */

    async function assignMeter(
        event
    ) {
        event.preventDefault();

        if (!isAdmin()) {
            showError(
                "Administrator access is required."
            );
            return;
        }

        const meterIdValue =
            $("assignment-meter-id")
                ?.value;

        const userIdValue =
            $("assignment-user-id")
                ?.value;

        if (
            !meterIdValue ||
            !userIdValue
        ) {
            showError(
                "Water meter and customer are required."
            );
            return;
        }

        const button =
            event.currentTarget
                .querySelector(
                    'button[type="submit"]'
                );

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
                        body:
                            JSON.stringify({
                                waterMeterId:
                                    meterIdValue,
                                userId:
                                    userIdValue
                            })
                    }
                );

            showSuccess(
                responseSuccessMessage(
                    result
                )
            );

            event.currentTarget.reset();

            await Promise.all([
                loadAdminMeters(),
                loadAdminCustomers()
            ]);

        } finally {
            setButtonLoading(
                button,
                false
            );
        }
    }

    async function unassignMeter(
        event
    ) {
        event.preventDefault();

        if (!isAdmin()) {
            showError(
                "Administrator access is required."
            );
            return;
        }

        const meterIdValue =
            $("unassignment-meter-id")
                ?.value;

        const userIdValue =
            $("unassignment-user-id")
                ?.value;

        if (
            !meterIdValue ||
            !userIdValue
        ) {
            showError(
                "Water meter and customer are required."
            );
            return;
        }

        const button =
            event.currentTarget
                .querySelector(
                    'button[type="submit"]'
                );

        setButtonLoading(
            button,
            true,
            "Removing..."
        );

        try {
            const result =
                await apiRequest(
                    ENDPOINTS.assignments,
                    {
                        method: "DELETE",
                        body:
                            JSON.stringify({
                                waterMeterId:
                                    meterIdValue,
                                userId:
                                    userIdValue
                            })
                    }
                );

            showSuccess(
                responseSuccessMessage(
                    result
                )
            );

            event.currentTarget.reset();

            await loadAdminMeters();

        } finally {
            setButtonLoading(
                button,
                false
            );
        }
    }


    /* =========================================================
       ADMIN INVOICES
       ========================================================= */

    async function loadAdminInvoices() {
        if (!isAdmin()) {
            return [];
        }

        const result =
            await apiRequest(
                ENDPOINTS.invoices,
                {
                    method: "GET"
                }
            );

        state.invoices =
            arrayFrom(
                responseData(result)
            );

        renderAdminInvoices(
            state.invoices
        );

        setText(
            "admin-invoice-count",
            state.invoices.length
        );

        return state.invoices;
    }

    function renderAdminInvoices(
        invoices
    ) {
        const overview =
            $("admin-overview-invoices");

        const body =
            $("admin-invoices-table");

        const sorted =
            [...invoices].sort(
                compareInvoicesDescending
            );

        const rows =
            sorted.length
                ? sorted
                    .map(
                        (invoice) => `
                            <tr>
                                <td>
                                    <strong>
                                        Invoice
                                    </strong>
                                </td>

                                <td>
                                    ${escapeHtml(
                                        customerName(
                                            invoice
                                        )
                                    )}
                                </td>

                                <td>
                                    ${escapeHtml(
                                        invoiceMeter(
                                            invoice
                                        )
                                    )}
                                </td>

                                <td>
                                    ${escapeHtml(
                                        invoicePeriod(
                                            invoice
                                        )
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
                        `
                    )
                    .join("")
                : emptyRow(
                    6,
                    "No invoices found."
                );

        if (body) {
            body.innerHTML =
                rows;
        }

        if (overview) {
            overview.innerHTML =
                sorted.length
                    ? sorted
                        .slice(0, 5)
                        .map(
                            (invoice) => `
                                <tr>
                                    <td>
                                        <strong>
                                            Invoice
                                        </strong>
                                    </td>

                                    <td>
                                        ${escapeHtml(
                                            customerName(
                                                invoice
                                            )
                                        )}
                                    </td>

                                    <td>
                                        ${escapeHtml(
                                            invoiceMeter(
                                                invoice
                                            )
                                        )}
                                    </td>

                                    <td>
                                        ${escapeHtml(
                                            invoicePeriod(
                                                invoice
                                            )
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
                            `
                        )
                        .join("")
                    : emptyRow(
                        6,
                        "No invoices found."
                    );
        }
    }

    function viewInvoice(
        id
    ) {
        if (!id) {
            return;
        }

        const invoice =
            state.invoices.find(
                (item) =>
                    String(
                        invoiceId(item)
                    ) === String(id)
            );

        if (!invoice) {
            showInfo(
                "Invoice details are not available."
            );
            return;
        }

        /*
         * Invoice UUID is deliberately not shown.
         */
        openInfoModal(
            "Invoice",
            `
                <div class="detail-list">
                    <div>
                        <span>Customer</span>
                        <strong>
                            ${escapeHtml(
                                customerName(
                                    invoice
                                )
                            )}
                        </strong>
                    </div>

                    <div>
                        <span>Meter</span>
                        <strong>
                            ${escapeHtml(
                                invoiceMeter(
                                    invoice
                                )
                            )}
                        </strong>
                    </div>

                    <div>
                        <span>Period</span>
                        <strong>
                            ${escapeHtml(
                                invoicePeriod(
                                    invoice
                                )
                            )}
                        </strong>
                    </div>

                    <div>
                        <span>Amount</span>
                        <strong>
                            ${escapeHtml(
                                formatCurrency(
                                    invoiceAmount(
                                        invoice
                                    )
                                )
                            )}
                        </strong>
                    </div>

                    <div>
                        <span>Status</span>
                        <strong>
                            ${escapeHtml(
                                invoiceStatus(
                                    invoice
                                )
                            )}
                        </strong>
                    </div>
                </div>
            `
        );
    }


    /* =========================================================
       ADMIN BILLING JOBS
       ========================================================= */

    async function startAdminBillingJob(
        event
    ) {
        event.preventDefault();

        if (!isAdmin()) {
            showError(
                "Administrator access is required."
            );
            return;
        }

        const form =
            event.currentTarget;

        const button =
            form.querySelector(
                'button[type="submit"]'
            );

        const year =
            Number(
                $("admin-billing-year")
                    ?.value
            );

        const month =
            Number(
                $("admin-billing-month")
                    ?.value
            );

        if (
            !year ||
            !month
        ) {
            showError(
                "Billing year and month are required."
            );
            return;
        }

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
                        body:
                            JSON.stringify({
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
                    data?.generationJobId,
                    data?.id
                );

            showSuccess(
                responseSuccessMessage(
                    result
                )
            );

            /*
             * Job ID exists only in JS memory.
             * It is never placed into the DOM.
             */
            if (jobId) {
                state.lastJobId =
                    String(jobId);

                await checkJobStatus(
                    state.lastJobId
                );

                startJobPolling(
                    state.lastJobId
                );
            }

            await loadAdminInvoices();

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
        if (!isAdmin()) {
            showError(
                "Administrator access is required."
            );
            return null;
        }

        const id =
            jobId ||
            state.lastJobId;

        if (!id) {
            showInfo(
                "No billing job has been started in this session."
            );

            return null;
        }

        try {
            const result =
                await apiRequest(
                    `/api/admin/invoices/generation-jobs/${encodeURIComponent(
                        id
                    )}`,
                    {
                        method: "GET"
                    }
                );

            const data =
                responseData(result);

            renderJobStatus(
                data
            );

            return data;

        } catch {
            return null;
        }
    }

    function startJobPolling(
        jobId
    ) {
        if (
            state.jobPollingTimer
        ) {
            clearInterval(
                state.jobPollingTimer
            );
        }

        state.jobPollingTimer =
            setInterval(
                async () => {
                    const data =
                        await checkJobStatus(
                            jobId
                        );

                    if (
                        isTerminalJobStatus(
                            data
                        )
                    ) {
                        clearInterval(
                            state.jobPollingTimer
                        );

                        state.jobPollingTimer =
                            null;

                        await loadAdminInvoices();
                    }
                },
                5000
            );
    }

    function isTerminalJobStatus(
        data
    ) {
        const status =
            String(
                firstDefined(
                    data?.status,
                    data?.state,
                    data?.jobStatus,
                    ""
                )
            ).toUpperCase();

        return [
            "COMPLETED",
            "SUCCESS",
            "FAILED",
            "ERROR",
            "CANCELLED",
            "CANCELED"
        ].includes(status);
    }

    function renderJobStatus(
        data
    ) {
        show(
            $("job-status-card")
        );

        hide(
            $("job-status-empty")
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

        /*
         * Backend response is shown after
         * recursively removing identifier fields.
         *
         * This keeps useful backend information
         * while guaranteeing UUIDs are not rendered.
         */
        const displayData =
            sanitizeJobStatus(
                data
            );

        const json =
            $("job-status-json");

        if (json) {
            json.textContent =
                safeJson(
                    displayData
                );
        }
    }

    function sanitizeJobStatus(
        value
    ) {
        if (
            Array.isArray(value)
        ) {
            return value.map(
                sanitizeJobStatus
            );
        }

        if (
            !value ||
            typeof value !== "object"
        ) {
            return value;
        }

        const result = {};

        Object.entries(value)
            .forEach(
                ([key, entry]) => {
                    /*
                     * Do not render backend IDs.
                     */
                    if (
                        /^(id|.*Id|.*ID)$/i.test(
                            key
                        )
                    ) {
                        return;
                    }

                    result[key] =
                        sanitizeJobStatus(
                            entry
                        );
                }
            );

        return result;
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

    async function loadHealth() {
        const result =
            await apiRequest(
                ENDPOINTS.health,
                {
                    method: "GET"
                }
            );

        const data =
            responseData(result);

        setText(
            "monitor-health-status",
            firstDefined(
                data?.status,
                "UP"
            )
        );

        const dot =
            $("monitor-health-dot");

        if (dot) {
            dot.classList.toggle(
                "healthy",
                String(
                    firstDefined(
                        data?.status,
                        ""
                    )
                ).toUpperCase() ===
                    "UP"
            );
        }

        const json =
            $("monitor-health-json");

        if (json) {
            json.textContent =
                safeJson(data);
        }

        setText(
            "admin-health-status",
            firstDefined(
                data?.status,
                "—"
            )
        );

        return data;
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

        const json =
            $("monitor-info-json");

        if (json) {
            json.textContent =
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

        const json =
            $("monitor-metrics-json");

        if (json) {
            json.textContent =
                safeJson(data);
        }

        return data;
    }


    /* =========================================================
       MODAL
       ========================================================= */

    let modalConfirmHandler =
        null;

    function openConfirmModal(
        title,
        message,
        onConfirm
    ) {
        setText(
            "modal-eyebrow",
            "Confirmation"
        );

        setText(
            "modal-title",
            title
        );

        const body =
            $("modal-body");

        if (body) {
            body.textContent =
                message;
        }

        show(
            $("modal-cancel")
        );

        setText(
            "modal-confirm",
            "Confirm"
        );

        modalConfirmHandler =
            onConfirm;

        show(
            $("modal-backdrop")
        );
    }

    function openInfoModal(
        title,
        html
    ) {
        setText(
            "modal-eyebrow",
            "Details"
        );

        setText(
            "modal-title",
            title
        );

        const body =
            $("modal-body");

        if (body) {
            body.innerHTML =
                html;
        }

        hide(
            $("modal-confirm")
        );

        hide(
            $("modal-cancel")
        );

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

        show(
            $("modal-confirm")
        );

        show(
            $("modal-cancel")
        );
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
        } catch {
            /*
             * Backend error already shown.
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
       BILLING PERIOD
       ========================================================= */

    function initialiseBillingPeriod(
        yearId,
        monthId
    ) {
        const now =
            new Date();

        /*
         * Previous calendar month.
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


    /* =========================================================
       ENTITY HELPERS
       ========================================================= */

    function meterId(
        meter
    ) {
        return firstDefined(
            meter?.id,
            meter?.waterMeterId
        );
    }

    function meterNumber(
        meter
    ) {
        return firstDefined(
            meter?.meterNumber,
            meter?.number,
            meter?.meterNo,
            "Meter"
        );
    }

    function billingPlanId(
        meter
    ) {
        return firstDefined(
            meter?.billingPlanId,
            meter?.planId,
            meter?.billingPlan?.id,
            meter?.billingPlan?.billingPlanId
        );
    }

    function planName(
        meter
    ) {
        return firstDefined(
            meter?.billingPlan?.name,
            meter?.plan?.name,
            meter?.billingPlanName,
            "—"
        );
    }

    function planId(
        plan
    ) {
        return firstDefined(
            plan?.id,
            plan?.billingPlanId
        );
    }

    function invoiceId(
        invoice
    ) {
        return firstDefined(
            invoice?.id,
            invoice?.invoiceId
        );
    }

    function invoiceAmount(
        invoice
    ) {
        return firstDefined(
            invoice?.totalAmount,
            invoice?.amount,
            invoice?.invoiceAmount,
            invoice?.grandTotal,
            invoice?.total,
            0
        );
    }

    function invoiceStatus(
        invoice
    ) {
        return firstDefined(
            invoice?.status,
            invoice?.invoiceStatus,
            "—"
        );
    }

    function customerName(
        invoice
    ) {
        return firstDefined(
            invoice?.customer?.username,
            invoice?.customerUsername,
            invoice?.username,
            invoice?.user?.username,
            "—"
        );
    }

    function invoiceMeter(
        invoice
    ) {
        return firstDefined(
            invoice?.waterMeter?.meterNumber,
            invoice?.meterNumber,
            invoice?.waterMeterNumber,
            "—"
        );
    }

    function invoicePeriod(
        invoice
    ) {
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
            return `${year}-${String(
                month
            ).padStart(2, "0")}`;
        }

        return firstDefined(
            invoice?.billingPeriod,
            invoice?.period,
            "—"
        );
    }


    /* =========================================================
       FORMATTERS
       ========================================================= */

    function formatCurrency(
        value
    ) {
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

        if (
            aYear !== bYear
        ) {
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

    function emptyRow(
        colspan,
        message
    ) {
        return `
            <tr>
                <td
                    colspan="${colspan}"
                    class="empty-state">
                    ${escapeHtml(
                        message
                    )}
                </td>
            </tr>
        `;
    }


    /* =========================================================
       ACTION DELEGATION
       ========================================================= */

    async function handleActionClick(
        event
    ) {
        const target =
            event.target.closest(
                "[data-action]"
            );

        if (!target) {
            return;
        }

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

            case "edit-current-meter":
                editCurrentMeter();
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

            case "view-invoice":
                viewInvoice(
                    target.dataset.invoiceId
                );
                break;
        }
    }


    /* =========================================================
       SECTION TARGETS
       ========================================================= */

    function handleSectionTargetClick(
        event
    ) {
        const target =
            event.target.closest(
                "[data-section-target]"
            );

        if (!target) {
            return;
        }

        const section =
            target.dataset.sectionTarget;

        if (!section) {
            return;
        }

        navigateTo(
            section
        );
    }

    function handleNavClick(
        event
    ) {
        const button =
            event.target.closest(
                ".nav-item[data-section]"
            );

        if (!button) {
            return;
        }

        navigateTo(
            button.dataset.section
        );
    }


    /* =========================================================
       FORM RESET
       ========================================================= */

    function handleFormReset(
        event
    ) {
        const form =
            event.target;

        setTimeout(() => {
            if (
                form.id ===
                "admin-create-plan-form"
            ) {
                const rows =
                    $("slab-rows");

                if (rows) {
                    rows.innerHTML =
                        "";
                }

                updatePlanTypeUI();
            }
        }, 0);
    }


    /* =========================================================
       EVENTS
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


        /* Sidebar */

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


        /* Refresh */

        $("refresh-current")
            ?.addEventListener(
                "click",
                async () => {
                    if (
                        state.currentSection
                    ) {
                        await loadSectionData(
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
                () =>
                    updatePlanTypeUI()
            );

        $("edit-plan-type")
            ?.addEventListener(
                "change",
                () =>
                    updatePlanTypeUI(
                        "edit"
                    )
            );


        /* Billing */

        $("admin-generate-form")
            ?.addEventListener(
                "submit",
                startAdminBillingJob
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


        /* Form reset */

        document.addEventListener(
            "reset",
            handleFormReset
        );


        /* Escape */

        document.addEventListener(
            "keydown",
            (event) => {
                if (
                    event.key ===
                    "Escape"
                ) {
                    closeModal();
                    closeMobileSidebar();
                }
            }
        );
    }


    /* =========================================================
       INITIALIZATION
       ========================================================= */

    function initialise() {
        bindEvents();

        /*
         * There is intentionally NO loadSession().
         *
         * state starts empty on every page load.
         */
        initialiseBillingPeriod(
            "customer-billing-year",
            "customer-billing-month"
        );

        initialiseBillingPeriod(
            "admin-billing-year",
            "admin-billing-month"
        );

        updatePlanTypeUI();

        updatePlanTypeUI(
            "edit"
        );

        showLoginView();
    }


    /* =========================================================
       START
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
