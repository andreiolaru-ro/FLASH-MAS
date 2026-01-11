import { connectToServer, registerHandler } from './events.js';
import {handleSidepanel, initDeployButton} from './ui.js';

handleSidepanel();
initDeployButton();

await connectToServer();

registerHandler();

