import { appContext, idOf } from "./common.js";
import { processSpec, applyUpdatesOnPort } from "./processing.js";

/**
 * EventBus address for Client -> Server communication.
 */
const CLIENT_TO_SERVER = 'client-to-server';

/**
 * EventBus address for Server -> Client communication.
 */
const SERVER_TO_CLIENT = 'server-to-client';

/**
 * The WebSocket endpoint for the EventBus.
 */
const WS_ENDPOINT = '/eventbus';

/**
 * Timeout in milliseconds before attempting to reconnect to the server.
 */
const RECONNECTION_TIMEOUT = 5000;

const GLOBAL_SCOPE = 'global';
const PORT_SCOPE = 'port';
const REGISTERED_SCOPE = 'registered';
const NOTIFY_SCOPE = 'notify';
const GLOBAL_COMMAND_SCOPE = 'global_command';
const DEPLOYMENT_SCOPE = 'deployment';

/**
 * The EventBus instance.
 */
let eventBus = null;

/**
 * Establishes the connection to the backend via Vert.x EventBus.
 * Handles onopen and onclose events, including automatic reconnection logic.
 *
 * @returns {Promise<void>} A promise that resolves when the connection is established.
 */
export function connectToServer() {
    return new Promise((resolve, _reject) => {
        eventBus = new EventBus(WS_ENDPOINT);
        eventBus.onopen = () => {
            resolve();
            console.log('Connected to backend');
        };

        eventBus.onclose = () => {
            console.log('Connection lost. Reconnecting...');
            setTimeout(() => connectToServer().then(registerHandler), RECONNECTION_TIMEOUT);
        };
    });
}

/**
 * Registers the message handler for incoming server messages and sends an initial
 * registration message to the server to announce this client.
 */
export function registerHandler() {
    eventBus.registerHandler(SERVER_TO_CLIENT,
        (_, message) => recvMessage(JSON.parse(message.body)));

    eventBus.send(CLIENT_TO_SERVER, JSON.stringify({ scope: REGISTERED_SCOPE }));
}

/**
 * Sends a deployment command to the server to start a new node on a remote machine.
 *
 * @param {string} ip - The IP address of the target machine.
 * @param {string} rawArguments - The startup arguments for the node.
 */
export function sendDeployCommand(ip, rawArguments) {
    if (!eventBus || eventBus.state !== EventBus.OPEN) {
        alert("Not connected to server. Cannot send deploy command.");
        return;
    }

    const msg = {
        scope: DEPLOYMENT_SCOPE,
        command: "DEPLOY_REMOTE",
        targetIp: ip,
        startupArgs: rawArguments
    };

    console.log("Sending deploy command via EventBus:", msg);
    eventBus.send('client-to-server', JSON.stringify(msg));
}

/**
 * Handles incoming messages from the server.
 * Dispatches logic based on the message scope (GLOBAL or PORT).
 *
 * @param {Object} message - The parsed JSON message received from the server.
 */
function recvMessage(message) {
    console.log('Received message', message);
    if (message.scope === GLOBAL_SCOPE) {
        if (message.subject === 'entities list') {
            appContext.entities = processSpec(message.content);
            console.log('Received new entities data', appContext.entities);
            import("./ui.js").then(ui => ui.updateEntitiesList());
            return;
        }

        if (message.subject == 'node-configs') {
            console.log('Received node configs', message.content);
            import("./ui.js").then(ui => ui.updateNodeConfigs(message.content));
            return;
        }

        if (message.subject === 'daemons list') {
            import("./ui.js").then(ui => {
                ui.renderDaemonList(message.content.list);
            });
            return;
        }
        return;
    }
    if (message.scope == PORT_SCOPE) {
        const entityName = message.subject.entity;
        const port = message.subject.port;
        applyUpdatesOnPort(entityName, port, message.content);
    }
}

/**
 * Replaces variable placeholders in a string (e.g., $entity) with actual values.
 *
 * @param {string} str - The string containing placeholders.
 * @param {Object} replaceMap - A map of keys to values for replacement.
 * @returns {string} The processed string.
 */
const replaceVariables = (str, replaceMap) =>
    str.replace(/\$([a-zA-Z_]\w*)/g, (_, key) => replaceMap[key] || '');

/**
 * Sends a notification message to the server.
 * Collects data from the relevant ports and constructs the message payload.
 *
 * @param {Object} trigger - The UI element configuration that triggered the event.
 * @param {string} entityName - The name of the entity associated with the UI element.
 */
export function notifyEntity(trigger, entityName) {
    const notifyTarget = replaceVariables(trigger.notify || '', {
        entity: entityName,
        port: trigger.port,
        role: trigger.role
    });

    console.log("Notify: " + idOf(entityName, trigger.port, trigger.role));
    const entity = appContext.entities[entityName];
    const content = Object.fromEntries(entity.ports[trigger.port].map(role => {
        let child = entity.data[idOf(entityName, trigger.port, role)];
        let value = (child.type == 'button') ? (role == trigger.role) : child.value;
        return [role, value];
    }));

    const msg = {
        scope: NOTIFY_SCOPE,
        source: [entityName, trigger.port, trigger.role],
        subject: [entityName, ...notifyTarget.split('/')],
        content: content
    };
    eventBus.send(CLIENT_TO_SERVER, JSON.stringify(msg));
}

/**
 * Sends a global command (without payload) to the server.
 * Used for high-level operations like START_APPLICATION.
 *
 * @param {string} commandName - The name of the global command.
 */
export function sendGlobalCommand(commandName) {
    const msg = {
        scope: GLOBAL_COMMAND_SCOPE,
        command: commandName
    };
    eventBus.send(CLIENT_TO_SERVER, JSON.stringify(msg));
}

/**
 * Sends a global command with a custom payload object to the server.
 *
 * @param {string} scope - The message scope (e.g., 'deployment').
 * @param {Object} payload - The JSON object containing command details.
 */
export function sendGlobalCommandWithPayload(scope, payload) {
    if (!payload.scope) payload.scope = scope;
    if (eventBus && eventBus.state === EventBus.OPEN) {
        eventBus.send('client-to-server', JSON.stringify(payload));
    } else {
        console.error("EventBus not connected");
    }
}