# Docket Meta hackathon module

This source snapshot contains the HM1 META endpoint policy and its tests. Later transport, tenant selection and reading-order prompts are not implemented in this snapshot.

The surrounding Docket platform was built before the hackathon and is distributed only as compiled JARs and binary Docker images. Platform extension points are included in those binaries.

Build and test from the repository root with `pwsh ./build.ps1`; see the root README for prerequisites and Docker commands. The standalone Maven model resolves platform modules from `../../libs` and third-party libraries from Maven Central. No platform sources or private parent POM are required.

The original source authorship is recorded in PROVENANCE.md. Public packaging, the standalone POM and build tools were prepared with OpenAI Codex on 27 October 2026.
