import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { renderToString } from 'react-dom/server';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { NotFoundPage } from '../NotFoundPage';

// Mock dependencies used by Navbar and Footer
vi.mock('../../features/auth/hooks/useAuth', () => ({
  useAuth: () => ({
    isAuthenticated: false,
    user: null,
    userDocument: null,
    loading: false,
    logout: vi.fn(),
  }),
}));

vi.mock('../../hooks/useNotifications', () => ({
  useUnreadNotificationCount: () => ({
    data: { count: 0 },
    isLoading: false,
  }),
}));

describe('NotFoundPage Component', () => {
  it('renders the 404 status code, heading, and description', () => {
    const html = renderToString(
      <MemoryRouter initialEntries={['/random-unmatched-path']}>
        <NotFoundPage />
      </MemoryRouter>
    );

    expect(html).toContain('404');
    expect(html).toContain('Page Not Found');
    expect(html).toContain('The page or resource you are looking for does not exist');
  });

  it('renders navigation links to Home ("/") and Dashboard ("/dashboard")', () => {
    const html = renderToString(
      <MemoryRouter initialEntries={['/missing-page']}>
        <NotFoundPage />
      </MemoryRouter>
    );

    expect(html).toContain('href="/"');
    expect(html).toContain('Return to Home');
    expect(html).toContain('href="/dashboard"');
    expect(html).toContain('Go to Dashboard');
  });

  it('correctly catches unmatched routes in a React Router Route hierarchy', () => {
    const html = renderToString(
      <MemoryRouter initialEntries={['/unknown/sub/path']}>
        <Routes>
          <Route path="/" element={<div>Home Page</div>} />
          <Route path="/dashboard" element={<div>Dashboard Page</div>} />
          <Route path="*" element={<NotFoundPage />} />
        </Routes>
      </MemoryRouter>
    );

    expect(html).toContain('Page Not Found');
    expect(html).not.toContain('Home Page');
    expect(html).not.toContain('Dashboard Page');
  });

  it('does not render 404 when matching a defined route', () => {
    const html = renderToString(
      <MemoryRouter initialEntries={['/dashboard']}>
        <Routes>
          <Route path="/" element={<div>Home Page</div>} />
          <Route path="/dashboard" element={<div>Dashboard Page</div>} />
          <Route path="*" element={<NotFoundPage />} />
        </Routes>
      </MemoryRouter>
    );

    expect(html).toContain('Dashboard Page');
    expect(html).not.toContain('Page Not Found');
  });
});

