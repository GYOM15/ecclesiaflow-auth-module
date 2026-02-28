# Commit Convention

This project follows standard GitHub commit message conventions.

## Format

```
<subject line>

<optional body>
```

- **Subject line**: imperative mood, English, capitalize first word, max 72 characters
- **Body** (optional): separated by a blank line, explains *why* the change was made

## Good Subject Lines

Start with a verb in the imperative:

```
Add initial password setup endpoint
Fix error message on invalid setup token
Remove dead email infrastructure and WebClient
Update CI workflow to JDK 21
Configure JaCoCo exclusions for generated code
Clean up unused imports
```

## Multi-line Example

```
Integrate Keycloak with compensation and retry

Replace direct JWT generation with Keycloak Admin API.
Add Spring Retry for distributed transaction resilience
and compensation logic on partial failures.
```

## Rules

1. One logical change per commit
2. `mvn verify` must pass before committing
3. No secrets or credentials in commit content
