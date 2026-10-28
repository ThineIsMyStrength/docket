// Built during the Meta Global AI Developer Hackathon 2026 (26 October to 4 November) with Meta's Muse Code.
// Licence: see LICENSE of the public repository (decision D-H4).
package com.marlab.docket.musecode.route;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marlab.docket.ai.remediation.RemediationProtocol;
import com.marlab.docket.ai.remediation.RemediationProvider;
import com.marlab.docket.ai.remediation.RemediationProviderRouter;
import com.marlab.docket.ai.remediation.RemediationProtocol.Capability;
import com.marlab.docket.ai.remediation.RemediationProtocol.Fact;
import com.marlab.docket.ai.remediation.RemediationProtocol.FactKind;
import com.marlab.docket.ai.remediation.RemediationProtocol.Packet;
import com.marlab.docket.ai.remediation.RemediationProtocol.Target;
import com.marlab.docket.ai.remediation.RemediationProvider.Route;
import com.marlab.docket.aigov.AiAugmentationPolicy;
import com.marlab.docket.aigov.AiFeatureCode;
import com.marlab.docket.aigov.BelowThresholdAction;
import com.marlab.docket.aigov.ProviderInvocationJournal;
import com.marlab.docket.musecode.MuseCodeHackathonConfiguration;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.FileSystemResource;

/**
 * HM1: the META endpoint pin admits only the pinned endpoint, the route contract admits META, a granted
 * FIGURE policy selects a META route through the router, and anything else is refused before any dispatch.
 * Provider admission is available in this tree (the owner's seam, D-HM1-1), so the real-route assertions run;
 * none of them is NOT_RUN.
 */
class ProviderEndpointPolicyTest {
    static final String MODEL = "muse-spark-1.3";
    static final String ENDPOINT = "https://api.meta.ai/v1/responses";
    static final UUID ORG = UUID.fromString("00000000-0000-4000-8000-000000000001");
    static final String SOURCE = "b".repeat(64);
    static final String SHA = "a".repeat(64);
    /**
     * The identity digest of {@link #meta(String)}, recorded from the first run of this test
     * ({@code hm1-recording}, which failed only on this placeholder) and pinned hereafter.
     */
    static final String META_DIGEST = "e54087b3513ecbe246bdc28bd97005ced312cb4a342a02ebeace692af1f2f6da";

    static Route meta(String endpoint) {
        return meta(endpoint, "GLOBAL");
    }

    static Route meta(String endpoint, String region) {
        return new Route("META", MODEL, region, URI.create(endpoint), Set.of(MODEL), Set.of(Capability.FIGURE),
                new BigDecimal("0.00000125"), new BigDecimal("0.00000425"), 2048, "meta-vision-1");
    }

    static AiAugmentationPolicy policy(String provider, String model) {
        return new AiAugmentationPolicy(AiFeatureCode.PDF_ACCESSIBILITY_FIGURE_ALT_TEXT, true, true, .95,
                BelowThresholdAction.CONTINUE_WITHOUT_APPLYING, provider, model, "ALT_TEXT_CONFIDENCE", 3, 1, null,
                null, Map.of(), null);
    }

    static Packet packet() {
        return new Packet(Capability.FIGURE, SOURCE, SHA, List.of(new Target("target", 1, SHA, List.of())),
                List.of(new Fact("caption", Set.of("target"), FactKind.TEXT, Map.of("text", "Annual sales chart"))),
                List.of(), "");
    }

    /** A dispatching adapter that counts calls; every refusal below must leave it at zero. */
    static final class Fake implements RemediationProvider {
        final Route route;
        final AtomicInteger calls = new AtomicInteger();

        Fake(Route route) {
            this.route = route;
        }

        @Override
        public Route route() {
            return route;
        }

        @Override
        public Reply propose(Packet packet, UUID token, int maxOutputTokens, Control control) {
            calls.incrementAndGet();
            return new Reply("{}", route.model(), 1, 1, true, "fixture");
        }
    }

    /** Selection and pre-dispatch refusal never touch the journal; any attempt fails the test. */
    static final class StubJournal implements ProviderInvocationJournal {
        @Override
        public Optional<Entry> find(Key key) {
            return Optional.empty();
        }

        @Override
        public Entry reserve(Key key, Limits limits, Spend spend, Instant now) {
            throw new UnsupportedOperationException("no reservation before authorize");
        }

        @Override
        public void complete(Entry entry, String responseJson) {
            throw new UnsupportedOperationException("no completion before authorize");
        }

        @Override
        public void complete(Entry entry, String responseJson, Spend actual) {
            throw new UnsupportedOperationException("no completion before authorize");
        }

        @Override
        public void rejected(Entry entry, Instant retryNotBefore) {
            throw new UnsupportedOperationException("no rejection before authorize");
        }

        @Override
        public void uncertain(Entry entry) {
            throw new UnsupportedOperationException("no uncertainty before authorize");
        }

        @Override
        public Usage usage(UUID organizationId, String jobId) {
            return Usage.NONE;
        }
    }

    static RemediationProviderRouter router(Fake fake, List<RemediationProviderRouter.EndpointPolicy> policies) {
        return new RemediationProviderRouter(List.of(fake),
                Map.of(ORG, new RemediationProviderRouter.Grant(Set.of(fake.route.sha256()), null,
                        new ProviderInvocationJournal.Limits(4, 2, 65536, 16384, BigDecimal.ONE, 10))),
                new StubJournal(), false, policies);
    }

    static Stream<Arguments> refusedEndpoints() {
        return Stream.of(
                Arguments.of("https://api.meta.example/v1/responses", "GLOBAL"),
                Arguments.of("https://api.meta.ai/v1/messages", "GLOBAL"),
                Arguments.of("http://api.meta.ai/v1/responses", "GLOBAL"),
                Arguments.of("https://api.meta.ai:8443/v1/responses", "GLOBAL"),
                Arguments.of(ENDPOINT, "EU"));
    }

    @Test
    void thePolicyNamesMeta() {
        assertThat(new ProviderEndpointPolicy().provider()).isEqualTo("META");
    }

    @Test
    void thePolicyAdmitsThePinnedEndpointOnTheDefaultPortAndOn443() {
        var policy = new ProviderEndpointPolicy();
        assertThat(policy.admits(meta(ENDPOINT))).isTrue();
        assertThat(policy.admits(meta("https://api.meta.ai:443/v1/responses"))).isTrue();
    }

    @ParameterizedTest(name = "{0} [{1}]")
    @MethodSource("refusedEndpoints")
    void thePolicyRefusesAnythingButThePin(String endpoint, String region) {
        assertThat(new ProviderEndpointPolicy().admits(meta(endpoint, region))).isFalse();
    }

    @Test
    void aGrantedFigurePolicySelectsTheMetaRoute() {
        var fake = new Fake(meta(ENDPOINT));
        var selected = router(fake, List.of(new ProviderEndpointPolicy())).selected(ORG, policy("META", MODEL),
                Capability.FIGURE);
        assertThat(selected.provider()).isEqualTo("META");
        assertThat(selected.credentialScope()).isEqualTo("meta-vision-1");
        assertThat(selected.sha256()).isEqualTo(fake.route.sha256());
        assertThatThrownBy(() -> router(fake, List.of(new ProviderEndpointPolicy())).selected(ORG,
                policy("META", "another-model"), Capability.FIGURE)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void anUnapprovedEndpointIsRefusedBeforeAnyCall() throws Exception {
        var fake = new Fake(meta("https://api.meta.example/v1/responses"));
        var router = router(fake, List.of(new ProviderEndpointPolicy()));
        var control = new RemediationProvider.Control(Instant.now().plusSeconds(30), Clock.systemUTC(), () -> {});
        assertThatThrownBy(() -> router.propose(ORG, UUID.randomUUID().toString(), policy("META", MODEL), packet(),
                control)).isInstanceOf(IllegalStateException.class).hasMessage("FORBIDDEN_ENDPOINT_OR_REGION");
        assertThat(fake.calls.get()).isZero();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("routerRefusedEndpoints")
    void theRouterRefusesUserInfoAQueryAFragmentAndAnUnpinnedHost(String endpoint) {
        var fake = new Fake(meta(endpoint));
        assertThatThrownBy(() -> router(fake, List.of(new ProviderEndpointPolicy())).selected(ORG,
                policy("META", MODEL), Capability.FIGURE)).isInstanceOf(IllegalStateException.class)
                .hasMessage("FORBIDDEN_ENDPOINT_OR_REGION");
        assertThat(fake.calls.get()).isZero();
    }

    static Stream<Arguments> routerRefusedEndpoints() {
        return Stream.of(
                Arguments.of("https://user@api.meta.ai/v1/responses"),
                Arguments.of("https://api.meta.ai/v1/responses?x=1"),
                Arguments.of("https://api.meta.ai/v1/responses#f"),
                Arguments.of("https://api.meta.example/v1/responses"));
    }

    @Test
    void withoutAPolicyForMetaLocalItsRouteIsRefused() {
        var local = new Route("META_LOCAL", "muse-glimmer-30b", "LOCAL", URI.create("https://vlm.internal/v1/responses"),
                Set.of("muse-glimmer-30b"), Set.of(Capability.ORDER), BigDecimal.ZERO, BigDecimal.ZERO, 2048);
        var fake = new Fake(local);
        assertThatThrownBy(() -> router(fake, List.of(new ProviderEndpointPolicy())).selected(ORG,
                policy("META_LOCAL", "muse-glimmer-30b"), Capability.ORDER)).isInstanceOf(IllegalStateException.class)
                .hasMessage("FORBIDDEN_ENDPOINT_OR_REGION");
        assertThatThrownBy(() -> router(fake, List.of()).selected(ORG, policy("META_LOCAL", "muse-glimmer-30b"),
                Capability.ORDER)).isInstanceOf(IllegalStateException.class)
                .hasMessage("FORBIDDEN_ENDPOINT_OR_REGION");
        assertThat(fake.calls.get()).isZero();
    }

    @Test
    void museIsRefusedByTheRouteConstructor() {
        assertThatThrownBy(() -> new Route("MUSE", MODEL, "GLOBAL", URI.create(ENDPOINT), Set.of(MODEL),
                Set.of(Capability.FIGURE), new BigDecimal("0.00000125"), new BigDecimal("0.00000425"), 2048))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Approved model quote required");
    }

    @Test
    void metaRouteDigestEqualsTheRecordedConstant() {
        assertThat(meta(ENDPOINT).sha256()).isEqualTo(META_DIGEST);
    }

    @Test
    void metaRouteDigestChangesWithEveryIdentityField() {
        String pinned = meta(ENDPOINT).sha256();
        assertThat(new Route("META", "other-model", "GLOBAL", URI.create(ENDPOINT), Set.of(MODEL),
                Set.of(Capability.FIGURE), new BigDecimal("0.00000125"), new BigDecimal("0.00000425"), 2048,
                "meta-vision-1").sha256()).isNotEqualTo(pinned);
        assertThat(meta("https://api.meta.ai:443/v1/responses").sha256()).isNotEqualTo(pinned);
        assertThat(meta(ENDPOINT, "EU").sha256()).isNotEqualTo(pinned);
        assertThat(new Route("META", MODEL, "GLOBAL", URI.create(ENDPOINT), Set.of(MODEL, MODEL + "-actual"),
                Set.of(Capability.FIGURE), new BigDecimal("0.00000125"), new BigDecimal("0.00000425"), 2048,
                "meta-vision-1").sha256()).isNotEqualTo(pinned);
        assertThat(new Route("META", MODEL, "GLOBAL", URI.create(ENDPOINT), Set.of(MODEL),
                Set.of(Capability.FIGURE, Capability.TABLE), new BigDecimal("0.00000125"),
                new BigDecimal("0.00000425"), 2048, "meta-vision-1").sha256()).isNotEqualTo(pinned);
        assertThat(new Route("META", MODEL, "GLOBAL", URI.create(ENDPOINT), Set.of(MODEL), Set.of(Capability.FIGURE),
                new BigDecimal("0.00000999"), new BigDecimal("0.00000425"), 2048, "meta-vision-1").sha256())
                .isNotEqualTo(pinned);
        assertThat(new Route("META", MODEL, "GLOBAL", URI.create(ENDPOINT), Set.of(MODEL), Set.of(Capability.FIGURE),
                new BigDecimal("0.00000125"), new BigDecimal("0.00000999"), 2048, "meta-vision-1").sha256())
                .isNotEqualTo(pinned);
        assertThat(new Route("META", MODEL, "GLOBAL", URI.create(ENDPOINT), Set.of(MODEL), Set.of(Capability.FIGURE),
                new BigDecimal("0.00000125"), new BigDecimal("0.00000425"), 1024, "meta-vision-1").sha256())
                .isNotEqualTo(pinned);
        assertThat(new Route("META", MODEL, "GLOBAL", URI.create(ENDPOINT), Set.of(MODEL), Set.of(Capability.FIGURE),
                new BigDecimal("0.00000125"), new BigDecimal("0.00000425"), 2048, "meta-vision-2").sha256())
                .isNotEqualTo(pinned);
        // An equivalent quote keeps the digest: trailing zeros are not identity.
        assertThat(new Route("META", MODEL, "GLOBAL", URI.create(ENDPOINT), Set.of(MODEL), Set.of(Capability.FIGURE),
                new BigDecimal("0.0000012500"), new BigDecimal("0.0000042500"), 2048, "meta-vision-1").sha256())
                .isEqualTo(pinned);
    }

    @Test
    void theConfigurationExposesTheMetaEndpointPolicy() {
        var policy = new MuseCodeHackathonConfiguration().metaEndpointPolicy();
        assertThat(policy.provider()).isEqualTo("META");
        assertThat(policy.admits(meta(ENDPOINT))).isTrue();
        assertThat(policy.admits(meta("https://api.meta.example/v1/responses"))).isFalse();
    }

    /** The bind target for the fixture: the adapter shape production binds, owned by this module. */
    record FixtureProviders(List<FixtureAdapter> adapters, Map<UUID, FixtureTenant> tenants) {
    }

    record FixtureAdapter(String id, String provider, String model, String region, String endpoint,
            List<String> actualModels, List<String> capabilities, BigDecimal inputTokenPrice,
            BigDecimal outputTokenPrice, int imageTokenCeiling, String credentialProperty, Boolean baa,
            String retention) {
    }

    record FixtureTenant(List<String> routes, String fallback, int calls, int pages, long inputTokens,
            long outputTokens, BigDecimal cost, int monthlyCalls) {
    }

    /**
     * The module-owned META fixture binds through the public Spring binder (the same mechanism the application
     * uses for {@code app.accessibility.providers}) and its values make a route the router selects. The module
     * never depends on {@code platform-api}, so the bind target mirrors the adapter shape instead of naming the
     * platform's {@code Properties} record; binding to that record is proven in the platform tree by
     * {@code MetaRouteSeamsTest.theHackathonProfileBindsAndStaysClosedUntilTheModulePinsMeta}.
     */
    @Test
    void theModuleFixtureBindsToASelectableMetaRoute() throws Exception {
        var fixture = Path.of("src", "test", "resources", "com", "marlab", "docket", "musecode", "route",
                "meta-route-fixture.yml");
        var environment = new StandardEnvironment();
        new YamlPropertySourceLoader().load("fixture", new FileSystemResource(fixture))
                .forEach(source -> environment.getPropertySources().addLast(source));
        var bound = Binder.get(environment).bind("app.accessibility.providers", FixtureProviders.class)
                .orElseThrow(() -> new IllegalStateException("the fixture binds no app.accessibility.providers"));
        assertThat(bound.adapters()).hasSize(1);
        var adapter = bound.adapters().getFirst();
        assertThat(adapter.id()).isEqualTo("meta-vision-1");
        assertThat(adapter.provider()).isEqualTo("META");
        assertThat(adapter.model()).isEqualTo(MODEL);
        assertThat(adapter.region()).isEqualTo("GLOBAL");
        assertThat(adapter.endpoint()).isEqualTo(ENDPOINT);
        assertThat(adapter.actualModels()).containsExactly(MODEL);
        assertThat(adapter.capabilities()).containsExactly("FIGURE");
        assertThat(adapter.inputTokenPrice()).isEqualByComparingTo("0.00000125");
        assertThat(adapter.outputTokenPrice()).isEqualByComparingTo("0.00000425");
        assertThat(adapter.imageTokenCeiling()).isEqualTo(2048);
        assertThat(adapter.credentialProperty()).isEqualTo("app.accessibility.providers.credentials.meta-vision-1");
        assertThat(adapter.baa()).isEqualTo(Boolean.FALSE);
        assertThat(adapter.retention()).isNull();
        var uuid = UUID.fromString("00000000-0000-4000-8000-000000000000");
        assertThat(bound.tenants().keySet()).containsExactly(uuid);
        var tenant = bound.tenants().get(uuid);
        assertThat(tenant.routes()).containsExactly("meta-vision-1");

        var route = new Route(adapter.provider(), adapter.model(), adapter.region(), URI.create(adapter.endpoint()),
                Set.copyOf(adapter.actualModels()),
                Set.of(RemediationProtocol.Capability.valueOf(adapter.capabilities().getFirst())),
                adapter.inputTokenPrice(), adapter.outputTokenPrice(), adapter.imageTokenCeiling(), adapter.id());
        var fake = new Fake(route);
        var selected = new RemediationProviderRouter(List.of(fake),
                Map.of(uuid, new RemediationProviderRouter.Grant(Set.of(route.sha256()), null,
                        new ProviderInvocationJournal.Limits(tenant.calls(), tenant.pages(), tenant.inputTokens(),
                                tenant.outputTokens(), tenant.cost(), tenant.monthlyCalls()))),
                new StubJournal(), false, List.of(new ProviderEndpointPolicy())).selected(uuid, policy("META", MODEL),
                Capability.FIGURE);
        assertThat(selected.credentialScope()).isEqualTo("meta-vision-1");
    }
}
