package com.ultimateautobuilder.core.validation;

import com.ultimateautobuilder.core.model.VoxelModel;
import com.ultimateautobuilder.core.placement.Placement;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Contrôles avant construction : taille, nombre de blocs, hauteur du monde, syntaxe des états. */
public final class StructureValidator {
    /** « namespace:chemin[propriété=valeur,…] » tel qu'accepté par l'analyseur d'états de Minecraft. */
    public static final Pattern STATE_SYNTAX =
            Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+(\\[[a-z0-9_]+=[a-z0-9_]+(,[a-z0-9_]+=[a-z0-9_]+)*])?");

    public enum Severity { ERROR, CONFIRM, WARNING }

    public record Issue(Severity severity, String message) {
    }

    public record Report(List<Issue> issues) {
        public boolean hasErrors() {
            return issues.stream().anyMatch(i -> i.severity() == Severity.ERROR);
        }

        public boolean needsConfirmation() {
            return issues.stream().anyMatch(i -> i.severity() == Severity.CONFIRM);
        }

        public String summary() {
            StringBuilder sb = new StringBuilder();
            for (Issue i : issues) {
                if (!sb.isEmpty()) {
                    sb.append(" ; ");
                }
                sb.append(i.message());
            }
            return sb.toString();
        }
    }

    private StructureValidator() {
    }

    public static boolean isValidState(String state) {
        return state != null && state.length() <= 256 && STATE_SYNTAX.matcher(state).matches();
    }

    /**
     * @param minWorldY hauteur minimale constructible (incluse)
     * @param maxWorldY hauteur maximale constructible (exclue)
     */
    public static Report validate(Placement placement, Limits limits, int minWorldY, int maxWorldY) {
        List<Issue> issues = new ArrayList<>();
        VoxelModel m = placement.model();
        if (m.isEmpty()) {
            issues.add(new Issue(Severity.ERROR, "Le modèle ne contient aucun bloc"));
            return new Report(issues);
        }
        int maxDim = Math.max(m.sizeX(), Math.max(m.sizeY(), m.sizeZ()));
        if (maxDim > limits.maxDimension()) {
            issues.add(new Issue(Severity.ERROR, "Dimension " + maxDim + " supérieure à la limite de " + limits.maxDimension() + " blocs"));
        }
        if (m.size() > limits.maxBlocks()) {
            issues.add(new Issue(Severity.ERROR, m.size() + " blocs : limite de " + limits.maxBlocks() + " dépassée"));
        } else if (m.size() > limits.giantThreshold()) {
            issues.add(new Issue(Severity.CONFIRM, "Construction géante (" + m.size() + " blocs) : confirmation requise"));
        }
        int[] b = placement.bounds();
        if (b[1] < minWorldY) {
            issues.add(new Issue(Severity.ERROR, "La base (y=" + b[1] + ") passe sous la limite du monde (y=" + minWorldY + ")"));
        }
        if (b[4] >= maxWorldY) {
            issues.add(new Issue(Severity.ERROR, "Le sommet (y=" + b[4] + ") dépasse la hauteur maximale du monde (y=" + (maxWorldY - 1) + ")"));
        }
        if (b[4] >= maxWorldY - 16 && b[4] < maxWorldY) {
            issues.add(new Issue(Severity.WARNING, "Le sommet est proche de la limite de hauteur"));
        }
        for (int i = 0; i < m.paletteSize(); i++) {
            if (!isValidState(m.paletteEntry(i))) {
                issues.add(new Issue(Severity.ERROR, "État de bloc invalide : " + m.paletteEntry(i)));
                break;
            }
        }
        return new Report(issues);
    }
}
