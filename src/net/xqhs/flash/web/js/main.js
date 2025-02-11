import { connectToServer, registerHandlers } from './events.js';
import { handleSidepanel } from './ui.js';

handleSidepanel();

await connectToServer();

registerHandlers();