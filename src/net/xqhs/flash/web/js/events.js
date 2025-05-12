import { appContext, idOf } from "./common.js";
import { processSpec, applyUpdatesOnPort } from "./processing.js";
import { updateEntitiesList } from "./ui.js";

const CLIENT_TO_SERVER = 'client-to-server';
const SERVER_TO_CLIENT = 'server-to-client';
const WS_ENDPOINT = '/eventbus';
const RECONNECTION_TIMEOUT = 5000;  // in ms

const GLOBAL_SCOPE = 'global';
const PORT_SCOPE = 'port';

const REGISTERED_SCOPE = 'registered';
const NOTIFY_SCOPE = 'notify';

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

/**
 * Replaces dollar-sign variables in a string with values from a map.
 * @param {string} str - The string containing variables to replace.
 * @param {Record<string, string>} replaceMap - The map containing variable names and their values.
 * @example replaceVariables('$port/$role', { port: 'myPort', role: 'myRole' }) // returns 'myPort/myRole'
 */
const replaceVariables = (str, replaceMap) =>
    str.replace(/\$([a-zA-Z_]\w*)/g, (_, key) => replaceMap[key] || '');

/**
 * Used to send a message to the server when an element with a 'notify' property
 * is interacted with. The message contains the data from all the elements of the
 * port, indexed by their role. For buttons, the value 'true' is sent for the pressed
 * button and 'false' for the rest. Same for checkboxes. The message will be redirected 
 * by the server to the corresponding entity on the route given by the 'notify' property.
 * @param {InterfaceElement} trigger - The element that was interacted with.
 * @param {string} entityName - The name of the entity to notify.
 * @returns {void}
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
        subject: [entityName, ...notifyTarget.split('/')],
        content: content
    };
    console.log("Sending message", msg);
    eventBus.send(CLIENT_TO_SERVER, JSON.stringify(msg));
}