export function getConfig() {
  return {
    appName: 'SQLForge',
    environment: process.env.NODE_ENV ?? 'development',
    port: Number(process.env.PORT ?? 3000),
    defaultSlaMs: 5000,
    defaultTargetConcurrency: 20,
    defaultTenantProfile: {
      id: 'default',
      maxConcurrency: 50,
      maxMemoryGb: 100,
      maxScanTbPerHour: 1
    }
  };
}
