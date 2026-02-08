## Project Overview

This is a Spring Boot project for a simple SNS (Social Networking Service) like Twitter.
It is a web application where users can post messages and see a timeline of posts.

## Important Note for AI Agent

As the AI Agent, you **must not** perform any actions or make any changes to the codebase that have not been explicitly instructed by the user. Adhere strictly to the user's commands and avoid proactive modifications or additions.

## Coding Rules

### General Rules
- Do not touch git for anything other than checking status.
- Do not use "@RequestMapping" and write out the full api endpoint.
- DTOs should be written as records.
- @ConfigurationProperties classes should be written as records.

### Database Rules
- Do not configure Foreign Key (FK) constraints in the database. Use only @JoinColumn without FK constraints.

### Transaction Rules
- Use @Transactional only when absolutely necessary. Do not use it as a convention:
  - Use when multiple write operations must be executed in a single transaction
  - Use when Dirty Checking is needed (entity modification without explicit save)
  - Do NOT use for single Repository operations (save, delete, find, etc. - they handle transactions automatically)
  - Do NOT use for simple read operations

### API Shell Script Rules
- When creating a new API, create a corresponding shell script file in `src/main/resources/http/` directory:
  - File naming: Use lowercase with the API resource name (e.g., `post.sh`, `follow.sh`, `repost.sh`)
  - Include curl commands for all API endpoints (POST, GET, PUT, DELETE)
  - Use `BASE_URL="http://localhost:8080"` variable
  - Use `-b cookies.txt` for authenticated requests
  - Add descriptive echo statements before each curl command
  - Format: `#!/bin/bash` shebang, then BASE_URL, then curl commands with proper formatting
