var eb;
var data;
var entities;
var interfaces;
var specification = false;
var entity_elements = {};
var interface_elements = {};
var selected_entities = 0;

function init() {
    eb = new EventBus("/eventbus");

    eb.onopen = () => {
        console.log('Eventbus opened');
        eb.registerHandler('server-to-client', (error, message) => {
           data = JSON.parse(message.body);
            if(!specification) {
                entities = document.getElementById(data['entities']);
                interfaces = document.getElementById(data['interfaces']);

                for(var element in data) {
                    var elements = element.split(' ');

                    if(elements[0] === 'entity')
                        entity_elements[elements[1]] = data[element];
                    if(elements[0] === 'interface')
                        interface_elements[elements[1]] = data[element];

                    //console.log('wrong specification')
                }
                specification = true;
            }
            else {
                var empty = jQuery.isEmptyObject(data);

                for (var index = 0; index < entities.children.length; index++) {
                    var entity = entities.children[index];
                    var checked = entity.children[0].checked;

                    if (empty || !data.hasOwnProperty(entity.id)) {

                        if (checked) {

                            // TODO: remove coresponding interface

                        }
                        entities.removeChild(entity);
                    } else if (!empty) {
                        for (var element in entity_elements) {

                            var type = entity_elements[element].split(" ");

                            if (type[0] === 'label') {

                                // TODO: modify coresponding label for entity

                            }

                            if (type[0] === 'spinner') {

                                // TODO: modify coresponding spinner for entity

                            }
                        }

                        if (checked) {

                            // TODO: modify coresponding interface

                        }
                    }
                }
                if (empty) {
                    entities.innerText = "No entities";
                    interfaces.innerText = "No entities selected";
                } else {
                    entities.innerText = "Entities";
                    if(selected_entities === 0)
                        interfaces.innerText = "No entities selected";
                    else
                        interfaces.innerText = "Selected entities";
                    for (var entity in data) {
                        //console.log(data[entity]);
                        if (document.getElementById('checkbox_' + entity) === null) {
                            new_entity(entity);
                        }
                    }
                }
            }
        });
        eb.send('client-to-server', "init");
    }
    eb.onclose = () => {
        console.log('Eventbus closed')
    }
};

function new_entity(entity) {
    var div = document.createElement('div');
    div.setAttribute('class', 'entity');
    div.setAttribute('id', entity);

    var checkbox =  document.createElement('input');
    checkbox.setAttribute('type', 'checkbox');
    checkbox.setAttribute('id', 'checkbox_' + entity);
    checkbox.setAttribute('onclick', 'checkbox(\'' + entity + '\')');

    var select = document.createElement('label');
    select.setAttribute('for', 'checkbox_' + entity);
    select.innerText = 'Select';

    var name = document.createElement('label');
    name.setAttribute('class', 'entity-name');
    name.innerText = entity;

    div.appendChild(checkbox);
    div.appendChild(select);
    div.appendChild(name);

    for(var element in entity_elements) {
        var type = entity_elements[element].split(' ');

        if(type[0] === 'button') {

            var button = document.createElement('input');
            button.setAttribute('type', 'text');
            button.setAttribute('class', 'entity-element');
            button.setAttribute('onclick', 'button(\'' + entity + ', ' + element + '\')');
            button.setAttribute('value', element);
            div.appendChild(button);

        }

        if(type[0] === 'label') {

            var label = document.createElement('label');
            label.setAttribute('id', 'label_' + entity + '_' + type[1]);
            label.setAttribute('class', 'entity-element');
            label.setAttribute('value', element);
            div.appendChild(label);

        }

        if(type[0] === 'form') {

            var text = document.createElement('input');
            text.setAttribute('type', 'text');
            text.setAttribute('id', 'text_' + entity + '_' + type[1]);
            text.setAttribute('class', 'entity-element');
            text.setAttribute('value', element);
            div.appendChild(text);

        }

        if(type[0] === 'spinner') {

            var number = document.createElement('input');
            button.setAttribute('type', 'number');
            number.setAttribute('id', 'number_' + entity + '_' + type[1]);
            button.setAttribute('class', 'entity-element');
            button.setAttribute('value', element);
            div.appendChild(number);

        }
    }

    entities.appendChild(div);
}

function button(entity, element) {
        eb.send('client-to-server', '{\"' + entity + '\":\"' + element + '\"}');
};

function checkbox(entity) {

    if(document.getElementById('checkbox_' + entity).checked) {
        // TODO: add coresponding interface
    }
    else {
        // TODO: remove coresponding interface
    }
};

function send_data(id) {
    eb.send('client-to-server', document.getElementById(id).nodeValue);
};