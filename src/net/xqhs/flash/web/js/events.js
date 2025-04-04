import { updateEntitiesList } from "./ui.js";

const CLIENT_TO_SERVER = 'client-to-server';
const SERVER_TO_CLIENT = 'server-to-client';
const WS_ENDPOINT = '/eventbus';
const GLOBAL_SCOPE = 'global';
const PORT_SCOPE = 'port';
const REGISTERED_SCOPE = 'registered';
const STATUS_PORT = 'standard-status';
const RECONNECTION_TIMEOUT = 5000;  // in ms

let eventBus = null;

export let appContext = {
    entitiesData: {             // to be removed soon
        specification: null,
        types: null,
        activators: null
    },
    entities: {},
    selectedEntities: []
};

export const idOf = (entity, port, role) => CSS.escape(`${entity}_${port}_${role}`);

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

// Flatten the specification into a list of [id, element] pairs
// including the children of containers and the container with children ids only
const flattenSpec = (spec) => spec.children.flatMap(({id, ...element}) =>
    element.type != 'container' ? [[id, element]] :
        [...flattenSpec(element), [id, {
            ...element, 
            children: element.children.map(({id}) => id)
        }]]);

function recvMessage(message) {
    console.log('Received message', message);
    if (message.scope == GLOBAL_SCOPE) {
        if (message.subject != 'entities list')
            return;

        for (let [entityName, spec] of Object.entries(message.content)) {
            let entityData = Object.fromEntries(flattenSpec(spec));
            appContext.entities[entityName] = {
                id: spec.id,
                data: entityData,
                children: spec.children.map(({id}) => id),
                // status: entityData[idOf(entityName, STATUS_PORT, 'content')].value
            };
        }
        console.log('Received new entities data', appContext.entities);
        updateEntitiesList();
        return;
    }
    if (message.scope == PORT_SCOPE) {
        const entity = message.subject.entity;
        const port = message.subject.port;
        console.log("Port output to: ", entity + " " + port);

        for (let role in message.content) {
            const content = message.content[role];
            const id = idOf(entity, port, role);
            console.log("Setting content to: ", id, content);
            const data = appContext.entities[entity].data[id];
            data.value = content;
            data.type == 'form' ? $('#' + id).val(content) : $('#' + id).text(content);

            if (port == 'standard-status' && role == 'content')
                console.log(`Status update: ${content}`);
        }
    }
}

function changeEntityStatus(entity, status) {
    appContext.entities[entity].status = status;
    $(appContext.entities[entity].specification.id).addClass(status);
}

export function activate(child) {
    if (!(child.id in appContext.entitiesData.activators)) {
        console.log("Activated element not in active ports:", child.id);
        return;
    }

    const activator = appContext.entitiesData.activators[child.id];
    const content = Object.fromEntries(activator.map(id => [
        id, appContext.entitiesData.types[id] == 'form' ? $('#' + id).val() : $('#' + id).text()
    ]));

    const msg = {
        scope: PORT_SCOPE, 
        subject: child.id, 
        content
    }
    console.log("Sending message", msg);
	eventBus.send(CLIENT_TO_SERVER, JSON.stringify(msg));
}

export function sendData(id) {
	let input = { 
        type: 'operation',
        name: 'operation ' + $('#' + id).text().toLowerCase().replace(' ', '_'),
        parameters: 'parameters'
    };
	eventBus.send(CLIENT_TO_SERVER, JSON.stringify({ all: input }));
}