# Local Secrets Guide

## Why secrets should not be committed

Secrets in git history are hard to remove completely.  
If keys are exposed, unauthorized users can abuse APIs and compromise the project.

## What `secrets.example.properties` is for

`secrets.example.properties` is a template file.  
It documents required key names and expected format using placeholder values only.

## What `secrets.properties` is for

`secrets.properties` stores real local development values on each developer machine.  
It is read by Gradle during local build/runtime configuration.

## Current keys used by Gradle

- `API_BASE_URL`
- `AUTH_HEADER_VALUE`

## Development and final submission key usage

- Development can use a local test key.
- Final submission must replace test key with the real key locally.
- Never hardcode or commit real key values.

## Git behavior requirement

`secrets.properties` must remain local-only and gitignored.

## Placeholder example (do not use real values)

```properties
API_BASE_URL=https://replace-with-course-server/
AUTH_HEADER_VALUE=replace_with_local_key_only
```
