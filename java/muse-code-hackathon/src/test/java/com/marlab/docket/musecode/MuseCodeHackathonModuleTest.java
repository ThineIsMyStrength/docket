// Built during the Meta Global AI Developer Hackathon 2026 (26 October to 4 November) with Meta's Muse Code.
// Licence: see LICENSE of the public repository (decision D-H4).
package com.marlab.docket.musecode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class MuseCodeHackathonModuleTest {

    @Test
    void charterNamesTheModuleTheEventAndThePackages() {
        MuseCodeHackathonModule module = MuseCodeHackathonModule.current();
        assertThat(module.id()).isEqualTo("muse-code-hackathon");
        assertThat(module.event()).contains("2026").contains("26 October").contains("4 November");
        assertThat(module.rootPackage()).isEqualTo("com.marlab.docket.musecode");
        assertThat(module.packages()).containsExactly("route", "order", "spi", "api", "measure");
    }

    @Test
    void theConfigurationExposesTheCharter() {
        assertThat(new MuseCodeHackathonConfiguration().museCodeHackathonModule())
                .isEqualTo(MuseCodeHackathonModule.current());
    }

    @Test
    void admitsOnlyTheRootPackageAndItsCharteredSubPackages() {
        MuseCodeHackathonModule module = MuseCodeHackathonModule.current();
        assertThat(module.admits("com.marlab.docket.musecode.MuseCodeHackathonModule")).isTrue();
        assertThat(module.admits("com.marlab.docket.musecode.route.MetaRemediationProvider")).isTrue();
        assertThat(module.admits("com.marlab.docket.musecode.order.OrderCandidates")).isTrue();
        assertThat(module.admits("com.marlab.docket.musecode.spi.OrderProposalHost")).isTrue();
        assertThat(module.admits("com.marlab.docket.musecode.elsewhere.Stray")).isFalse();
        assertThat(module.admits("com.marlab.docket.api.PlatformApplication")).isFalse();
        assertThat(module.admits(null)).isFalse();
    }

    @Test
    void aCharterWithoutAnIdOrEventIsRefused() {
        assertThatThrownBy(() -> new MuseCodeHackathonModule(" ", "e", "p", List.of())).hasMessage("id required");
        assertThatThrownBy(() -> new MuseCodeHackathonModule("i", "", "p", List.of())).hasMessage("event required");
        assertThatThrownBy(() -> new MuseCodeHackathonModule("i", "e", null, List.of())).hasMessage("root package required");
    }

    /** Every source file of this module lies inside the charter, so a stray package fails here, not in a review. */
    @Test
    void everySourceFileOfTheModuleLiesInsideTheCharter() throws IOException {
        MuseCodeHackathonModule module = MuseCodeHackathonModule.current();
        Path sources = Path.of("src", "main", "java");
        List<String> classes = new ArrayList<>();
        try (Stream<Path> files = Files.walk(sources)) {
            files.filter(path -> path.toString().endsWith(".java") && !path.endsWith("package-info.java"))
                    .forEach(path -> classes.add(sources.relativize(path).toString()
                            .replace('\\', '/').replace('/', '.').replaceAll("\\.java$", "")));
        }
        assertThat(classes).isNotEmpty().allMatch(module::admits);
    }
}
