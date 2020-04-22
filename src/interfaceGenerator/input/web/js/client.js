var eb;
var agents_number = 0;
var agents_index = {};

function init() {
    eb = new EventBus("/eventbus");

    eb.onopen = () => {
        console.log('Eventbus opened');
        eb.registerHandler('server-to-client', (error, message) => {
            var agents_info = JSON.parse(message.body);
            var header = document.getElementById('header');
            var body = document.getElementById('body');
            console.log(agents_info)
            /*
            if(agents_number === 0) {
                if(jQuery.isEmptyObject(agents_info))
                    header.innerText = 'No agents Found';
                else {
                    header.innerText = 'Agents:';
                    index = 1;
                    for(var agent_name in agents_info) {
                        agent_entry(body, agent_name, agents_info[agent_name], index);
                        index += 1;
                    }
                    agents_number = index - 1;
                }
            } else if(jQuery.isEmptyObject(agents_info)) {
                header.innerText = 'No agents Found';
                body.innerHTML = '';
            } else {
                console.log(agents_number);
                for(var index = 1; index <= agents_number; index += 1) {
                    agent_name = agents_index[index];
                    if(agents_info.hasOwnProperty(agent_name)) {
                        document.getElementById('status_' + index).innerText = agents_info[agent_name];
                        delete agents_info[agent_name];
                    }
                    else {
                        if(index == agents_number)
                            body.removeChild(document.getElementById(agent_name));
                        else {
                            document.getElementById('status_' + index).innerText = document.getElementById('status_' + agents_number).innerText;
                            document.getElementById(agents_index[agents_number]).remove();
                            document.getElementById(agent_name).children[0].innerText = 'Agent ' + agents_index[agents_number] + ' is ';
                            document.getElementById(agent_name).setAttribute('id', agents_index[agents_number]);
                            agents_index[index] = agents_index[agents_number];
                            index -= 1;
                        }
                        delete agents_index[agents_number];
                        agents_number -= 1;
                    }
                }
                index = agents_number + 1;
                for(var agent_name in agents_info) {
                    agent_entry(body, agent_name, agents_info[agent_name], index);
                    index += 1;
                }
                agents_number = index - 1;
            } */
        });
        eb.send('client-to-server', "init");
    };
    eb.onclose = () => {
        console.log('Eventbus closed')
    }
}

function agent_entry(body, agent_name, agent_status, index) {
    var div = document.createElement('div');
    div.setAttribute('id', agent_name);

    var name = document.createElement('label');
    name.innerText = 'Agent ' + agent_name + ' is ';

    var status = document.createElement('label');
    status.setAttribute('id', 'status_' + index);
    status.innerText = agent_status;

    var command = document.createElement('label');
    command.setAttribute('for', 'select_' + index);
    command.innerText = '. Command ';

    var select = document.createElement('select');
    select.setAttribute('id', 'select_' + index);

    var start = document.createElement('option');
    start.innerText = 'start';

    var stop = document.createElement('option');
    stop.innerText = 'stop';

    var send = document.createElement('input');
    send.setAttribute('type', 'button');
    send.setAttribute('value', 'send');
    send.setAttribute('onclick', 'send(' + index + ')');

    div.appendChild(name);
    div.appendChild(status);
    div.appendChild(command);
    select.appendChild(stop);
    select.appendChild(start);
    div.appendChild(select);
    div.appendChild(send);
    body.appendChild(div);
    agents_index[index] = agent_name;
}

function send(index) {
    var status = document.getElementById('status_' + index).innerText;
    var command = document.getElementById('select_' + index).value;
    if (status === 'running' && command === 'start' || status === 'stopped' && command === 'stop')
        alert('Agnet ' + agents_index[index] + ' is allready ' + status);
    else
        eb.send('client-to-server', '{\"' + agents_index[index] + '\":\"' + command + '\"}');
}

function check_for_reload() {
    document.getElementById('check_for_reload').style.display = 'none';
}

function stop() {
    eb.send('client-to-server', 'stop');
}