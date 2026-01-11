import { appContext, idOf } from "./common.js";
import { processSpec, applyUpdatesOnPort } from "./processing.js";
import { updateEntitiesList } from "./ui.js";

const CLIENT_TO_SERVER = 'client-to-server';
const SERVER_TO_CLIENT = 'server-to-client';
const WS_ENDPOINT = '/eventbus';
const RECONNECTION_TIMEOUT = 5000;

const GLOBAL_SCOPE = 'global';
const PORT_SCOPE = 'port';
const REGISTERED_SCOPE = 'registered';
const NOTIFY_SCOPE = 'notify';
const GLOBAL_COMMAND_SCOPE = 'global_command';
const DEPLOYMENT_SCOPE = 'deployment';

let eventBus = null;

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

export function registerHandler() {
    eventBus.registerHandler(SERVER_TO_CLIENT,
        (_, message) => recvMessage(JSON.parse(message.body)));

    eventBus.send(CLIENT_TO_SERVER, JSON.stringify({ scope: REGISTERED_SCOPE }));
}

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

function recvMessage(message) {
    console.log('Received message', message);
    if (message.scope == GLOBAL_SCOPE) {
        if (message.subject != 'entities list')
            return;

        appContext.entities = processSpec(message.content);
        console.log('Received new entities data', appContext.entities);
        updateEntitiesList();
        return;
    }
    if (message.scope == PORT_SCOPE) {
        const entityName = message.subject.entity;
        const port = message.subject.port;
        applyUpdatesOnPort(entityName, port, message.content);
    }
}

const replaceVariables = (str, replaceMap) =>
    str.replace(/\$([a-zA-Z_]\w*)/g, (_, key) => replaceMap[key] || '');

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

export function sendGlobalCommand(commandName) {
    const msg = {
        scope: GLOBAL_COMMAND_SCOPE,
        command: commandName
    };
    eventBus.send(CLIENT_TO_SERVER, JSON.stringify(msg));
}