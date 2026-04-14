export class TenantPolicy {
  constructor(defaultProfile) {
    this.defaultProfile = defaultProfile;
    this.profiles = new Map([
      [
        'tenant-a',
        {
          id: 'tenant-a',
          maxConcurrency: 20,
          maxMemoryGb: 100,
          maxScanTbPerHour: 1
        }
      ],
      [
        'tenant-b',
        {
          id: 'tenant-b',
          maxConcurrency: 50,
          maxMemoryGb: 200,
          maxScanTbPerHour: 2
        }
      ]
    ]);
  }

  resolve(tenantId) {
    if (!tenantId) {
      return this.defaultProfile;
    }

    return this.profiles.get(tenantId) ?? {
      ...this.defaultProfile,
      id: tenantId
    };
  }
}
