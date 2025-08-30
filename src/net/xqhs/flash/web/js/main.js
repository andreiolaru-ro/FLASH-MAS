import { connectToServer, registerHandler } from './events.js';
import { handleSidepanel } from './ui.js';

handleSidepanel();

await connectToServer();

registerHandler();