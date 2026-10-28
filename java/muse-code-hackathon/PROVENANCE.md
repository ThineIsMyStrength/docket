# Provenance of every file in `muse-code-hackathon`

One row per source, test, resource and configuration file of this module, maintained by the prompt that adds or changes the file (the lead agent, at its commit). The Muse Code session export named in the last column is the record that Meta's coding agent wrote the file; it is scrubbed of keys and internal paths before it enters the evidence folder.

| File | Prompt | Muse Code session (export) | Commit | Built with |
|---|---|---|---|---|
| `pom.xml` | restructuring, 2026-10-26 | none (module skeleton prepared by the owner's agent before the first Muse Code session) | HM1's commit | Claude Code (skeleton); every later change Muse Code |
| `src/main/java/com/marlab/docket/musecode/package-info.java` | same | same | same | same |
| `src/main/java/com/marlab/docket/musecode/MuseCodeHackathonModule.java` | same | same | same | same |
| `src/main/java/com/marlab/docket/musecode/MuseCodeHackathonConfiguration.java` | skeleton, extended by HM1 | skeleton: none (see above); HM1: Muse Code session 01a17b2d-ae0d-7da1-b614-c305a2eec162 (HM1/muse-session.json) | HM1's commit | Muse Code (HM1 bean) |
| `src/main/java/com/marlab/docket/musecode/route/ProviderEndpointPolicy.java` | HM1 | Muse Code session 01a17b2d-ae0d-7da1-b614-c305a2eec162 (HM1/muse-session.json) | HM1's commit | Muse Code |
| `src/test/java/com/marlab/docket/musecode/MuseCodeHackathonModuleTest.java` | same as skeleton | same as skeleton | same | same |
| `src/test/java/com/marlab/docket/musecode/route/ProviderEndpointPolicyTest.java` | HM1 | Muse Code session 01a17b2d-ae0d-7da1-b614-c305a2eec162 (HM1/muse-session.json) | HM1's commit | Muse Code |
| `src/test/resources/com/marlab/docket/musecode/route/meta-route-fixture.yml` | HM1 | Muse Code session 01a17b2d-ae0d-7da1-b614-c305a2eec162 (HM1/muse-session.json) | HM1's commit | Muse Code |
| `README.md`, `PROVENANCE.md` | skeleton, extended by HM1 | skeleton: none (see above); HM1: Muse Code session 01a17b2d-ae0d-7da1-b614-c305a2eec162 (HM1/muse-session.json) | HM1's commit | Muse Code (HM1 status) |

Rows for `route/*` (HM2, HM3; HM7), `spi/*`, `order/*`, `api/*` and `measure/*` (HM10) are added by those prompts with the session id the Muse Code TUI shows and the export's file name. The seam hunks in pre-existing files (README, seams table) are listed here too, one row per file, with the prompt and the commit, so a judge can see every line the hackathon changed outside this module.


Public packaging prepared with OpenAI Codex on 27 October 2026. Module Java and test/resource bytes are preserved from the supplied copy. The standalone POM and public build documentation are packaging changes. Original session exports are not included in this source-only module package.
