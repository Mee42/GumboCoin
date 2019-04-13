import RSocketClient from 'rsocket-core'
// import JsonSerializers from 'rsocket-core'
// import RSocketWebSocketClient from 'rsocket-websocket-client';

// Create an instance of a client
// const client = new RSocketClient({
//     // send/receive objects instead of strings/buffers
//     serializers: JsonSerializers,
//     setup: {
//         // ms btw sending keepalive to server
//         keepAlive: 60000,
//         // ms timeout if no keepalive response
//         lifetime: 180000,
//         // format of `data`
//         dataMimeType: 'application/json',
//         // format of `metadata`
//         metadataMimeType: 'application/json',
//     },
//     transport: new RSocketWebSocketClient({uri: 'wss://...'}),
// });


function sleep(ms){
    let date = new Date();
    let curDate = null;
    do { curDate = new Date(); }
    while(curDate-date < ms);
}