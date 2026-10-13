# Architecture analysis

Understand-Anything analyzed the Fakkerni Android/iOS source tree in English on October 13, 2026. The snapshot records the source commit in `.ua/meta.json`; later documentation or release commits may follow it.

## Coverage

- 131 of 131 selected files: 75 code/resource files, 40 configuration files, 13 documents, 2 scripts, and 1 CI workflow. Binary assets retain file-level metadata rather than invented code symbols.
- 438 nodes: 77 file nodes, 81 classes/types, 226 functions/methods, 40 configurations, 13 documents, and 1 pipeline.
- 838 relationships: 24 imports, 307 containment, 212 exports, 162 dependencies, 26 test links, 34 calls, 3 triggers, 39 configuration links, 18 documentation links, and 13 related links.
- Nine architectural layers and a 14-step guided tour. Every file-level node belongs to exactly one layer; all tour references resolve.

The [knowledge graph](../.ua/knowledge-graph.json) covers native capture screens, provider clients, local contact matching, approval, persistence, notification scheduling, shared assets, tests, and release tooling. Open it with the Understand-Anything dashboard from the project root. Its fingerprints support future incremental analysis; automatic updates are disabled in the checked-in configuration.

## Validation and limits

Deterministic validation found no missing required fields, duplicate node IDs, dangling relationships, missing layer assignments, or invalid tour references. It reported 21 isolated resource/configuration/document nodes with no recorded edges; those files remain visible, not omitted.

The merge helper misclassified Android's `SmokeInstrumentation.java` and dropped 10 test relationships. A source review verified its assertions and restored those links. Android same-package relationships are represented by source-backed dependency/call edges rather than Java import declarations, so an import-only traversal is incomplete. Unsupported text formats were read manually; binary files were inspected at metadata level.

This graph is a navigation aid and static analysis snapshot, **not runtime test evidence**. See the release notes and GitHub Actions for executed tests. Build output, dependencies, secrets, signing material, and analysis scratch files are excluded from the public analysis.
