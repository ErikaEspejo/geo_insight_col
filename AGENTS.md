# AGENTS.md

## Project Guidelines

This repository contains a Java 21 project developed using
Spec-Driven Development.

Always read the relevant specification and plan before implementing
or modifying functionality.

The specification is the source of truth for expected system behavior.

Do not invent requirements that are not present in the specifications.

## Development Principles

- Apply Object-Oriented Programming meaningfully.
- Follow SOLID principles pragmatically.
- Prefer simple solutions over unnecessary abstractions.
- Follow KISS and YAGNI.
- Apply DRY to meaningful conceptual duplication.
- Prefer composition over inheritance.
- Use inheritance only for legitimate "is-a" relationships.
- Use polymorphism only when behaviors genuinely vary.
- Avoid overengineering.

## Dataset-Driven Design

When behavior or modeling depends on SGC data:

- Inspect the actual dataset and its metadata first.
- Do not invent fields, constraints, classifications, or geometry types.
- Distinguish domain information from source/provider technical metadata.
- Do not silently normalize or reinterpret source data without specification support.

## Clean Code

- Use meaningful domain-oriented names.
- Keep classes cohesive and focused.
- Keep methods small and focused.
- Avoid God Classes and God Services.
- Avoid deeply nested conditionals.
- Avoid magic numbers and magic strings.
- Avoid unnecessary comments.
- Prefer self-explanatory code.
- Do not introduce abstractions without a concrete purpose.

## Java

- Use Java 21.
- Prefer immutable objects when appropriate.
- Use constructor injection when dependency injection is needed.
- Do not use field injection.
- Use records when appropriate for immutable data structures.
- Use enums for finite states.
- Avoid unnecessary setters.
- Avoid unnecessary static state.
- Avoid generic RuntimeException when a meaningful exception can be used.
- Avoid excessive null usage.

## Domain

The domain model must remain independent from technical infrastructure.

Domain classes must not depend directly on:

- Spring;
- Jackson;
- HTTP;
- JSON files;
- Leaflet;
- frontend technologies.

Domain objects should contain behavior when that behavior naturally
belongs to them.

Protect domain invariants inside the domain whenever possible.

Do not force behavior into domain objects when the behavior does not naturally belong to them.
Avoid both anemic domain models and artificial domain behavior.

## Architecture

Maintain the boundaries defined by the approved technical plan.

Do not move business logic into:

- controllers;
- repositories;
- JSON serialization;
- frontend code.

Controllers handle presentation/HTTP concerns.

Repositories handle persistence concerns.

Application services/use cases coordinate operations.

Domain objects contain domain rules and behavior.

## Persistence

Persistence is implemented using local JSON files.

Access to JSON files must remain behind repository abstractions.

Do not introduce:

- databases;
- JPA;
- Hibernate;
- Spring Data;

unless the project specification or plan is explicitly changed.

## Testing

Add or update tests whenever behavior changes.

Prioritize:

1. domain unit tests;
2. application/use-case tests;
3. integration tests where necessary.

Test behavior rather than implementation details.

Do not remove, disable, or weaken tests simply to make the build pass.

Use Mockito only when isolation is genuinely necessary.

## Scope Control

Do not:

- add unrequested features;
- introduce speculative abstractions;
- introduce external services;
- introduce infrastructure not required by the plan;
- refactor unrelated code;
- change public behavior without specification support;
- add dependencies without a concrete reason.

## Validation

Before considering an implementation complete:

- compile the project;
- run the relevant tests;
- verify the acceptance criteria;
- check for duplicated business logic;
- check class responsibilities;
- check unnecessary coupling;
- check unnecessary abstractions.

When Maven Wrapper becomes available, prefer:

./mvnw test
./mvnw verify