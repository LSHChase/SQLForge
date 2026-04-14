export class InMemoryBaselineRepository {
  constructor() {
    this.records = new Map();
  }

  save(record) {
    this.records.set(record.fingerprint, {
      ...record,
      updatedAt: new Date().toISOString()
    });
  }

  get(fingerprint) {
    return this.records.get(fingerprint) ?? null;
  }
}
