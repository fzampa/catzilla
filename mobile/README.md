# catzilla-mobile

The Catzilla app, where people track their cats' health, habits, weight and feeding times together with everyone in their Home (see the [product vision](../docs/product-vision.md)).

Built with Expo + React Native + TypeScript. Web (on a phone, over the home network) is the first target; iOS and Android follow from the same code.

## Run

```bash
npm install
cp .env.example .env    # optional: point EXPO_PUBLIC_API_URL at a running API
npm run web             # or: npm run ios / npm run android
```

## Checks

```bash
npm run check           # typecheck, lint, format check, tests
npm run build:web       # static web export to dist/ (served by infra/)
npm run doctor          # Expo dependency and config checks
```

See [AGENTS.md](AGENTS.md) for the code layout and conventions.
