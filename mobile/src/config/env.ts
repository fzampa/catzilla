/**
 * Runtime configuration. EXPO_PUBLIC_* variables are inlined at build time,
 * so never put secrets here.
 */
export const apiBaseUrl: string = (process.env.EXPO_PUBLIC_API_URL ?? '').replace(/\/+$/, '');
