let eb;
let values;

function init() {
    eb = new EventBus("/eventbus");

    eb.onopen = () => {
        console.log('Eventbus opened');
        eb.send('client-to-server', "init");
        eb.registerHandler('server-to-client', (error, message) => {
            let ids = JSON.parse(message.body)["data"];
            console.log(ids);
            let list_values = {};

            for (let i = 0; i < ids.length; i++) {
                list_values[ids[i]] = document.getElementById(ids[i]).value
            }

            values = {"data": list_values};
            let data = JSON.stringify(values);
            console.log(data);
            eb.send('client-to-server', "active-value: " + data); // sending input value to server
        });
    };

    eb.onclose = () => {
        console.log('Eventbus closed')
    };
}

function stop() {
    eb.send('client-to-server', 'stop');
}

function send_data(id) {
    console.log("Sending data to server");
    console.log(id);
    eb.send('client-to-server', "button-id: " + id); // sending button id to server
}