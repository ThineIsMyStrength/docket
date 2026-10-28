/**
 * Built during the Meta Global AI Developer Hackathon 2026 (26 October to 4 November) with Meta's Muse Code.
 * Licence: see LICENSE of the public repository (decision D-H4).
 *
 * <p>The root package of the hackathon module. Everything the hackathon added to Docket lives below it:
 * <ul>
 * <li>{@code route}: the {@code META} provider's endpoint policy, the Responses-API transport and the tenant
 *     route selection (prompts HM1 to HM3);
 * <li>{@code order}: the reading-order choice among engine-enumerated candidates: the candidate enumerator, the
 *     packet builder and the routed proposer (prompt HM10);
 * <li>{@code spi}: the host interfaces the module needs from the platform (rendering a region, listing a head's
 *     reorderable containers, storing a sealed proposal, resolving a policy). {@code platform-api} implements them
 *     in thin adapters under {@code com.marlab.docket.api.musecode}; this module never depends on {@code platform-api};
 * <li>{@code api}: the module's own HTTP routes (the reading-order proposal request);
 * <li>{@code measure}: test-scope measurement runs with denominators.
 * </ul>
 *
 * <p>Pre-existing platform files the hackathon had to touch (the "seams": an enum member, a provider pattern, a
 * one-line call into this module) are listed in the module's README and published as patches; nothing else of the
 * platform is published as source. The platform's own modules reach judges as compiled jars.
 */
package com.marlab.docket.musecode;
