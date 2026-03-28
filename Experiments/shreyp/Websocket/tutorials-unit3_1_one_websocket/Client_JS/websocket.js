var ws;

function connect() {
    var username = document.getElementById("username").value.trim();
    var wsserver = document.getElementById("wsserver").value.trim();
    if (!username) {
        alert("Please enter a username");
        return;
    }

        // 3️⃣ Ensure wsserver ends with "/" so URL is valid
    if (!wsserver.endsWith("/")) wsserver += "/";

    var url = wsserver + username;
//    var url = "ws://localhost:8080/chat";

    ws = new WebSocket(url);

    ws.onmessage = function(event) { // Called when client receives a message from the server
        console.log(event.data);

        // display on browser
        var log = document.getElementById("log");
        log.innerHTML += "message from server: " + event.data + "\n";
    };

    ws.onopen = function(event) { // called when connection is opened
        var log = document.getElementById("log");
        log.innerHTML += "Connected to " + event.currentTarget.url + "\n";
    };
}

function send() {  // this is how to send messages
    var content = document.getElementById("msg").value;
    ws.send(content);
}
