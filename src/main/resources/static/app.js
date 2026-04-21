const statusPanel = document.getElementById("status");

const carClientSelect = document.getElementById("car-client-select");
const orderClientSelect = document.getElementById("order-client-select");
const supplierSelect = document.getElementById("supplier-select");
const deliverySupplierSelect = document.getElementById("delivery-supplier-select");
const orderCarSelect = document.getElementById("order-car-select");
const mechanicSelect = document.getElementById("mechanic-select");
const partSelect = document.getElementById("part-select");
const deliveryPartSelect = document.getElementById("delivery-part-select");
const operationSelect = document.getElementById("operation-select");
const consultationRequestedCheckbox = document.getElementById("consultation-requested");
const estimatedDurationLabel = document.getElementById("estimated-duration");
const estimatedPriceLabel = document.getElementById("estimated-price");
const appointmentDateInput = document.querySelector('input[name="appointmentDate"]');
const availabilityTimeInput = document.getElementById("availability-time");
const checkMechanicsButton = document.getElementById("check-mechanics");
const availableMechanicsDiv = document.getElementById("available-mechanics");
const timeSlotsContainer = document.getElementById("time-slots-container");
const timeSlotsDiv = document.getElementById("time-slots");
const scheduledAtHidden = document.getElementById("scheduled-at-hidden");

const ordersList = document.getElementById("orders-list");
const partsList = document.getElementById("parts-list");
const operationDurationMinutes = new Map();
const operationPriceMap = new Map([
    ["WHEEL_CHANGE", 120],
    ["OIL_CHANGE", 80],
    ["BRAKE_INSPECTION", 70],
    ["BRAKE_PADS_REPLACEMENT", 150],
    ["ENGINE_DIAGNOSTIC", 100],
    ["BATTERY_REPLACEMENT", 60],
    ["AC_SERVICE", 120]
]);

const DEFAULT_CONSULTATION_MINUTES = 30;

function log(message, level = "info") {
    const stamp = new Date().toLocaleTimeString();
    const symbol = level === "error" ? "ERR" : "OK ";
    statusPanel.textContent = `[${stamp}] ${symbol} ${message}\n${statusPanel.textContent}`.trim();
}

async function api(path, options = {}) {
    const response = await fetch(path, {
        headers: {"Content-Type": "application/json"},
        ...options
    });

    if (!response.ok) {
        let message = `HTTP ${response.status}`;
        try {
            const body = await response.json();
            if (body.error) {
                message = body.error;
            }
        } catch (_) {
            // no body
        }
        throw new Error(message);
    }

    if (response.status === 204) {
        return null;
    }

    return response.json();
}

function setOptions(selectElement, values, labelBuilder) {
    selectElement.innerHTML = "";
    if (values.length === 0) {
        const option = document.createElement("option");
        option.value = "";
        option.textContent = "No data available";
        selectElement.appendChild(option);
        return;
    }

    values.forEach(item => {
        const option = document.createElement("option");
        option.value = item.id;
        option.textContent = labelBuilder(item);
        selectElement.appendChild(option);
    });
}

function fromForm(form) {
    const formData = new FormData(form);
    return Object.fromEntries(formData.entries());
}

function selectedOperationCodes() {
    const selectedValue = operationSelect.value;
    return selectedValue ? [selectedValue] : [];
}

function estimateDurationMinutes() {
    const selectedOperations = selectedOperationCodes();
    let estimatedMinutes = selectedOperations
        .map(code => operationDurationMinutes.get(code) || 0)
        .reduce((sum, current) => sum + current, 0);

    if (consultationRequestedCheckbox.checked || estimatedMinutes === 0) {
        estimatedMinutes += DEFAULT_CONSULTATION_MINUTES;
    }

    return estimatedMinutes;
}

function refreshEstimatedDuration() {
    const estimatedMinutes = estimateDurationMinutes();
    estimatedDurationLabel.textContent = `Durata estimata: ${estimatedMinutes} minute`;
    refreshEstimatedPrice();
}

function refreshEstimatedPrice() {
    const selectedOperations = selectedOperationCodes();
    let estimatedPrice = 0;
    
    for (const operation of selectedOperations) {
        estimatedPrice += (operationPriceMap.get(operation) || 0);
    }
    
    if (estimatedPrice === 0 || consultationRequestedCheckbox.checked) {
        estimatedPrice += 50;
    }
    
    estimatedPriceLabel.textContent = `Cost estimat: ${estimatedPrice} lei`;
}

async function refreshClients() {
    const clients = await api("/api/clients");
    setOptions(carClientSelect, clients, c => `${c.firstName} ${c.lastName} (${c.email})`);
    setOptions(orderClientSelect, clients, c => `${c.firstName} ${c.lastName} (${c.email})`);
}

async function refreshSuppliers() {
    const suppliers = await api("/api/suppliers");
    setOptions(supplierSelect, suppliers, s => `${s.name}`);
    setOptions(deliverySupplierSelect, suppliers, s => `${s.name}`);
}

async function refreshCars() {
    const cars = await api("/api/cars");
    setOptions(orderCarSelect, cars, car => `${car.brand} ${car.model} [${car.plateNumber}]`);
}

async function refreshMechanics() {
    const mechanics = await api("/api/mechanics");
    setOptions(mechanicSelect, mechanics, m => `${m.firstName} ${m.lastName}`);
}

async function refreshParts() {
    const parts = await api("/api/parts");
    if (partSelect) setOptions(partSelect, parts, p => `${p.name} | stock: ${p.availableStock}`);
    setOptions(deliveryPartSelect, parts, p => `${p.name} | stock: ${p.availableStock}`);

    partsList.innerHTML = "";
    parts.forEach(part => {
        const item = document.createElement("div");
        item.className = "list-item";
        item.innerHTML = `
            <strong>${part.name}</strong>
            <p>Stock: ${part.availableStock}</p>
            <p>Unit Price: ${part.unitPrice}</p>
            <p>Supplier ID: ${part.supplierId}</p>
        `;
        partsList.appendChild(item);
    });
}

async function refreshOperationCatalog() {
    const operations = await api("/api/service-orders/operation-catalog");
    operationDurationMinutes.clear();

    operationSelect.innerHTML = '<option value="">-- Selecteaza serviciul --</option>';
    operations
        .filter(operation => operation.code !== "CONSULTATION")
        .forEach(operation => {
            operationDurationMinutes.set(operation.code, operation.estimatedDurationMinutes);

            const option = document.createElement("option");
            option.value = operation.code;
            option.textContent = `${operation.label} (${operation.estimatedDurationMinutes} min)`;
            operationSelect.appendChild(option);
        });

    refreshEstimatedDuration();
}

async function refreshAvailableMechanics() {
    const selectedDate = appointmentDateInput.value;
    const selectedTime = availabilityTimeInput.value;
    const selectedOperation = operationSelect.value;

    if (!selectedDate || !selectedTime) {
        availableMechanicsDiv.innerHTML = "";
        return;
    }

    const operationParam = selectedOperation ? `&operation=${selectedOperation}` : "";
    const mechanics = await api(`/api/service-orders/available-mechanics?date=${selectedDate}&time=${selectedTime}${operationParam}`);

    availableMechanicsDiv.innerHTML = mechanics.length === 0
        ? "<p class='hint'>Nu exista mecanici disponibili pentru acest interval.</p>"
        : mechanics.map(mechanic => `<div class='list-item'><strong>${mechanic.firstName} ${mechanic.lastName}</strong><p>${mechanic.phone}</p><p>${mechanic.workingStartTime || '08:00'} - ${mechanic.workingEndTime || '17:00'}</p></div>`).join("");
}

async function loadTimeSlots() {
    const mechanicId = mechanicSelect.value;
    const appointmentDate = appointmentDateInput.value;
    const selectedOperation = operationSelect.value;
    
    if (!mechanicId || !appointmentDate) {
        timeSlotsContainer.style.display = "none";
        return;
    }
    
    try {
        const opParam = selectedOperation ? `&operation=${selectedOperation}` : "";
        const slots = await api(`/api/service-orders/available-slots?mechanicId=${mechanicId}&date=${appointmentDate}${opParam}`);
        timeSlotsDiv.innerHTML = "";
        
        slots.forEach(slot => {
            const button = document.createElement("button");
            button.type = "button";
            button.className = `time-slot ${!slot.available ? 'disabled' : ''}`;
            const startTime = new Date(slot.startTime).toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
            const endTime = new Date(slot.endTime).toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
            button.textContent = `${startTime} - ${endTime}`;
            button.title = `Durata: ${slot.duration || 30} min`;
            
            if (!slot.available) {
                button.disabled = true;
            } else {
                button.addEventListener("click", (e) => {
                    e.preventDefault();
                    document.querySelectorAll(".time-slot").forEach(s => s.classList.remove("selected"));
                    button.classList.add("selected");
                    scheduledAtHidden.value = slot.startTime;
                });
            }
            
            timeSlotsDiv.appendChild(button);
        });
        
        timeSlotsContainer.style.display = "block";
    } catch (error) {
        log(`Failed to load time slots: ${error.message}`, "error");
        timeSlotsContainer.style.display = "none";
    }
}

async function refreshOrders() {
    const orders = await api("/api/service-orders?status=IN_PROGRESS");
    const completed = await api("/api/service-orders?status=COMPLETED");
    const allOrders = [...orders, ...completed];

    ordersList.innerHTML = "";
    allOrders.forEach(order => {
        const item = document.createElement("div");
        item.className = "list-item";
        const statusClass = order.status === "COMPLETED" ? "ok" : "warn";
        item.innerHTML = `
            <strong>${order.serviceName}</strong>
            <p>Order ID: ${order.id}</p>
            <p>Client: ${order.clientId || "-"}</p>
            <p>Masina: ${order.carName || "-"}</p>
            <p>Total cost: ${order.totalCost}</p>
            <p>Scheduled: ${order.scheduledAt}</p>
            <p>Estimated duration: ${order.estimatedDurationMinutes || DEFAULT_CONSULTATION_MINUTES} min</p>
            <p>Estimated end: ${order.estimatedEndAt || order.scheduledAt}</p>
            <p>Consultation: ${order.consultationRequested ? "YES" : "NO"}</p>
            <p>Operations: ${(order.selectedOperations || []).join(", ") || "CONSULTATION"}</p>
            <span class="status-inline ${statusClass}">${order.status}</span>
        `;

        if (order.status === "IN_PROGRESS") {
            const completeButton = document.createElement("button");
            completeButton.type = "button";
            completeButton.textContent = "Complete Order";
            completeButton.addEventListener("click", async () => {
                try {
                    await api(`/api/service-orders/${order.id}/complete`, {method: "PATCH"});
                    log(`Order ${order.id} completed`);
                    await refreshOrders();
                } catch (error) {
                    log(error.message, "error");
                }
            });
            item.appendChild(completeButton);
        }

        ordersList.appendChild(item);
    });
}

document.getElementById("client-form").addEventListener("submit", async event => {
    event.preventDefault();
    const body = fromForm(event.target);
    try {
        await api("/api/clients", {method: "POST", body: JSON.stringify(body)});
        event.target.reset();
        log("Client created");
        await refreshClients();
    } catch (error) {
        log(error.message, "error");
    }
});

document.getElementById("car-form").addEventListener("submit", async event => {
    event.preventDefault();
    const body = fromForm(event.target);
    body.fabricationYear = Number(body.fabricationYear);
    try {
        await api("/api/cars", {method: "POST", body: JSON.stringify(body)});
        event.target.reset();
        log("Car created");
        await refreshCars();
    } catch (error) {
        log(error.message, "error");
    }
});

document.getElementById("mechanic-form").addEventListener("submit", async event => {
    event.preventDefault();
    const body = fromForm(event.target);
    try {
        await api("/api/mechanics", {method: "POST", body: JSON.stringify(body)});
        event.target.reset();
        log("Mechanic created");
        await refreshMechanics();
    } catch (error) {
        log(error.message, "error");
    }
});

document.getElementById("supplier-form").addEventListener("submit", async event => {
    event.preventDefault();
    const body = fromForm(event.target);
    try {
        await api("/api/suppliers", {method: "POST", body: JSON.stringify(body)});
        event.target.reset();
        log("Supplier created");
        await refreshSuppliers();
    } catch (error) {
        log(error.message, "error");
    }
});

document.getElementById("part-form").addEventListener("submit", async event => {
    event.preventDefault();
    const body = fromForm(event.target);
    body.availableStock = Number(body.availableStock);
    body.unitPrice = Number(body.unitPrice);
    try {
        await api("/api/parts", {method: "POST", body: JSON.stringify(body)});
        event.target.reset();
        log("Part created");
        await refreshParts();
    } catch (error) {
        log(error.message, "error");
    }
});

document.getElementById("order-form").addEventListener("submit", async event => {
    event.preventDefault();
    
    const clientId = orderClientSelect.value;
    const carId = orderCarSelect.value;
    const mechanicId = mechanicSelect.value;
    const selectedOperation = operationSelect.value;
    const consultationRequested = consultationRequestedCheckbox.checked;
    const scheduledAtValue = scheduledAtHidden.value;
    
    if (!clientId || !carId || !mechanicId || !selectedOperation || !scheduledAtValue) {
        log("Completeaza toate campurile si selecteaza ora disponibila", "error");
        return;
    }
    
    let serviceName = "Service";
    let laborCost = 50;
    
    if (selectedOperation) {
        serviceName = Array.from(operationSelect.options).find(o => o.value === selectedOperation)?.text || "Service";
        laborCost = operationPriceMap.get(selectedOperation) || 50;
    }
    
    const payload = {
        clientId: clientId,
        carId: carId,
        mechanicId: mechanicId,
        serviceName: serviceName,
        description: consultationRequested ? "Consult initial" : "Service appointment",
        laborCost: laborCost,
        scheduledAt: scheduledAtValue,
        requiredParts: [],
        selectedOperations: selectedOperation ? [selectedOperation] : [],
        consultationRequested: consultationRequested
    };

    try {
        await api("/api/service-orders", {method: "POST", body: JSON.stringify(payload)});
        event.target.reset();
        timeSlotsContainer.style.display = "none";
        log("Programare creata cu succes!");
        await refreshOrders();
    } catch (error) {
        log(error.message, "error");
    }
});

operationSelect.addEventListener("change", refreshEstimatedDuration);
consultationRequestedCheckbox.addEventListener("change", refreshEstimatedDuration);
mechanicSelect.addEventListener("change", loadTimeSlots);
appointmentDateInput.addEventListener("change", loadTimeSlots);
operationSelect.addEventListener("change", async () => {
    if (appointmentDateInput.value && availabilityTimeInput.value) {
        try {
            await refreshAvailableMechanics();
        } catch (error) {
            log(error.message, "error");
        }
    }
});
appointmentDateInput.addEventListener("change", async () => {
    if (availabilityTimeInput.value) {
        try {
            await refreshAvailableMechanics();
        } catch (error) {
            log(error.message, "error");
        }
    }
});
checkMechanicsButton.addEventListener("click", async () => {
    try {
        await refreshAvailableMechanics();
        log("Mechanics availability loaded");
    } catch (error) {
        log(error.message, "error");
    }
});
availabilityTimeInput.addEventListener("change", async () => {
    if (appointmentDateInput.value) {
        try {
            await refreshAvailableMechanics();
        } catch (error) {
            log(error.message, "error");
        }
    }
});

document.getElementById("delivery-form").addEventListener("submit", async event => {
    event.preventDefault();
    const body = fromForm(event.target);
    const payload = {
        partId: body.partId,
        supplierId: body.supplierId,
        quantity: Number(body.quantity),
        deliveredAt: new Date(body.deliveredAt).toISOString().slice(0, 19)
    };

    try {
        await api("/api/deliveries/receive", {method: "PATCH", body: JSON.stringify(payload)});
        event.target.reset();
        log("Delivery recorded and stock increased");
        await refreshParts();
    } catch (error) {
        log(error.message, "error");
    }
});

document.getElementById("refresh-orders").addEventListener("click", () => refreshOrders());
document.getElementById("refresh-parts").addEventListener("click", () => refreshParts());

async function bootstrap() {
    try {
        await Promise.all([
            refreshClients(),
            refreshSuppliers(),
            refreshCars(),
            refreshMechanics(),
            refreshOperationCatalog(),
            refreshParts(),
            refreshOrders()
        ]);
        log("Dashboard loaded");
    } catch (error) {
        log(`Startup failed: ${error.message}`, "error");
    }
}

bootstrap();
