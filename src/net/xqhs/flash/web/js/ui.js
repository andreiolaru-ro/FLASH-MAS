import { appContext, sendData, activate } from "./events.js";

// let sidebarEntities = [];

export function handleSidepanel() {
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
}

export function updateEntitiesList() {
    $('#entities-list').empty();
    for (let [entityName, entity] of Object.entries(appContext.entitiesData.specification)) {
        const checkbox = $('<input>').addClass('entity-checkbox')
            .attr('type', 'checkbox').attr('name', entityName + '-checkbox')
            .on('change', e => handleEntityCheckboxChange(e.target.checked, entityName))
            .on('click', e => e.stopPropagation());
        const label = $('<label>').text(entityName)
            .addClass('entity-label').attr('for', entityName + '-checkbox');

        $('#entities-list').append(
            $('<div>').attr('id', entity.id).addClass('side-drawer entity-item')
            .append(checkbox, label)
            .on('click', function() { 
                checkbox.prop('checked', !checkbox.prop('checked'));
                checkbox.trigger('change')
            })
        );
    }
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
    $('#main-header > h1').text(appContext.selectedEntities.length ? 'Selected Entities' : 'No Entities Selected');
    $('#selected-entities-list').empty();
    console.log('Selected entities', appContext.selectedEntities);
    for (let entityName of appContext.selectedEntities) {
        const entity = appContext.entitiesData.specification[entityName];
        $('#selected-entities-list').append($('<div>').addClass('selected-entity').append(
            $('<h2>').text(entityName),
            entity.children.map(child => {
                let item = agentComponents[child.type]?.(child);
                if (!(child.type in agentComponents)) {
                    console.log('Unknown child type', child);
                    return null;
                }
                item.attr('id', child.id);
                return item;
            })
        ));
    }
}

// different components that can be rendered in an entity UI
const agentLabel = (el) => $('<label>').text(el.value);
const agentButton = (el) => {
    let btn = $('<button>').text(el.value);
    if (el.role == 'activate')
        btn.on('click', () => activate(el));
    return btn;
}
const agentInput = (el) => $('<input>').attr('type', 'text').val(el.value);
const agentContainer = (el) => {
    let container = $('<div>').addClass('agent-container').css('--direction', el.properties.layout);
    el.children.forEach(child => {
        let item = agentComponents[child.type]?.(child);
        if (item) container.append(item);
    });
    return container;
}

const agentComponents = {
    label: agentLabel,
    button: agentButton,
    form: agentInput,
    container: agentContainer
};


