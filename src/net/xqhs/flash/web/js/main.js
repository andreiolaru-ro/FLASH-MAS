import { connectToServer, registerHandlers } from './events.js';
import { handleSidepanel } from './ui.js';
// import { connectToServer, registerHandlers, handleSidepanel } from "./test.js";

console.log("Hello");

handleSidepanel();

await connectToServer();

registerHandlers();