import { StyleSheet, Text, View } from 'react-native';

import { ApiStatusBadge } from '@/features/health/api-status-badge';
import { useApiStatus } from '@/features/health/use-api-status';

export default function HomeScreen() {
  const status = useApiStatus();

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Catzilla</Text>
      <ApiStatusBadge status={status} />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, alignItems: 'center', justifyContent: 'center', gap: 16, padding: 16 },
  title: { fontSize: 32, fontWeight: '700' },
});
