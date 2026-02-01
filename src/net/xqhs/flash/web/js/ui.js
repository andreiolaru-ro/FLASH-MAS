import { appContext } from "./common.js";
import {notifyEntity, sendDeployCommand, sendGlobalCommand, sendGlobalCommandWithPayload} from "./events.js";
import { applyStyles } from "./processing.js";

/**
 * Initializes the side panel logic, including opening/closing behavior and
 * binding handlers for global control buttons (Start, Stop, Pause Application).
 */
export function handleSidepanel() {
    $('#sidepanel-btn').on('click', function () {
        $('#sidepanel').toggleClass('open');
        $('#main-content').toggleClass('collapsed');
        $(this).hide();
    }).hide();

    $('#global-start-application-btn').on('click', () => {
        console.log("Start Application clicked");
        sendGlobalCommand('START_APPLICATION');
    });

    $('#sidepanel-close-btn').on('click', function () {
        $('#sidepanel').removeClass('open');
        $('#main-content').removeClass('collapsed');
        $('#sidepanel-btn').show();
    });

    // $('#global-start-btn').on('click', () => sendData('node-start_activate_0'));
    // $('#global-stop-btn').on('click', () => sendData('node-stop_activate_0'));
    // $('#global-pause-btn').on('click', () => sendData('pause-simulation_activate_0'));
    // $('#global-start-simulation-btn').on('click', () => sendData('start-simulation_activate_0'));
    // $('#global-stop-simulation-btn').on('click', () => sendData('stop-simulation_activate_0'));

    const btnStart = $('#global-start-application-btn');
    const btnPause = $('#global-pause-application-btn');
    const btnStop = $('#global-stop-application-btn');

    btnPause.prop('disabled', true);
    btnStop.prop('disabled', true);

    btnStart.on('click', () => {
        sendGlobalCommand('START_APPLICATION');
        btnPause.text("Pause");

        btnStart.prop('disabled', true);
        btnPause.prop('disabled', false);
        btnStop.prop('disabled', false);
    });

    btnStop.on('click', () => {
        sendGlobalCommand('STOP_APPLICATION');
        btnPause.text("Pause");

        btnStart.prop('disabled', false);
        btnPause.prop('disabled', true);
        btnStop.prop('disabled', true);
    });

    btnStart.on('click', () => {
        console.log("Start Application clicked");
        sendGlobalCommand('START_APPLICATION');

        btnPause.text("Pause");
    });
    btnPause.on('click', function() {
        const currentText = $(this).text();

        if (currentText === "Pause") {

            console.log("Pause clicked -> Sending PAUSE_APPLICATION");
            sendGlobalCommand('PAUSE_APPLICATION');

            $(this).text("Resume");

        } else {
            console.log("Resume clicked -> Sending START_APPLICATION");
            sendGlobalCommand('START_APPLICATION');

            $(this).text("Pause");
        }
    });
    btnStop.on('click', () => {
        console.log("Stop Application clicked");
        sendGlobalCommand('STOP_APPLICATION');

        btnPause.text("Pause");
    });
}

const DEFAULT_ARGS = "-loader agent:composite -node nodeC -pylon webSocket:clientPylon connectTo:ws://127.0.0.1:8886 -agent composite:AgentC -shard messaging -shard remoteOperation -shard swingGui from:basic-chat.yml -shard test.guiGeneration.BasicChatShard otherAgent:AgentA";

/**
 * Initializes the "Deploy" section logic, binding events to the "Add Daemon" and "Deploy All" buttons.
 */
export function initDeployButton() {
    const addBtn = document.getElementById('add-daemon-btn');
    const ipInput = document.getElementById('new-daemon-ip');
    const listContainer = document.getElementById('daemon-list');
    const deployAllBtn = document.getElementById('deploy-all-btn');

    if (addBtn) {
        addBtn.onclick = () => {
            const ip = ipInput.value.trim();
            if (!ip) return alert("Please enter an IP address!");
            addDaemonCard(ip, listContainer, " ... generic config ...");
        };
    }

    if (deployAllBtn) {
        deployAllBtn.onclick = () => {
            deployAll();
        };
    }
}

/**
 * Updates the configuration arguments for daemons in the UI based on data received from the server.
 * If a daemon card does not exist for an IP, it creates one.
 *
 * @param {Object} configMap - A map of IP addresses to configuration strings.
 */
export function updateNodeConfigs(configMap) {
    const listContainer = document.getElementById('daemon-list');
    if (!listContainer) return;

    Object.keys(configMap).forEach(ip => {
        const args = configMap[ip];
        const exists = Array.from(listContainer.children).some(card => card.dataset.ip === ip);
        if (!exists) {
            addDaemonCard(ip, listContainer, args);
        }
    });
}

/**
 * Adds a new visual card for a daemon to the specified container.
 *
 * @param {string} ip - The IP address of the daemon.
 * @param {HTMLElement} container - The element to append the card to.
 * @param {string} [initialArgs=""] - Optional initial startup arguments.
 */
function addDaemonCard(ip, container, initialArgs = "") {
    const defaultText = initialArgs || "-loader agent:composite -node nodeC -pylon webSocket:clientPylon connectTo:ws://127.0.0.1:8886 -agent composite:AgentC -shard messaging -shard remoteOperation -shard swingGui from:basic-chat.yml -shard test.guiGeneration.BasicChatShard otherAgent:AgentA";

    const card = document.createElement('div');
    card.classList.add('daemon-card');
    card.dataset.ip = ip;

    card.innerHTML = `
        <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:10px;">
            <strong style="font-size: 1.1em; color: #333;">Daemon: ${ip}</strong>
            <button class="remove-btn" style="background:#ff4444; color:white; border:none; padding: 5px 10px; border-radius:4px; cursor:pointer;">Remove</button>
        </div>
        
        <div style="margin-bottom: 10px;">
            <label style="font-size:0.85em; color: #666; display:block; margin-bottom: 5px;">Startup Arguments:</label>
            <textarea class="args-input" style="width:100%; height:80px; font-family:monospace; font-size:0.85em; padding: 8px; border: 1px solid #ccc; border-radius: 4px; resize: vertical;">${DEFAULT_ARGS}</textarea>
        </div>

        <div style="display:flex;">
            <button class="deploy-btn" style="background: #2196F3; color: white; border:none; padding: 10px; border-radius:4px; cursor:pointer; width: 100%; font-weight: bold;">
                DEPLOY NODE
            </button>
        </div>
    `;

    const deployBtn = card.querySelector('.deploy-btn');
    if (deployBtn) {
        deployBtn.onclick = () => {
            const args = card.querySelector('.args-input').value;
            import("./events.js").then(module => {
                module.sendDeployCommand(ip, args);
            });
        };
    }

    const removeBtn = card.querySelector('.remove-btn');
    if (removeBtn) {
        removeBtn.onclick = () => {
            container.removeChild(card);
        };
    }

    container.appendChild(card);
}

/**
 * Refreshes the list of entities in the side panel.
 * Handles checkbox selection logic for filtering which entities are displayed in the main view.
 */
export function updateEntitiesList() {
    $('#entities-list').empty();
    const selectedEntities = [];
    for (let [entityName, entity] of Object.entries(appContext.entities)) {
        const selected = appContext.selectedEntities.includes(entityName);
        if (selected) selectedEntities.push(entityName);

        const checkbox = $('<input>').addClass('entity-checkbox')
            .attr('type', 'checkbox').attr('name', entityName + '-checkbox')
            .attr('checked', selected)
            .on('change', e => handleEntityCheckboxChange(e.target.checked, entityName))
            .on('click', e => e.stopPropagation());
        const label = $('<label>').text(entityName)
            .addClass('entity-label').attr('for', entityName + '-checkbox');

        $('#entities-list').append(
            $('<div>').attr('id', entity.id).addClass('side-drawer entity-item')
                .append(checkbox, label)
                .on('click', function () {
                    checkbox.prop('checked', !checkbox.prop('checked'));
                    checkbox.trigger('change')
                })
        );
    }
    appContext.selectedEntities = selectedEntities;
    updateSelectedEntities();
}

function handleEntityCheckboxChange(checked, entityName) {
    if (checked) {
        appContext.selectedEntities.push(entityName);
    } else {
        appContext.selectedEntities = appContext.selectedEntities.filter(e => e != entityName);
    }
    updateSelectedEntities();
}

function updateSelectedEntities() {
    $('#main-header > h1').text(appContext.selectedEntities.length ?
        'Selected Entities' : 'No Entities Selected');

    $('#selected-entities-list').empty();
    for (let entityName of appContext.selectedEntities) {
        const entity = appContext.entities[entityName];
        const componentBuilder = new AgentComponentBuilder(entityName);
        $('#selected-entities-list').append($('<div>').addClass('selected-entity').append(
            $('<h2>').text(entityName),
            entity.children.map(childId => {
                const child = entity.data[childId];
                const item = componentBuilder.build(child, childId);
                return item;
            })
        ));
    }
}

/**
 * Renders the list of daemons into the main table.
 * Maintains user-edited configuration text even after a list refresh.
 * Updates the "Deploy All" button count.
 *
 * @param {Array} list - Array of daemon objects containing status, IP, port, etc.
 */
export function renderDaemonList(list) {
    const tbody = document.getElementById("daemon-list-body");
    if (!tbody) return;

    const currentConfigs = {};
    document.querySelectorAll('.config-textarea').forEach(el => {
        currentConfigs[el.getAttribute('data-ip')] = el.value;
    });

    tbody.innerHTML = "";

    const deployAllBtn = document.getElementById('deploy-all-btn');
    if (deployAllBtn) {
        const readyCount = list ? list.filter(d => d.status === "ONLINE" && !d.deployed).length : 0;

        deployAllBtn.innerText = `Deploy ${readyCount} configuration(s)`;

        deployAllBtn.style.opacity = readyCount > 0 ? "1" : "0.6";
        deployAllBtn.disabled = readyCount === 0;
    }

    if (!list || list.length === 0) {
        tbody.innerHTML = "<tr><td colspan='4' style='text-align:center;'>No daemons connected. Add one above.</td></tr>";
        return;
    }

    list.forEach(d => {
        const tr = document.createElement("tr");

        let statusClass = "red";
        let statusText = "OFFLINE";
        if (d.status === "ONLINE") { statusClass = "green"; statusText = "ONLINE"; }
        else if (d.status === "ERROR") { statusClass = "yellow"; statusText = "ERROR"; }

        const statusCell = `<td><span class='status-dot ${statusClass}'></span> ${statusText}</td>`;

        const addrCell = `<td><strong>${d.ip}</strong><br><span style='font-size:0.8em; color:#666'>:${d.port}</span></td>`;

        const defaultArgs = `-loader agent:composite -node node_${d.ip.replace(/\./g,'_')} -pylon webSocket:clientPylon connectTo:ws://127.0.0.1:8886 -agent composite:AgentC -shard messaging -shard remoteOperation -shard swingGui from:basic-chat.yml -shard test.guiGeneration.BasicChatShard otherAgent:AgentA`;
        const configVal = currentConfigs[d.ip] || defaultArgs;

        const configCell = `<td class="config-cell">
            <textarea id="config-${d.ip}" data-ip="${d.ip}" class="config-textarea" spellcheck="false" 
            style="width:100%; height:60px; font-family:monospace; font-size:0.85em; resize:vertical;">${configVal}</textarea>
        </td>`;

        let deployBtn;
        if (d.deployed) {
            deployBtn = `<button class='deploy-btn' style="background-color:#28a745; color:white; cursor:default;" disabled>✔ Deployed</button>`;
        } else if (d.status === "ONLINE") {
            deployBtn = `<button class='deploy-btn' id="btn-deploy-${d.ip}" onclick="ui.deploySingle('${d.ip}', ${d.port})">🚀 Deploy</button>`;
        } else {
            deployBtn = `<button disabled style='opacity:0.5;'>Wait...</button>`;
        }

        const killJvmBtn = `<button onclick="ui.killNode('${d.ip}', ${d.port})" style="background:#ff9800; color:white; border:none; padding:4px 8px; font-size:11px; margin-left:5px; cursor:pointer;">Kill JVM</button>`;
        const killDaemonBtn = `<button onclick="ui.killDaemon('${d.ip}', ${d.port})" style="background:#f44336; color:white; border:none; padding:4px 8px; font-size:11px; margin-left:5px; cursor:pointer;">Kill Daemon</button>`;

        const actionCell = `<td class='daemon-actions' style="white-space:nowrap;">
            ${deployBtn}
            <div style="margin-top:5px;">${killJvmBtn} ${killDaemonBtn}</div>
        </td>`;

        tr.innerHTML = statusCell + addrCell + configCell + actionCell;
        tbody.appendChild(tr);
    });
}

/**
 * Reads user input from the daemon form and sends a CONNECT_DAEMON command.
 */
export function connectDaemon() {
    const ip = document.getElementById("new-daemon-ip").value;
    const port = document.getElementById("new-daemon-port").value;
    if (!ip) { alert("Please enter an IP"); return; }

    sendGlobalCommandWithPayload("deployment", {
        command: "CONNECT_DAEMON",
        ip: ip,
        port: parseInt(port)
    });
}

/**
 * Triggers deployment for a single daemon using its specific configuration.
 *
 * @param {string} ip - The IP of the target daemon.
 * @param {number} port - The port of the target daemon.
 */
export function deploySingle(ip, port) {
    const textArea = document.getElementById(`config-${ip}`);
    if (!textArea) return;

    const args = textArea.value.trim();

    const btn = document.getElementById(`btn-deploy-${ip}`);
    if(btn) { btn.innerText = "⏳ Sending..."; btn.disabled = true; }

    sendGlobalCommandWithPayload("deployment", {
        command: "DEPLOY_REMOTE",
        targetIp: ip,
        port: parseInt(port),
        startupArgs: args
    });
}

/**
 * Triggers batch deployment for all online and undeployed daemons.
 * Asks for user confirmation before proceeding.
 */
export function deployAll() {
    const deployableButtons = [];
    const textAreas = document.querySelectorAll('.config-textarea');

    textAreas.forEach(ta => {
        const ip = ta.getAttribute('data-ip');
        const btn = document.getElementById(`btn-deploy-${ip}`);

        if (btn && !btn.disabled) {
            deployableButtons.push(btn);
        }
    });

    if (deployableButtons.length === 0) {
        alert("No undeployed, online nodes found to deploy.");
        return;
    }

    if (confirm(`Are you sure you want to deploy ${deployableButtons.length} nodes?`)) {
        deployableButtons.forEach(btn => btn.click());
    }
}

/**
 * Requests the latest list of daemons from the server.
 */
export function refreshDaemons() {
    sendGlobalCommand("GET_DAEMONS_LIST");
}

/**
 * Sends a command to kill the remote JVM (Node) on the specified IP.
 *
 * @param {string} ip - The IP of the target daemon.
 * @param {number} port - The port of the target daemon.
 */
export function killNode(ip, port) {
    if(!confirm(`Kill JVM on ${ip}?`)) return;
    sendGlobalCommandWithPayload("deployment", {
        command: "KILL_NODE_REMOTE",
        ip: ip,
        port: parseInt(port)
    });
}

/**
 * Sends a command to kill the remote Daemon process on the specified IP.
 * This will result in a lost connection.
 *
 * @param {string} ip - The IP of the target daemon.
 * @param {number} port - The port of the target daemon.
 */
export function killDaemon(ip, port) {
    if(!confirm(`Kill DAEMON on ${ip}? Connection will be lost.`)) return;
    sendGlobalCommandWithPayload("deployment", {
        command: "KILL_DAEMON_REMOTE",
        ip: ip,
        port: parseInt(port)
    });
}

/**
 * Sends a command to kill ALL running JVMs (Nodes) across all connected daemons.
 */
export function killAllJVMs() {
    if(confirm("⚠️ ARE YOU SURE?\nThis will kill ALL running Java Nodes (Agents).")) {
        sendGlobalCommand("KILL_ALL_JVMS");
    }
}

/**
 * Sends a command to kill ALL connected Daemons.
 */
export function killAllDaemons() {
    if(confirm("⚠️ CRITICAL WARNING\nThis will shut down ALL remote Daemons.\nYou will have to manually restart them on each machine.")) {
        sendGlobalCommand("KILL_ALL_DAEMONS");
    }
}

/** Exposes functions to create the UI components for the agent */
class AgentComponentBuilder {
    /** @param entityName {string} */
    constructor(entityName) {
        this.entityName = entityName;
        this.entity = appContext.entities[entityName];
        this.data = this.entity.data;
    }

    label(el, id) {
        return $('<label>').text(el.value).id(id);
    }

    button(el, id) {
        let btn = $('<button>').id(id).text(el.value);
        if ('notify' in el) {
            btn.on('click', () => notifyEntity(el, this.entityName));
        }
        return btn;
    }

    form(el, id) {
        let form = $('<input>').attr('type', 'text').id(id).val(el.value)
            .on('change', e => el.value = e.target.value);
        if ('notify' in el)
            form.on('enter', () => notifyEntity(el, this.entityName));
        return form;
    }

    spinner(el, id) {
        let spinner = $('<input>').attr('type', 'number').id(id).val(el.value)
            .on('change', e => el.value = parseFloat(e.target.value));
        if ('notify' in el)
            spinner.on('change', () => notifyEntity(el, this.entityName));
        return spinner;
    }

    container(el, id) {
        let container = $('<div>').id(id).addClass('agent-container')
            .css('--direction', el.properties.layout);
        el.children.forEach(childId => {
            const child = this.data[childId];
            const item = this.build(child, childId);
            if (item) container.append(item);
            item.attr('id', childId);
        });
        return container;
    }

    build(el, id) {
        if (!(el.type in this)) {
            console.warn(`No builder for element type ${el.type}`);
            return null;
        }
        let $element = this[el.type](el, id);
        applyStyles(this.entity, id, $element);
        return $element;
    }
};