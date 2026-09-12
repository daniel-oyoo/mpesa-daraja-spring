/**
 * M-Pesa STK Push — Frontend Demo
 * Talks to:
 *   POST /api/v1/mpesa/stk-push
 *   GET  /api/v1/mpesa/stk-push/status/{checkoutId}
 */

const API_BASE = "/api/v1/mpesa";
const POLL_INTERVAL_MS = 3000;
const MAX_POLLS = 40; // 2 minutes max

const form = document.getElementById("paymentForm");
const payBtn = document.getElementById("payBtn");
const statusCard = document.getElementById("statusCard");
const errorCard = document.getElementById("errorCard");
const statusTitle = document.getElementById("statusTitle");
const statusValue = document.getElementById("statusValue");
const statusMessage = document.getElementById("statusMessage");
const checkoutIdEl = document.getElementById("checkoutId");
const receiptLine = document.getElementById("receiptLine");
const receiptNumber = document.getElementById("receiptNumber");
const resultBanner = document.getElementById("resultBanner");
const errorMessage = document.getElementById("errorMessage");
const pulse = document.querySelector(".pulse");

let pollTimer = null;
let pollCount = 0;

form.addEventListener("submit", async(e) => {
    e.preventDefault();
    resetUI();

    const payload = {
        phoneNumber: form.phoneNumber.value.trim(),
        amount: form.amount.value.trim(),
        accountReference: form.accountReference.value.trim(),
        description: form.description.value.trim(),
    };

    setLoading(true);

    try {
        const res = await fetch(`${API_BASE}/stk-push`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload),
        });

        const data = await res.json();

        if (!res.ok) {
            throw new Error(data.message || `Request failed (${res.status})`);
        }

        showStatus(data);
        startPolling(data.CheckoutRequestID);
    } catch (err) {
        showError(err.message);
    } finally {
        setLoading(false);
    }
});

function resetUI() {
    stopPolling();
    errorCard.classList.add("hidden");
    statusCard.classList.add("hidden");
    resultBanner.classList.remove("show", "success", "failed");
    receiptLine.classList.add("hidden");
    receiptNumber.textContent = "—";
    pollCount = 0;
}

function setLoading(isLoading) {
    payBtn.disabled = isLoading;
    payBtn.textContent = isLoading ? "Sending…" : "Pay Now";
}

function showStatus(data) {
    statusCard.classList.remove("hidden");
    statusTitle.textContent = "Awaiting payment…";
    statusMessage.textContent =
        data.CustomerMessage || "Please check your phone and enter PIN.";
    checkoutIdEl.textContent = data.CheckoutRequestID || "—";
    setBadge("PENDING");
}

function showError(message) {
    errorCard.classList.remove("hidden");
    errorMessage.textContent = message;
    statusCard.classList.add("hidden");
}

function setBadge(status) {
    statusValue.textContent = status;
    statusValue.className = "badge " + status.toLowerCase();
}

function startPolling(checkoutId) {
    stopPolling();
    pollTimer = setInterval(() => pollStatus(checkoutId), POLL_INTERVAL_MS);
}

function stopPolling() {
    if (pollTimer) {
        clearInterval(pollTimer);
        pollTimer = null;
    }
}

async function pollStatus(checkoutId) {
    pollCount++;

    if (pollCount > MAX_POLLS) {
        stopPolling();
        statusTitle.textContent = "Timed out";
        statusMessage.textContent =
            "No callback received after 2 minutes. Check the logs or try again.";
        setBadge("FAILED");
        pulse.className = "pulse failed";
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/stk-push/status/${checkoutId}`);
        const data = await res.json();

        if (!res.ok) {
            // 404 = not yet saved, keep polling
            if (res.status === 404) return;
            throw new Error(data.message || "Status check failed");
        }

        const status = (data.status || "").toUpperCase();
        statusValue.textContent = status;
        setBadge(status);

        if (status === "SUCCESS") {
            stopPolling();
            pulse.className = "pulse success";
            statusTitle.textContent = "Payment received 🎉";
            statusMessage.textContent = data.resultDesc || "Transaction completed.";
            if (data.receipt) {
                receiptLine.classList.remove("hidden");
                receiptNumber.textContent = data.receipt;
            }
            showBanner("success", `Payment successful — Receipt: ${data.receipt || "N/A"}`);
        } else if (["FAILED", "CANCELLED", "TIMEOUT"].includes(status)) {
            stopPolling();
            pulse.className = "pulse failed";
            statusTitle.textContent = "Payment " + status.toLowerCase();
            statusMessage.textContent =
                data.resultDesc || "The transaction did not complete.";
            showBanner("failed", `Payment ${status.toLowerCase()} — ${data.resultDesc || ""}`);
        } else {
            statusMessage.textContent = "Waiting for you to enter your PIN…";
        }
    } catch (err) {
        console.error("Poll error:", err);
    }
}

function showBanner(type, message) {
    resultBanner.textContent = message;
    resultBanner.className = `result-banner show ${type}`;
}