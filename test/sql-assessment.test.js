import test from 'node:test';
import assert from 'node:assert/strict';
import { QueryIntentEngine } from '../src/modules/access/domain/query-intent-engine.js';

test('QueryIntentEngine assesses high-risk BI SQL', () => {
  const engine = new QueryIntentEngine(5000);
  const result = engine.assess(
    'select * from lake.orders o join lake.users u on o.user_id = u.id order by o.create_time'
  );

  assert.equal(result.labels.scanPattern, 'full-table');
  assert.equal(result.labels.joinType, 'star');
  assert.ok(result.risks.some((risk) => risk.code === 'FULL_SCAN'));
  assert.ok(result.risks.some((risk) => risk.code === 'SELECT_STAR'));
  assert.ok(result.risks.some((risk) => risk.code === 'UNBOUNDED_SORT'));
  assert.equal(result.validations.projectionPushdown.status, 'warn');
  assert.equal(result.validations.sortNecessity.status, 'warn');
  assert.equal(result.validations.bucketJoin.status, 'opportunity');
  assert.match(result.fingerprint, /^[a-f0-9]{32}$/);
});

test('QueryIntentEngine detects repeated joins and expressions', () => {
  const engine = new QueryIntentEngine(5000);
  const result = engine.assess(
    'select sum(o.amount), sum(o.amount) from lake.orders o join lake.dim_users u1 on o.user_id = u1.id join lake.dim_users u2 on o.user_id = u2.id where lower(o.region) = \'na\' group by o.user_id'
  );

  assert.ok(result.risks.some((risk) => risk.code === 'REPEATED_TABLE_JOIN'));
  assert.ok(result.risks.some((risk) => risk.code === 'REPEATED_EXPRESSION'));
  assert.equal(result.validations.expressionReuse.status, 'warn');
  assert.equal(result.validations.repeatedJoin.status, 'warn');
  assert.equal(result.validations.predicatePushdown.status, 'warn');
});
