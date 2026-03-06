---
description: "Lutece 8 service layer constraints: CDI scopes, injection, events, configuration"
paths:
  - "**/service/**/*.java"
---

# Service Layer — Lutece 8

## CDI Scopes

- Singleton service: `@ApplicationScoped`
- Per-request service: `@RequestScoped`
- NEVER use static `getInstance()` in new code — use `@Inject` or `CDI.current().select()`
- `getInstance()` wrapping `CDI.current().select()` is `@Deprecated(since = "8.0", forRemoval = true)` in lutece-core — remove when all callers are internal, keep temporarily with `@Deprecated` only if external callers depend on it

## Injection

- Prefer `@Inject` field injection for services
- Use `CDI.current().select(IMyService.class).get()` only in static contexts (Home classes)
- Multiple implementations: `CDI.current().select(IProvider.class).stream().filter(...)`

## Events (CDI)

- Fire: `CDI.current().getBeanManager().getEvent().fire(new MyEvent(...))`
- Observe: `public void onEvent(@Observes MyEvent event) { }`
- NEVER use deprecated `ResourceEventManager` or Spring event patterns

## Configuration

- Static properties: `AppPropertiesService.getProperty("key", "default")`
- Runtime overrides: `DatastoreService.getInstanceDataValue("key", "default")`
- Injected config: `@Inject @ConfigProperty(name = "key", defaultValue = "x")`

## Cache Integration

- Extend `AbstractCacheableService<K, V>` for cacheable services
- Invalidate on mutations: `_cache.remove(key)` or `_cache.removeAll()`
- Fire cache events via CDI on reset
