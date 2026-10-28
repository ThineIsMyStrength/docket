// Built during the Meta Global AI Developer Hackathon 2026 (26 October to 4 November) with Meta's Muse Code.
// Licence: see LICENSE of the public repository (decision D-H4).
package com.marlab.docket.musecode.route;

import com.marlab.docket.ai.remediation.RemediationProvider;
import com.marlab.docket.ai.remediation.RemediationProviderRouter;

/**
 * HM1: the endpoint pin for provider {@code META}, registered through the router's public
 * {@link RemediationProviderRouter.EndpointPolicy} extension point (D-HM1-1, satisfied by the owner's seam:
 * the router consults one policy per provider and refuses a route no policy admits).
 *
 * <p>A META route is admitted only for {@code https://api.meta.ai/v1/responses}, port 443 or the default,
 * region {@code GLOBAL}. User info, a query and a fragment are refused for every provider by
 * {@code RemediationProviderRouter} itself, whatever a policy admits, so they are not repeated here; the
 * router's built-in ANTHROPIC and GEMINI pins and its loopback-test exception are untouched (a supplied
 * policy can neither replace a built-in one nor admit a loopback endpoint).
 *
 * <p>The policy is inert without a configured META route: the router looks a policy up by the provider of a
 * route the deployment configured and granted, so a deployment with no META route behaves exactly as before
 * the hackathon, and a tenant policy naming META selects no route there.
 */
public final class ProviderEndpointPolicy implements RemediationProviderRouter.EndpointPolicy {
    /** The provider this policy pins. */
    public static final String PROVIDER = "META";
    /** The only host a META route may reach. */
    public static final String HOST = "api.meta.ai";
    /** The only path a META route may reach. */
    public static final String PATH = "/v1/responses";
    /** The only region a META route may declare. */
    public static final String REGION = "GLOBAL";

    @Override
    public String provider() {
        return PROVIDER;
    }

    @Override
    public boolean admits(RemediationProvider.Route route) {
        var endpoint = route.endpoint();
        return "https".equals(endpoint.getScheme())
                && HOST.equals(endpoint.getHost())
                && PATH.equals(endpoint.getPath())
                && (endpoint.getPort() == -1 || endpoint.getPort() == 443)
                && REGION.equals(route.region());
    }
}
