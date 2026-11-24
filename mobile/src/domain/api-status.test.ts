import { toApiStatus } from './api-status';

describe('toApiStatus', () => {
  it('is up only when actuator reports UP', () => {
    expect(toApiStatus({ status: 'UP' })).toBe('up');
    expect(toApiStatus({ status: 'DOWN' })).toBe('down');
    expect(toApiStatus({})).toBe('down');
    expect(toApiStatus(undefined)).toBe('down');
  });
});
