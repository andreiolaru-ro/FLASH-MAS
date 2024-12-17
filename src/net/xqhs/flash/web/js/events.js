import { updateEntitiesList } from "./ui.js";

let eventBus = null;

export let appContext = {
    entitiesData: {
        specification: null,
        types: null,
        activators: null
    },
    selectedEntities: []
};

export function connectToServer() {
    return new Promise((resolve, _reject) => {
        eventBus = new EventBus('/eventbus');
        eventBus.onopen = () => {
            resolve();
            console.log('Connected to backend');
        };

        eventBus.onclose = () => {
            console.log('Connection lost. Reconnecting...');
            setTimeout(() => connectToServer().then(registerHandlers), 5000);
        };
    });
}

export function registerHandlers() {
    eventBus.registerHandler('server-to-client',
        (_, message) => recvMessage(JSON.parse(message.body)));
    eventBus.registerHandler('server-to-client-agent', 
        (_, message) => recvAgentMessage(JSON.parse(message.body)));
}

function recvMessage(message) {
    console.log('Received message', message);
    if (message.scope == 'global') {
        if (message.subject != 'entities list')
            return;

        appContext.entitiesData = message.content;
        console.log('Received new entities data', appContext.entitiesData);
        updateEntitiesList();
        return;
    }
    if (message.scope == 'port') {
        console.log("Port output to: ", message.content);
        for (let id in message.content) {
            const content = message.content[id];
            appContext.entitiesData.types[id] == 'form' ? $('#' + id).val(content) : $('#' + id).text(content);
        }
    }
}

function recvAgentMessage(message) {}

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
        scope: "port", 
        subject: child.id, 
        content
    }
    console.log("Sending message", msg);
	eventBus.send('client-to-server', JSON.stringify(msg));
}

export function sendData(id) {
	let input = { 
        type: 'operation',
        name: 'operation ' + $('#' + id).text().toLowerCase().replace(' ', '_'),
        parameters: 'parameters'
    };
	eventBus.send('client-to-server', JSON.stringify({ all: input }));
}