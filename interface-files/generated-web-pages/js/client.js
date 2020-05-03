let eb;
let values;

function timer() {

}

function init() {
    eb = new EventBus("/eventbus");

    eb.onopen = () => {
        console.log('Eventbus opened');
        eb.send('client-to-server', "init");
        eb.registerHandler('server-to-client', (error, message) => {
            // TODO: cases for active input, passive input and output
            let tokens = message.body.split(" ");

            if (tokens[0] === "active-input:") {
                // active input
                let info = message.body.slice("active-input: ".length);
                let ids = JSON.parse(info)["data"];
                let list_values = {};

                for (let i = 0; i < ids.length; i++) {
                    list_values[ids[i]] = document.getElementById(ids[i]).value
                }

                values = {"data": list_values};
                let data = JSON.stringify(values);
                eb.send('client-to-server', "active-value: " + data); // sending input value to server
            } else if (tokens[0] === 'passive-input:') {
                // passive input
                let info = message.body.slice("passive-input: ".length);
                let ids = JSON.parse(info)["data"];
                let list_values = {};

                for (let i = 0; i < ids.length; i++) {
                    list_values[ids[i]] = document.getElementById(ids[i]).value
                }

                values = {"data": list_values};
                let data = JSON.stringify(values);
                eb.send('client-to-server', "passive-value: " + data);
            } else if (tokens[0] === 'output:') {
                console.log(message.body);
                let info = message.body.slice("output: ".length);
                let ids = JSON.parse(info);
                console.log(ids);
                for (let key in ids) {
                    if (ids.hasOwnProperty(key)) {
                        let val = ids[key];
                        if (document.getElementById(key).getAttribute("type") === 'text') {
                            document.getElementById(key).value = val;
                        } else {
                            if (!isNaN(val)) {
                                document.getElementById(key).value = Number(val);
                            }
                        }
                    }
                }
            }
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