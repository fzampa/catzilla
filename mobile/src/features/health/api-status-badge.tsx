import { StyleSheet, Text } from 'react-native';

import type { ApiStatus } from '@/domain/api-status';

const labels: Record<ApiStatus, string> = {
  checking: 'Checking API…',
  up: 'API is up',
  down: 'API is unreachable',
};

export function ApiStatusBadge({ status }: { status: ApiStatus }) {
  return (
    <Text accessibilityRole="text" style={[styles.badge, styles[status]]}>
      {labels[status]}
    </Text>
  );
}

const styles = StyleSheet.create({
  badge: { paddingHorizontal: 12, paddingVertical: 6, borderRadius: 999, overflow: 'hidden' },
  checking: { backgroundColor: '#E5E7EB', color: '#374151' },
  up: { backgroundColor: '#D1FAE5', color: '#065F46' },
  down: { backgroundColor: '#FEE2E2', color: '#991B1B' },
});
