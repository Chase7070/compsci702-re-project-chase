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
- `AUTH_HEADER_VALUE` (local bootstrap fallback; debug copies it to a local asset, and locally built
  release can also use it if `/session` is unavailable)
- `CERT_PINS` (required for release builds; `sha256/...` for base host, `host=sha256/...` for extra allowlisted hosts)
- `RELEASE_SIGNING_SHA256` (optional release signing fingerprint check)

## Development and final submission key usage

- Development/debug can use a local bootstrap key if `/session` is unavailable.
- Locally built release APKs may also use that local bootstrap value as a compatibility fallback, but
  server-issued session tokens remain the preferred long-term release path.
- Final submission must replace test key with the real key locally where required.
- Never hardcode or commit real key values.

## Git behavior requirement

`secrets.properties` must remain local-only and gitignored.

## Placeholder example (do not use real values)

```properties
API_BASE_URL=https://replace-with-course-server/
AUTH_HEADER_VALUE=replace_with_local_key_only
# CERT_PINS=sha256/replace_with_release_pin_a=,sha256/replace_with_release_pin_b=
# RELEASE_SIGNING_SHA256=replace_with_release_signing_cert_sha256
```
