var eb;
var data;
var entities_id = 'entities_list';
var interfaces_id = 'interfaces';
var specification = false;
var entity_elements = {};
var interface_elements = {};
var selected_entities = {};
var favoriteAgent = null;
var content = null;

function init() {
	eb = new EventBus("/eventbus");

	eb.onopen = () => {
		console.log('Eventbus opened');
		eb.registerHandler('server-to-client', (_, message) => {
			console.log('message here');
			message = JSON.parse(message.body);

			switch (message['scope']) {
				case 'global':
					switch (message['subject']) {
						case 'entities list':
							data = message['content'];
							console.log('new data:', data);

							for (var entity in data) {
								new_entity(document.getElementById(entities_id), entity);
							}

							break;
					}
					break;
			}


/*            if(!specification) {
                entities_id = data['entities'];
                interfaces_id = data['interfaces'];

                for(var element in data) {
                    var elements = element.split(' ');

                    if (data[element].toLowerCase() === 'quick send message') {
                        content = elements[4];
                        favoriteAgent = elements[5];
                    }

                    if(elements[0] === 'entity')
                        entity_elements[elements[1] + ' ' + elements[2] + ' ' + elements[3]] = data[element];

                    if(elements[0] === 'interface')
                        interface_elements[elements[1] + ' ' + elements[2] + ' ' + elements[3]] = data[element];

                    //console.log('wrong specification')
                }
                specification = true;
                console.log(interface_elements);
            }
            else {
                //console.log(data);
                var entities = document.getElementById(entities_id);
                var interfaces = document.getElementById(interfaces_id);
                var empty = jQuery.isEmptyObject(data);

                for (var index = 0; index < entities.children.length; index++) {
                    var entity = entities.children[index];
                    var checked = entity.children[0].checked;
                    var contains = data.hasOwnProperty(entity.id);

                    if (empty || !contains) {

                        if (checked)
                            remove(interfaces, entity.id);
                        entity.remove();

                    } else if (contains) {
                        for (var element in entity_elements) {

                            var type = entity_elements[element].split(' ');

                            if (type[2] === 'label') {

                                if(entity_elements[element].toLowerCase() === 'status') {

                                    document.getElementById('label_' + entity.id + '_' + type[0] + '_' + type[1] + '_' + type[2]).innerText = data[entity.id].split(' ')[1];

                                }
                            }

                            }

                            if (type[2] === 'spinner') {

                                // TODO: modify coresponding spinner for entity

                            }
                        }

                        if (checked) {

                            // TODO: modify coresponding interface
                            for (var element in interface_elements) {

                                var type = interface_elements[element].split(' ');

                                if (type[2] === 'label') {

                                    if(interface_elements[element].toLowerCase() === 'status') {

                                        document.getElementById('label_' + entity.id + '_' + type[0] + '_' + type[1] + '_' + type[2]).innerText = data[entity.id].split(' ')[1];

                                    }

                                }

                                if (type[2] === 'spinner') {

                                    // TODO: modify coresponding spinner for entity

                                }

                        }
                    }
                }
                if (empty) {
                    entities.innerText = "No entities";
                    interfaces.children[0].innerText = "No entities selected";
                } else {
                    if(Object.keys(selected_entities).length === 0)
                        interfaces.children[0].innerText = "No entities selected";
                    else
                        interfaces.children[0].innerText = "Selected entities";
                    for (var entity in data) {
                        if (document.getElementById(entity) === null) {
                            if(entities.children.length === 0)
                                entities.innerText = "Entities";
                            new_entity(entities, entity);
                        }
                    }
                }
            }
*/        });
		eb.send('client-to-server', 'init');
		eb.registerHandler('server-to-client-agent-message', (error, message) => {
			var messages = JSON.parse(message.body);
			for (var agent in messages) {
				messages[agent] = JSON.parse(messages[agent]);
				var n = 3;
				for (var element in entity_elements) {
					var type = element.split(' ');
					if (type[0] === 'message-agent' && type[1] === 'show-messages') {
						document.getElementById('entity_' + agent + '_' + type[0] + '_' + type[1] + '_' + type[2]).innerText = messages[agent]['agent'] + ':' + messages[agent]['content'];
						document.getElementById(agent).children[n].innerText = 'Last message from';
						eb.send('client-to-server-agent-message', agent);
					}
					if (type[2] === 'button')
						n += 1;
					else
						n += 2;
				}
				n = 2;
				if (document.getElementById(agent).children[0].checked) {
					console.log(messages);
					for (var element in interface_elements) {
						var type = element.split(' ');
						if (type[0] === 'message-agent' && type[1] === 'show-messages') {
							document.getElementById('interface_' + agent + '_' + type[0] + '_' + type[1] + '_' + type[2]).innerText = messages[agent]['agent'] + ': ' + messages[agent]['content'];
							document.getElementById('interface_' + agent).children[n].innerText = 'Last message from';
							eb.send('client-to-server-agent-message', agent);
						}
						if (type[2] === 'button')
							n += 2;
						else
							n += 3;
					}
				}
			}
		});
	}
	eb.onclose = () => {
		console.log('Eventbus closed')
	}
};

function new_entity(entities, entity) {
	//	var info = data[entity_id].split(' ');
	var div = document.createElement('div');
	div.setAttribute('class', 'entity');
	div.setAttribute('id', entity);


	var checkbox = document.createElement('input');
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

	//	for (var element in entity_elements) {
	//		var type = element.split(' ');
	//
	//		if ((type[0] === 'message-agent' || type[0] === 'quick-send') && info[0] !== 'agent')
	//			continue;
	//
	//		if (type[2] === 'button') {
	//
	//			var button = document.createElement('input');
	//			button.setAttribute('type', 'button');
	//			button.setAttribute('class', 'entity-element');
	//			button.setAttribute('onclick', 'button(\'' + entity_id + '\', \'' + element + '\')');
	//			button.setAttribute('value', entity_elements[element]);
	//			div.appendChild(button);
	//
	//		}
	//
	//		if (type[2] === 'label') {
	//
	//			var label = document.createElement('label');
	//			label.setAttribute('id', 'entity_' + entity_id + '_' + type[0] + '_' + type[1] + '_' + type[2]);
	//			label.setAttribute('class', 'entity-element');
	//
	//			if (entity_elements[element].toLowerCase() === 'type')
	//				label.innerText = info[0];
	//
	//			if (entity_elements[element].toLowerCase() === 'status')
	//				label.innerText = info[1];
	//
	//			// TODO: implement other label options
	//
	//			var description = document.createElement('label');
	//			description.setAttribute('for', 'entity_' + entity_id + '_' + type[0] + '_' + type[1] + '_' + type[2]);
	//			if (entity_elements[element].toLowerCase() === 'messages') {
	//				description.innerText = 'No';
	//				label.innerText = entity_elements[element].toLowerCase();
	//			}
	//			else
	//				description.innerText = entity_elements[element];
	//
	//			div.appendChild(description);
	//			div.appendChild(label);
	//
	//		}
	//
	//		if (type[2] === 'form') {
	//
	//			var text = document.createElement('input');
	//			text.setAttribute('type', 'text');
	//			text.setAttribute('id', 'entity_' + entity_id + '_' + type[0] + '_' + type[1] + '_' + type[2]);
	//			text.setAttribute('class', 'entity-element');
	//
	//			var description = document.createElement('label');
	//			description.setAttribute('for', 'entity_' + entity_id + '_' + type[0] + '_' + type[1] + '_' + type[2]);
	//			description.innerText = entity_elements[element];
	//
	//			div.appendChild(description);
	//			div.appendChild(text);
	//
	//		}
	//
	//		if (type[2] === 'spinner') {
	//
	//			var number = document.createElement('input');
	//			number.setAttribute('type', 'number');
	//			number.setAttribute('id', 'entity_' + entity_id + '_' + type[0] + '_' + type[1] + '_' + type[2]);
	//			number.setAttribute('class', 'entity-element');
	//
	//			// TODO: implement number options
	//
	//			var description = document.createElement('label');
	//			description.setAttribute('for', 'entity_' + entity_id + '_' + type[0] + '_' + type[1] + '_' + type[2]);
	//			description.innerText = entity_elements[element];
	//
	//			div.appendChild(description);
	//			div.appendChild(number);
	//
	//		}
	//
	//		if (type[2] === 'list') {
	//
	//			var select = document.createElement('select');
	//			select.setAttribute('id', 'entity_' + entity_id + '_' + type[0] + '_' + type[1] + '_' + type[2]);
	//
	//			if (entity_elements[element].toLowerCase() === 'operations') {
	//
	//				if (info[0] === 'node') {
	//
	//					var start = document.createElement('option');
	//					start.innerText = 'start';
	//					select.appendChild(start);
	//
	//					var stop = document.createElement('option');
	//					stop.innerText = 'stop';
	//					select.appendChild(stop);
	//
	//				}
	//				else {
	//					var operations = JSON.parse(info[2]);
	//
	//					for (var operation in operations) {
	//
	//						var option = document.createElement('select');
	//						option.innerText = operations[operation]['name'].replace('_', ' ');
	//						select.appendChild(option);
	//
	//					}
	//
	//				}
	//			}
	//
	//			// TODO: implement other list options
	//
	//			var description = document.createElement('label');
	//			description.setAttribute('for', 'entity_' + entity_id + '_' + type[0] + '_' + type[1] + '_' + type[2]);
	//			description.innerText = entity_elements[element];
	//
	//			div.appendChild(description);
	//			div.appendChild(select);
	//		}
	//	}

	//	if (info[0] === 'agent')
	//		entities.insertBefore(div, entities.children[0]);
	//	else
	entities.appendChild(div);
}

function new_interface(interfaces, entity) {
	var index = Object.keys(selected_entities).length;

	var div = document.createElement('div');
	div.setAttribute('class', 'interface');
	div.setAttribute('id', 'interface_' + entity);

	var info = data[entity];

	for (var idx in info['gui']['children']) {
		var element = info['gui']['children'][idx]
		var item = null
		console.log("rendering: " + element)

		switch (element['type']) {
			case 'label':
				item = document.createElement('label');
				item.setAttribute('id', element['id']);
				item.innerText = element['value'];
				break;
			case 'button':
				item = document.createElement('input');
				item.setAttribute('id', element['id']);
				item.setAttribute('type', 'button');
				item.setAttribute('value', element['value']);
				break;
			default:
				console.log("Unknown element type: ", element['type']);
		}
		div.appendChild(item);
		div.appendChild(document.createElement('br'));
	}

	//	for (var element in interface_elements) {
	//		var type = element.split(' ');
	//
	//		if ((type[0] === 'message-agent' || type[0] === 'quick-send') && info[0] !== 'agent')
	//			continue;
	//
	//		if (type[2] === 'button') {
	//
	//			var button = document.createElement('input');
	//			button.setAttribute('type', 'button');
	//			button.setAttribute('class', 'entity-element');
	//			button.setAttribute('onclick', 'button_interface(\'' + entity + '\', \'' + element + '\')');
	//			button.setAttribute('value', interface_elements[element]);
	//			div.appendChild(button);
	//
	//		}
	//
	//		if (type[2] === 'label') {
	//
	//			var label = document.createElement('label');
	//			label.setAttribute('id', 'interface_' + entity + '_' + type[0] + '_' + type[1] + '_' + type[2]);
	//			label.setAttribute('class', 'entity-element');
	//
	//			if (interface_elements[element].toLowerCase() === 'type')
	//				label.innerText = info[0];
	//
	//			if (interface_elements[element].toLowerCase() === 'status')
	//				label.innerText = info[1];
	//
	//			// TODO: implement other label options
	//
	//			var description = document.createElement('label');
	//			description.setAttribute('for', 'interface_' + entity + '_' + type[0] + '_' + type[1] + '_' + type[2]);
	//			if (interface_elements[element].toLowerCase() === 'messages') {
	//				description.innerText = 'No';
	//				label.innerText = interface_elements[element].toLowerCase();
	//			}
	//			else
	//				description.innerText = interface_elements[element];
	//
	//			div.appendChild(description);
	//			div.appendChild(label);
	//
	//		}
	//
	//		if (type[2] === 'form') {
	//
	//			var text = document.createElement('input');
	//			text.setAttribute('type', 'text');
	//			text.setAttribute('id', 'interface_' + entity + '_' + type[0] + '_' + type[1] + '_' + type[2]);
	//			text.setAttribute('class', 'entity-element');
	//
	//			var description = document.createElement('label');
	//			description.setAttribute('for', 'interface_' + entity + '_' + type[0] + '_' + type[1] + '_' + type[2]);
	//			description.innerText = interface_elements[element];
	//
	//			div.appendChild(description);
	//			div.appendChild(text);
	//
	//		}
	//
	//		if (type[2] === 'spinner') {
	//
	//			var number = document.createElement('input');
	//			number.setAttribute('type', 'number');
	//			number.setAttribute('id', 'interface_' + entity + '_' + type[0] + '_' + type[1] + '_' + type[2]);
	//			number.setAttribute('class', 'entity-element');
	//
	//			// TODO: implement number options
	//
	//			var description = document.createElement('label');
	//			description.setAttribute('for', 'interface_' + entity + '_' + type[0] + '_' + type[1] + '_' + type[2]);
	//			description.innerText = interface_elements[element];
	//
	//			div.appendChild(description);
	//			div.appendChild(number);
	//
	//		}
	//
	//		if (type[2] === 'list') {
	//
	//			var select = document.createElement('select');
	//			select.setAttribute('id', 'interface_' + entity + '_' + type[0] + '_' + type[1] + '_' + type[2]);
	//
	//			if (interface_elements[element].toLowerCase() === 'operations') {
	//
	//				if (info[0] === 'node') {
	//
	//					var start = document.createElement('option');
	//					start.innerText = 'start';
	//					select.appendChild(start);
	//
	//					var stop = document.createElement('option');
	//					stop.innerText = 'stop';
	//					select.appendChild(stop);
	//
	//				}
	//				else {
	//					var operations = JSON.parse(info[2]);
	//
	//					for (var operation in operations) {
	//						var option = document.createElement('option');
	//						option.innerText = operations[operation]['name'].replace('_', ' ');
	//						select.appendChild(option);
	//
	//					}
	//
	//				}
	//			}
	//
	//			// TODO: implement other list options
	//
	//			var description = document.createElement('label');
	//			description.setAttribute('for', 'interface_' + entity + '_' + type[0] + '_' + type[1] + '_' + type[2]);
	//			description.innerText = interface_elements[element];
	//
	//			div.appendChild(description);
	//			div.appendChild(select);
	//		}
	//
	//		div.appendChild(document.createElement('br'));
	//	}

	if (index % 3 === 0) {
		var line = document.createElement('div');

		line.setAttribute('class', 'line');
		line.appendChild(div);
		interfaces.appendChild(line);
	}
	else
		interfaces.children[1 + (index - (index % 3)) / 3].appendChild(div)

	selected_entities[entity] = index;
}

function remove(interfaces, entity) {
	var index = selected_entities[entity];
	var interface = index % 3;
	var line = 1 + (index - (interface)) / 3;

	delete selected_entities[entity];
	interfaces.children[line].children[interface].remove();

	for (line; line < interfaces.children.length; line++) {

		if (line === interfaces.children.length - 1) {
			if (interfaces.children[line].children.length === 0) {
				interfaces.children[line].remove();
				break;
			}
		}
		else
			interfaces.children[line].appendChild(interfaces.children[line + 1].children[0]);

		for (interface; interface < interfaces.children[line].children.length; interface++)
			selected_entities[interfaces.children[line].children[interface].id.substring(10)] -= 1;
		interface = 0;
	}

	if (Object.keys(selected_entities).length === 0)
		interfaces.children[0].innerText = 'No selected entities'

}

function checkbox(entity) {
	var interfaces = document.getElementById(interfaces_id);

	if (document.getElementById('checkbox_' + entity).checked) {

		if (Object.keys(selected_entities).length === 0)
			interfaces.children[0].innerText = 'Selected entities';
		new_interface(interfaces, entity);
	}
	else
		remove(interfaces, entity);

	//console.log(selected_entities);
};

function button(entity, element) {
	var operations = {};
	var port = element.split(' ')[0];
	var select = 'select';
	var text = 'text';
	var number = 'number';

	for (var entity_element in entity_elements) {
		var type = entity_element.split(' ');

		if (type[0] === port) {
			if (type[2] === 'list')
				select += ' ' + document.getElementById('entity_' + entity + '_' + type[0] + '_' + type[1] + '_' + type[2]).value;
			if (type[2] === 'form')
				text += ' ' + document.getElementById('entity_' + entity + '_' + type[0] + '_' + type[1] + '_' + type[2]).value;
			if (type[2] === 'spinner')
				number += ' ' + document.getElementById('entity_' + entity + '_' + type[0] + '_' + type[1] + '_' + type[2]).value;
		}
	}

	if (entity_elements[element].toLowerCase() === 'send') {
		var input = { 'type': 'message' };
		input['content_destination'] = text;
		operations[entity] = input;
		eb.send('client-to-server', JSON.stringify(operations));
	}
	else if (entity_elements[element].toLowerCase() === 'quick send message') {
		var input = { 'type': 'message' };
		input['content_destination'] = text + ' ' + content + ' ' + favoriteAgent;
		operations[entity] = input;
		if (favoriteAgent === entity)
			alert('Favourite agent is ' + favoriteAgent + ' and can not send a message to himself');
		else
			eb.send('client-to-server', JSON.stringify(operations));
	}
	else if (entity_elements[element].toLowerCase() === 'execute') {
		var input = { 'type': 'operation' };
		input['name'] = select;
		input['parameters'] = text;
		operations[entity] = input;
		eb.send('client-to-server', JSON.stringify(operations));
	}
	else if (entity_elements[element].toLowerCase() === 'start' || entity_elements[element].toLowerCase() === 'stop') {
		var input = { 'type': 'operation' };
		input['name'] = select + ' ' + entity_elements[element].toLowerCase();
		input['parameters'] = text;
		operations[entity] = input;
		eb.send('client-to-server', JSON.stringify(operations));
	}
	else {
		//TODO: implementation for other input options
	}
};

function button_interface(entity, element) {
	var operations = {};
	var port = element.split(' ')[0];
	var select = 'select';
	var text = 'text';
	var number = 'number';

	for (var interface_element in interface_elements) {
		var type = interface_element.split(' ');

		if (type[0] === port) {
			if (type[2] === 'list')
				select += ' ' + document.getElementById('interface_' + entity + '_' + type[0] + '_' + type[1] + '_' + type[2]).value;
			if (type[2] === 'form')
				text += ' ' + document.getElementById('interface_' + entity + '_' + type[0] + '_' + type[1] + '_' + type[2]).value;
			if (type[2] === 'spinner')
				number += ' ' + document.getElementById('interface_' + entity + '_' + type[0] + '_' + type[1] + '_' + type[2]).value;
		}
	}

	if (interface_elements[element].toLowerCase() === 'send') {
		var input = { 'type': 'message' };
		input['content_destination'] = text;
		operations[entity] = input;
		var message = text.split(" ");
		var n = message.length;
		if (message[n - 1] === entity)
			alert('An agent can not send a message to himself');
		else if (n > 2)
			eb.send('client-to-server', JSON.stringify(operations));
	}
	else if (interface_elements[element].toLowerCase() === 'quick send message') {
		var input = { 'type': 'message' };
		input['content_destination'] = text + ' ' + content + ' ' + favoriteAgent;
		operations[entity] = input;
		if (favoriteAgent === entity)
			alert('Favourite agent is ' + favoriteAgent + ' and can not send a message to himself');
		else
			eb.send('client-to-server', JSON.stringify(operations));
	}
	else if (interface_elements[element].toLowerCase() === 'execute') {
		var input = { 'type': 'operation' };
		input['name'] = select;
		input['parameters'] = text;
		operations[entity] = input;
		eb.send('client-to-server', JSON.stringify(operations));
	}
	else if (interface_elements[element].toLowerCase() === 'start' || interface_elements[element].toLowerCase() === 'stop') {
		var input = { 'type': 'operation' };
		input['name'] = select + ' ' + interface_elements[element].toLowerCase();
		input['parameters'] = text;
		operations[entity] = input;
		eb.send('client-to-server', JSON.stringify(operations));
	}
	else {
		//TODO: implementation for other input options
	}
};

function send_data(id) {
	var operations = {};
	var input = { 'type': 'operation' };
	input['name'] = 'operation' + ' ' + document.getElementById(id).innerText.toLowerCase().replace(' ', '_');
	input['parameters'] = 'parameters';
	operations['all'] = input;
	eb.send('client-to-server', JSON.stringify(operations));
};

