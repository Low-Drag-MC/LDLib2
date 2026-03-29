package com.lowdragmc.lowdraglib2.client.shader;

import lombok.experimental.UtilityClass;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
@UtilityClass
public final class LDProgramDefineManager {
    private static final Set<String> PROGRAM_DEFINES = new LinkedHashSet<>();

    public static void addProgramDefine(String define) {
        PROGRAM_DEFINES.add(define);
    }

    public static void removeProgramDefine(String define) {
        PROGRAM_DEFINES.remove(define);
    }

    public static void clearProgramDefines() {
        PROGRAM_DEFINES.clear();
    }

    public static boolean hasProgramDefine(String define) {
        return PROGRAM_DEFINES.contains(define);
    }

    public static boolean hasProgramDefines() {
        return !PROGRAM_DEFINES.isEmpty();
    }

    public static String createProgramDefinesString() {
        return createProgramDefinesString('\n');
    }

    public static String createProgramDefinesString(char separator) {
        StringBuilder sb = new StringBuilder();
        PROGRAM_DEFINES.forEach(def -> sb.append("#define ").append(def).append(separator));
        return sb.toString();
    }

    public static String createProgramNameWithDefines(String name) {
        return PROGRAM_DEFINES.isEmpty() ? name : name + "_" + createProgramDefinesString('_');
    }

    public static Set<String> getProgramDefines() {
        return PROGRAM_DEFINES;
    }
}
