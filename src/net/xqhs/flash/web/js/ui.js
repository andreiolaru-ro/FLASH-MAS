import { appContext } from "./common.js";
import { notifyEntity } from "./events.js";
import { applyStyles } from "./processing.js";

export function handleSidepanel() {
    $('#sidepanel-btn').on('click', function () {
        $('#sidepanel').toggleClass('open');
        $('#main-content').toggleClass('collapsed');
        $(this).hide();
    }).hide();

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
}

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
        return $('<input>').attr('type', 'text').id(id).val(el.value)
            .on('change', e => el.value = e.target.value)
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


