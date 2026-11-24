import { useEffect, useState } from 'react';

import { getJson } from '@/api/client';
import { type ApiStatus, toApiStatus } from '@/domain/api-status';

export function useApiStatus(): ApiStatus {
  const [status, setStatus] = useState<ApiStatus>('checking');

  useEffect(() => {
    let cancelled = false;
    getJson<{ status?: string }>('/api/actuator/health')
      .then((health) => !cancelled && setStatus(toApiStatus(health)))
      .catch(() => !cancelled && setStatus('down'));
    return () => {
      cancelled = true;
    };
  }, []);

  return status;
}
