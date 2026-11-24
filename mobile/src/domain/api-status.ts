export type ApiStatus = 'checking' | 'up' | 'down';

/** Maps a Spring Boot Actuator health payload to the status shown to the user. */
export function toApiStatus(
  health: { status?: string } | undefined,
): Exclude<ApiStatus, 'checking'> {
  return health?.status === 'UP' ? 'up' : 'down';
}
