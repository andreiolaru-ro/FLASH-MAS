import { appContext, idOf } from "./common.js";

/**
 * Preprocess the spec received from the server to the structure used by the frontend
 * @param {object} spec 
 * @returns {object} The processed spec object
 */
export function processSpec(spec) {
    let processedSpec = {};
    for (let [entityName, entitySpec] of Object.entries(spec)) {
        let entityData = Object.fromEntries(flattenSpec(entitySpec));
        processedSpec[entityName] = {
            id: entitySpec.id,
            data: entityData,
            children: entitySpec.children.map(({id}) => id),
            ports: groupPorts(entityData),
            styleTriggers: getStyleTriggers(entityName, entityData),
        };
    }
    return processedSpec;
}

// Flatten the specification of an entity into a list of [id, element] pairs
// including the children of containers and the container with children ids only
const flattenSpec = (entitySpec) => entitySpec.children.flatMap(({id, ...element}) =>
    element.type != 'container' ? [[id, element]] :
        [...flattenSpec(element), [id, {
            ...element, 
            children: element.children.map(({id}) => id)
        }]]);

// Retrieve the ports with all roles from the entity
const groupPorts = (entityData) => Object.entries(entityData).reduce((acc, [_id, element]) => {
    let roles = acc[element.port] || [];
    roles.push(element.role);
    acc[element.port] = roles;
    return acc;
}, {});

// Retrieve a map of element ids that trigger style changes on other elements
const getStyleTriggers = (entityName, entityData) => 
    Object.entries(entityData).reduce((acc, [id, element]) => {
        if (!element.when) return acc;
        for (let {conditions} of element.when) {
            for (let trigger of Object.keys(conditions)) {
                // The trigger is a "port/role" string
                let [port, role] = trigger.split('/');
                port = port || element.port;
                role = role || 'content';
                let triggerId = idOf(entityName, port, role);

                conditions[triggerId] = conditions[trigger];
                delete conditions[trigger];

                acc[triggerId] = acc[triggerId] || new Set();
                acc[triggerId].add(id);
            }
        }
        return acc;
    }, {});

export function applyUpdatesOnPort(entityName, port, roles) {
    console.log("Port output to: ", entityName + " " + port);
    const entity = appContext.entities[entityName];

    for (let role in roles) {
        const content = roles[role];
        const id = idOf(entityName, port, role);
        const data = entity.data[id];
        console.log("Setting content to: ", id, content);
        data.value = content;
        data.type == 'form' ? $('#' + id).val(content) : $('#' + id).text(content);

        if (id in entity.styleTriggers) 
            entity.styleTriggers[id].forEach(id => applyStyles(entity, id, $('#' + id)));
    }
}

// $element - jQuery element to apply the styles to (might not be added to the DOM yet)
export function applyStyles(entity, id, $element) {
    if (!(id in entity.data)) return;
    for (let {conditions, style} of entity.data[id].when) {
        let isValid = Object.entries(conditions).every(([trigger, value]) => 
            entity.data[trigger]?.value == value);
        const className = `conditional-style-${style}`;

        if (isValid) $element.addClass(className);
        else $element.removeClass(className);
    }
}