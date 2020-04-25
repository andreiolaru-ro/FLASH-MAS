var eb;
var active_input_element_id;
var active_input_value;

function init() {
    eb = new EventBus("/eventbus");

    eb.onopen = () => {
        console.log('Eventbus opened');
        eb.registerHandler('server-to-client', (error, message) => {
            console.log(message.body);
        });
        eb.send('client-to-server', "init");

        if (active_input_value != null) {
            console.log(active_input_value);
        }
    };

    eb.onclose = () => {
        console.log('Eventbus closed');
    }
}

function stop() {
    eb.send('client-to-server', 'stop');
}

function send_data() {
    console.log("Sending data to server");
    active_input_value = document.getElementById("active_input").value;
    console.log(active_input_value);
    eb.send('client-to-server', active_input_value);
}