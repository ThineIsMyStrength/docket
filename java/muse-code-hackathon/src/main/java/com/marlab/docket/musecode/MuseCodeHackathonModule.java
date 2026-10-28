// Built during the Meta Global AI Developer Hackathon 2026 (26 October to 4 November) with Meta's Muse Code.
// Licence: see LICENSE of the public repository (decision D-H4).
package com.marlab.docket.musecode;

import java.util.List;

/**
 * The hackathon module's charter as data: its id, the event, its root package and the sub-packages it may hold.
 * A test pins these so the evidence folder and the public README can state them from the code rather than by hand,
 * and so a class placed outside the charter's packages fails the build's own check rather than a reviewer's eye.
 *
 * @param id          the Maven artifact id of the module
 * @param event       the event the module was built for, with its building window
 * @param rootPackage the one root package every hackathon class lives below
 * @param packages    the sub-packages the charter allows, relative to the root
 */
public record MuseCodeHackathonModule(String id, String event, String rootPackage, List<String> packages) {
    public static final String ID = "muse-code-hackathon";
    public static final String EVENT = "Meta Global AI Developer Hackathon 2026 (26 October to 4 November)";
    public static final String ROOT_PACKAGE = "com.marlab.docket.musecode";
    public static final List<String> PACKAGES = List.of("route", "order", "spi", "api", "measure");

    public MuseCodeHackathonModule {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id required");
        if (event == null || event.isBlank()) throw new IllegalArgumentException("event required");
        if (rootPackage == null || rootPackage.isBlank()) throw new IllegalArgumentException("root package required");
        packages = List.copyOf(packages);
    }

    /** The charter this build was made under. */
    public static MuseCodeHackathonModule current() {
        return new MuseCodeHackathonModule(ID, EVENT, ROOT_PACKAGE, PACKAGES);
    }

    /** Whether a class name lies inside the charter: the root package itself or one of its allowed sub-packages. */
    public boolean admits(String className) {
        if (className == null || !className.startsWith(rootPackage + ".")) return false;
        String rest = className.substring(rootPackage.length() + 1);
        int dot = rest.indexOf('.');
        return dot < 0 || packages.contains(rest.substring(0, dot));
    }
}
