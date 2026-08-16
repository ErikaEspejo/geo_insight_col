<!--
Sync Impact Report
- Version change: 1.0.0 -> 1.0.1
- Modified principles: I. Specification-Driven Development First (clarified the Spec Kit artifact location)
- Added sections: none
- Removed sections: none
- Deferred TODOs: none
- Migration impact: approved feature specifications remain in `specs/<feature>/`; no artifact copy or code migration is required
-->

# GeoInsight Colombia Constitution

## Core Principles

### I. Specification-Driven Development First

Specifications are the source of truth for expected system behavior.
No implementation MAY precede an approved specification. Ambiguous or
missing business rules MUST be resolved in the specification, never
invented in code. A specification is approved only when it has passed
the Spec Kit workflow and is recorded in `specs/<feature>/`. The
`.specify/memory` directory is reserved for project-wide governance memory,
including this constitution, and MUST NOT duplicate feature specifications.

Rationale: This guarantees that code, plans, and tests describe the
same agreed behavior, and that undefined business decisions surface as
specification questions instead of silent implementation choices.

### II. Meaningful Object-Oriented Design

Encapsulation, abstraction, inheritance, polymorphism, and composition
MUST be applied only when they represent real domain needs. Prefer
composition over inheritance. Do NOT introduce artificial hierarchies,
superfluous abstractions, or generic patterns solely to demonstrate OOP
concepts.

Rationale: Object-oriented techniques are tools for expressing genuine
domain structure, not ends in themselves. Forced abstractions increase
complexity and obscure the actual behavior.

### III. Dataset-Driven Domain Modeling

Domain concepts, attributes, constraints, classifications, and geometry
rules MUST be derived from the actual SGC datasets and their metadata.
Do NOT invent mandatory fields, enum values, relationships, or geometry
assumptions without evidence. Distinguish domain information from
provider-specific technical metadata. Partial samples MUST NOT be
generalized into universal rules.

Rationale: GeoInsight represents real geoscientific records. Models that
contradict the observed data misrepresent the domain and produce wrong
behavior at runtime.

### IV. Domain Independence

The domain model MUST remain independent from infrastructure and
presentation technologies. Domain classes MUST NOT depend directly on
Spring, Jackson, HTTP, JSON files, Leaflet, or frontend technologies.
Business rules MUST NOT live only in controllers, repositories,
serializers, or frontend code.

Rationale: Keeping the domain free of technical coupling preserves its
meaning, testability, and stability regardless of the surrounding
infrastructure.

### V. Scientific Correctness

GeoInsight provides descriptive geoscientific exploration and
characterization. It MUST NOT infer risk, hazard, safety, causality, or
prediction unless a future approved specification defines a scientifically
justified methodology. Geographic calculations MUST respect CRS,
coordinate ordering, geometry semantics, and appropriate units.

Rationale: Describing what is recorded is different from judging what it
means. Unsupported inference is scientifically misleading and outside the
agreed scope of the system.

### VI. Simplicity and Scope Discipline

Prefer the simplest design that satisfies the current specification.
Follow KISS and YAGNI. Avoid speculative abstractions, unnecessary
dependencies, premature generalization, and overengineering.

Rationale: Each unneeded concept, dependency, or abstraction is ongoing
cost with no compensating behavior. Simplicity keeps the system
understandable and changeable.

### VII. Contract-Based Testing

Tests MUST verify specified behavior and contracts. Domain rules and
invariants MUST be covered by automated tests. Tests MUST NOT be
weakened, removed, or disabled simply to make the build pass.

Rationale: The test suite is the executable record of the specification.
Weakening tests to satisfy the build breaks the traceability chain and
erodes confidence in the behavior.

### VIII. Traceability

Maintain traceability from specification to requirements, implementation,
and tests. If implementation work reveals an undefined business decision,
MUST return to the specification before proceeding.

Rationale: Traceability proves that delivered behavior is specified
behavior. Decisions discovered during implementation are specification
matters and must be resolved there.

## Additional Constraints

- Persistence is implemented with local JSON files; access to JSON files
  MUST remain behind repository abstractions.
- Do NOT introduce databases, JPA, Hibernate, or Spring Data unless the
  project specification or plan is explicitly changed.
- Use Java 21. Prefer immutable objects, records, enums, and constructor
  injection where appropriate. Avoid field injection and unnecessary
  static state.
- SGC records are immutable reference data: they MUST NOT be edited or
  deleted from GeoInsight. Records created in GeoInsight (`GEOINSIGHT`)
  are editable and deletable only by the administrator, and MUST NOT be
  presented as official SGC information.
- The system MUST NOT calculate or communicate risk, hazard,
  vulnerability, danger, occurrence probability, safety, or predictive
  recommendations.

## Development Workflow

1. Inspect the relevant specification and constitution before starting.
2. Derive requirements and data rules from the actual datasets and
   metadata before finalizing contracts.
3. Produce a plan from the approved specification; the plan MUST NOT add
   behavior absent from the specification.
4. Implement against the plan, keeping business logic out of controllers,
   repositories, serializers, and frontend code.
5. Write or update tests covering specified behavior and invariants.
6. Validate by compiling the project, running the relevant tests, and
   verifying the acceptance criteria against the specification.
7. If any undefined business decision is discovered, return to the
   specification and do not invent a resolution in code.

## Governance

This constitution is the supreme governance document of the GeoInsight
Colombia project. All specifications, plans, implementations, and reviews
MUST be checked against this constitution.

- Compliance review: every specification and plan MUST be checked against
  this constitution before approval. Any violation MUST be explicitly
  justified in writing, and the justification MUST be recorded with the
  artifact.
- Complexity justification: every abstraction, dependency, and design
  decision beyond the simplest sufficient option MUST carry an explicit
  reason tied to a specification need.
- Amendments: changes to these principles or governance rules require an
  explicit constitution amendment. An amendment MUST document the change,
  its rationale, and a migration impact note, and MUST be ratified before
  it takes effect.
- Versioning policy: the constitution version follows Semantic Versioning
  (MAJOR.MINOR.PATCH). MAJOR covers backward-incompatible principle
  removals or redefinitions; MINOR covers added principles or materially
  expanded guidance; PATCH covers clarifications and wording refinements.
- Source of truth: in case of conflict, this constitution takes precedence
  over runtime development guidance and working files, which MUST NOT
  contradict it.

**Version**: 1.0.1 | **Ratified**: 2026-08-15 | **Last Amended**: 2026-08-16
