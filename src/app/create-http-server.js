import { createRouter } from './router.js';

function sendJson(response, statusCode, payload) {
  response.writeHead(statusCode, { 'content-type': 'application/json; charset=utf-8' });
  response.end(JSON.stringify(payload, null, 2));
}

async function readJsonBody(request) {
  const chunks = [];

  for await (const chunk of request) {
    chunks.push(chunk);
  }

  if (chunks.length === 0) {
    return {};
  }

  const raw = Buffer.concat(chunks).toString('utf8');
  return JSON.parse(raw);
}

export function createHttpServer(container, config) {
  const router = createRouter();

  router.register('GET', '/health', async () => ({
    statusCode: 200,
    body: {
      status: 'ok',
      service: config.appName
    }
  }));

  router.register('POST', '/api/v1/sql/assess', async ({ body }) => ({
    statusCode: 200,
    body: container.assessSql.execute(body)
  }));

  router.register('POST', '/api/v1/benchmarks/plan', async ({ body }) => ({
    statusCode: 200,
    body: container.createBenchmarkPlan.execute(body)
  }));

  router.register('POST', '/api/v1/benchmarks/analyze', async ({ body }) => ({
    statusCode: 200,
    body: container.analyzeBenchmark.execute(body)
  }));

  router.register('POST', '/api/v1/reports/sql-performance', async ({ body }) => ({
    statusCode: 200,
    body: container.generateReport.execute(body)
  }));

  router.register('POST', '/api/v1/workflows/bi-release', async ({ body }) => ({
    statusCode: 200,
    body: container.evaluateBiRelease.execute(body)
  }));

  router.register('POST', '/api/v1/workflows/plan-stability', async ({ body }) => ({
    statusCode: 200,
    body: container.analyzePlanStability.execute(body)
  }));

  router.register('POST', '/api/v1/workflows/capacity-plan', async ({ body }) => ({
    statusCode: 200,
    body: container.planPromotionCapacity.execute(body)
  }));

  return async function httpServer(request, response) {
    try {
      const url = new URL(request.url, 'http://localhost');
      const route = router.match(request.method, url.pathname);

      if (!route) {
        sendJson(response, 404, {
          error: 'Not Found',
          path: url.pathname
        });
        return;
      }

      const body = request.method === 'GET' ? {} : await readJsonBody(request);
      const result = await route({ body, query: Object.fromEntries(url.searchParams.entries()) });

      sendJson(response, result.statusCode ?? 200, result.body ?? {});
    } catch (error) {
      sendJson(response, 400, {
        error: error.message
      });
    }
  };
}
