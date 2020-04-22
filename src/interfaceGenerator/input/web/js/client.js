var eb;
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

    eb.send('client-to-server', "plm");
}

function check_for_reload() {
    document.getElementById('check_for_reload').style.display = 'none';
}

function stop() {
    eb.send('client-to-server', 'stop');
}

function send_data(data) {

}