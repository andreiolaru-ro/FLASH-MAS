import { connectToServer, registerHandler } from './events.js';
import * as ui from './ui.js';
import { initVisualizations } from './visualizations.js';

window.ui = ui;

ui.handleSidepanel();
ui.initDeployButton();

await connectToServer();
registerHandler();

initVisualizations();