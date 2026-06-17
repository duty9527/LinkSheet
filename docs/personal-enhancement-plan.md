# Personal Enhancement Plan

This fork prioritizes a personal, rule-driven link opening flow over feature parity with upstream.

## Direction

Use `LinkEngine` as the default resolver path. Keep the legacy resolver available while behavior is compared and migrated.

## First Priorities

1. Keep the project buildable and remove obvious duplication.
2. Move new link behavior into `features/engine`.
3. Define personal rules for hosts, apps, URL cleanup, redirects, downloads, and previews.
4. Reduce features that are not useful for a personal build.
5. Rework the bottom sheet after the resolver behavior is stable.

## Near-Term Checks

- Compare legacy resolver and `LinkEngine` behavior for common links.
- Add tests for personal host/app rules before removing legacy behavior.
- Keep UI changes small until the resolver path is settled.
