import { createServer } from 'node:http';
import { createHttpServer } from './app/create-http-server.js';
import { createContainer } from './bootstrap/create-container.js';
import { getConfig } from './config/index.js';

const config = getConfig();
const container = createContainer(config);
const app = createHttpServer(container, config);

const server = createServer(app);

server.listen(config.port, () => {
  console.log(`SQLForge listening on http://localhost:${config.port}`);
});
