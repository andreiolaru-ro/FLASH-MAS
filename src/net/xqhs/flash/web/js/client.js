$(async function() {

// UI controls
$('#sidepanel-btn').on('click', function() {
    $('#sidepanel').toggleClass('open');
    $('#main-content').toggleClass('collapsed');
    $(this).hide();
}).hide();

$('#sidepanel-close-btn').on('click', function() {
    $('#sidepanel').removeClass('open');
    $('#main-content').removeClass('collapsed');
    $('#sidepanel-btn').show();
});

$('#global-start-btn').on('click', () => sendData('node-start_activate_0'));
$('#global-stop-btn').on('click', () => sendData('node-stop_activate_0'));
$('#global-pause-btn').on('click', () => sendData('pause-simulation_activate_0'));
$('#global-start-simulation-btn').on('click', () => sendData('start-simulation_activate_0'));
$('#global-stop-simulation-btn').on('click', () => sendData('stop-simulation_activate_0'));

// Events & Data
let eventBus = null;
let entitiesData = {
    specification: null,
    types: null,
    activators: null
};
let selectedEntities = [];

eventBus = new EventBus('/eventbus');
eventBus.onclose = () => console.log('Eventbus closed');

// wait for the eventbus to open
await new Promise((res, _rej) => eventBus.onopen = res);

console.log('Eventbus opened');
eventBus.registerHandler('server-to-client',
    (_, message) => recvMessage(JSON.parse(message.body)));
eventBus.registerHandler('server-to-client-agent-message', 
    (_, message) => recvAgentMessage(JSON.parse(message.body)));

function recvMessage(message) {
    console.log('Received message', message);
    if (message.scope == 'global') {
        if (message.subject != 'entities list')
            return;

        entitiesData = message.content;
        console.log('Received new entities data', entitiesData);
        updateEntitiesList();
        return;
    }
    if (message.scope == 'port') {
        console.log("Port output to: ", message.content);
        for (let id in message.content) {
            const content = message.content[id];
            entitiesData.types[id] == 'form' ? $('#' + id).val(content) : $('#' + id).text(content);
        }
    }
}

function updateEntitiesList() {
    $('#entities-list').empty();
    for (let [entityName, entity] of Object.entries(entitiesData.specification)) {
        $('#entities-list').append(
            $('<div>').attr('id', entity.id).addClass('side-drawer entity-item').append(
                $('<input>').attr('type', 'checkbox').addClass('entity-checkbox')
                    .on('change', e => handleEntityCheckboxChange(e.target.checked, entityName)),
                $('<label>').text(entityName).addClass('entity-label')
            )
        );
    }
}

function handleEntityCheckboxChange(checked, entityName) {
    if (checked) {
        selectedEntities.push(entityName);
    } else {
        selectedEntities = selectedEntities.filter(e => e != entityName);
    }
    updateSelectedEntities();
}

function updateSelectedEntities() {
    $('#main-header > h1').text(selectedEntities.length ? 'Selected Entities' : 'No Entities Selected');
    $('#selected-entities-list').empty();
    console.log('Selected entities', selectedEntities);
    for (let entityName of selectedEntities) {
        const entity = entitiesData.specification[entityName];
        $('#selected-entities-list').append($('<div>').addClass('selected-entity').append(
            $('<h2>').text(entityName),
            entity.children.map(child => {
                let item = null;
                if (child.type == 'label') {
                    item = $('<label>').text(child.value);
                } else if (child.type == 'button') {
                    item = $('<button>').text(child.value || child.id);
                    if (child.role == 'activate')
                        item.on('click', () => activate(child));
                } else if (child.type == 'form') {
                    item = $('<input>').attr('type', 'text').val(child.value);
                } else {
                    console.log('Unknown child type', child);
                    return null;
                }
                item.attr('id', child.id);
                return item;
            })
        ));
    }
}

function activate(child) {
    if (!(child.id in entitiesData.activators)) {
        console.log("Activated element not in active ports:", child.id);
        return;
    }
    const activator = entitiesData.activators[child.id];
    const content = Object.fromEntries(activator.map(id => [
        id, child.type == 'form' ? $('#' + id).val() : $('#' + id).text()
    ]));
    const msg = {
        scope: "port", 
        subject: child.id, 
        content
    }
	eventBus.send('client-to-server', JSON.stringify(msg));
}

function recvAgentMessage(message) {}

function sendData(id) {
	let input = { 
        type: 'operation',
        name: 'operation ' + $('#' + id).text().toLowerCase().replace(' ', '_'),
        parameters: 'parameters'
    };
	eventBus.send('client-to-server', JSON.stringify({ all: input }));
}

});