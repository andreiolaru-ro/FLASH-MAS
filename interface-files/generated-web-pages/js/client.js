var eb;
var agents_index = {};
var active_input_value;
var value;

function init() {
    eb = new EventBus("/eventbus");

    eb.onopen = () => {
        console.log('Eventbus opened');
        eb.registerHandler('server-to-client', (error, message) => {
            console.log("id input from server " + message.body);
            value = document.getElementById(message.body).value;
            // console.log(value);
        });
        eb.send('client-to-server', "init");
    };

    eb.onclose = () => {
        console.log('Eventbus closed')
    }
}

function stop() {
    eb.send('client-to-server', 'stop');
}

function send_data(id) {
    console.log("Sending data to server");
    console.log(id);
    eb.send('client-to-server', "button-id: " + id); // sending button id to server
    // receiving form id

    /*
    eb.registerHandler('server-to-client', (error, message) => {
        console.log("id input from server " + message.body);
        value = document.getElementById(message.body).value;
        console.log(value);

    });
    */
    eb.send('client-to-server', "active-value: " + value);
}