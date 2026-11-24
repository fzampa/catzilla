# ADR-001: Choose Expo, React Native, and TypeScript for Catzilla

- **Status:** Accepted
- **Date:** 2026-11-18

## Context

The product should reach an initial user quickly. The first version can be used on a phone through its browser while connected to the home network. This browser version is a mini-MVP and is not expected to remain the primary product for long; the intended destination is a cross-platform mobile application.

A public, search-indexed website is not required. Reusing web technologies and as much application code as practical across the browser MVP and the later mobile app is valuable.

## Decision

Build the application with **Expo, React Native, and TypeScript**. Use Expo's web support for the browser-accessible mini-MVP, and target iOS and Android from the same project as the product evolves into a mobile app.

Treat the browser build as a phone-oriented application for use on the home network, rather than as a public marketing or content site.

## Rationale

- Expo supports web and native targets in one project, making it a direct fit for a browser-first trial followed by mobile releases.
- React Native lets the long-term mobile product use native platform UI while keeping much of the application code in React and TypeScript.
- Starting with the intended mobile framework avoids making a web-only UI the foundation and later replacing it wholesale.
- The lack of public SEO requirements removes a major reason to begin with a traditional web framework.

## Consequences

- Prefer Expo and React Native APIs and libraries that support the platforms the app needs. Check web compatibility before adopting platform-specific packages.
- Expect some platform-specific implementation for browser and native behavior; shared code does not mean every component or capability works identically everywhere.
- Verify the browser experience on a real phone early, including access over the home network.
- Browser access to some device APIs may require a secure context (HTTPS), and available capabilities can differ from native builds. Validate any camera, location, notifications, or other device API requirements when they become known.
- Keep product logic and data access separable from platform-specific presentation and device integrations where that improves reuse.

## Alternatives considered

### Ionic React with Capacitor

This maximizes reuse of conventional HTML, CSS, and web UI in the native app. It is a sound choice when the web application is the durable product or when a WebView-based mobile UI is acceptable. It is not the preferred starting point here because the browser MVP is temporary and the intended long-term product is a React Native mobile app.

### Separate web application, then migrate to React Native

This can optimize the first web release, but would likely require rebuilding more of the UI when moving to mobile. There is no current SEO or desktop-web requirement to justify that additional split.

## Revisit this decision if

- The browser product becomes a long-lived, desktop-first or public website.
- The app depends on web libraries or browser capabilities that Expo web cannot support well.
- The required native experience or device integrations cannot be met reliably with Expo and its ecosystem.
