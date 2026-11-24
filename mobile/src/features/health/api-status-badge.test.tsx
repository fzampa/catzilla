import { render, screen } from '@testing-library/react-native';

import { ApiStatusBadge } from './api-status-badge';

describe('ApiStatusBadge', () => {
  it('renders the label for the given status', async () => {
    await render(<ApiStatusBadge status="up" />);
    expect(screen.getByText('API is up')).toBeOnTheScreen();
  });
});
