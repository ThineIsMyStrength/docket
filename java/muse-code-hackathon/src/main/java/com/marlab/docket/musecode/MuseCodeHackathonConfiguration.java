// Built during the Meta Global AI Developer Hackathon 2026 (26 October to 4 November) with Meta's Muse Code.
// Licence: see LICENSE of the public repository (decision D-H4).
package com.marlab.docket.musecode;

import com.marlab.docket.ai.remediation.RemediationProviderRouter;
import com.marlab.docket.musecode.route.ProviderEndpointPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The module's Spring configuration. {@code PlatformApplication} scans {@code com.marlab.docket}, so this class is
 * found without any change to the platform's application class. Today it contributes only the charter bean; each
 * hackathon feature adds its own beans here behind its own gate (a configured route, a tenant grant, or an explicit
 * property that defaults to off), so that a deployment with no META route and no flag behaves exactly as before the
 * hackathon.
 */
@Configuration(proxyBeanMethods = false)
public class MuseCodeHackathonConfiguration {
    @Bean
    public MuseCodeHackathonModule museCodeHackathonModule() {
        return MuseCodeHackathonModule.current();
    }

    /**
     * HM1 (D-HM1-2): the META endpoint pin, collected by {@code RemediationProviderConfiguration} with the
     * platform's own beans and passed to the router, which refuses a META route no policy admits. The gate is a
     * configured META route: the router consults this policy only for a route the deployment configured, so the
     * bean alone changes no dispatch, no digest and no grant.
     */
    @Bean
    public RemediationProviderRouter.EndpointPolicy metaEndpointPolicy() {
        return new ProviderEndpointPolicy();
    }
}
