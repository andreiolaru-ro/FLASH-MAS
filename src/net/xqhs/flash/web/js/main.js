import { connectToServer, registerHandler } from './events.js';
import * as ui from './ui.js';

window.ui = ui;

ui.handleSidepanel();
ui.initDeployButton();

await connectToServer();
registerHandler();