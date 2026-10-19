package com.ultimateautobuilder.core.image;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Éditeur du modèle généré avec annuler/rétablir. Chaque opération enregistre une copie
 * (volume borné à {@link ImageModel#MAX_CELLS}) ; l'historique est limité pour borner la mémoire.
 */
public final class ModelEditor {
    public static final int MAX_HISTORY = 16;

    private ImageModel current;
    private final Deque<ImageModel> undo = new ArrayDeque<>();
    private final Deque<ImageModel> redo = new ArrayDeque<>();

    public ModelEditor(ImageModel initial) {
        if (initial == null) {
            throw new IllegalArgumentException("Modèle absent");
        }
        this.current = initial;
    }

    public ImageModel model() {
        return current;
    }

    private void push(ImageModel next) {
        undo.push(current);
        while (undo.size() > MAX_HISTORY) {
            undo.removeLast();
        }
        redo.clear();
        current = next;
    }

    /** Remplace un matériau ; sans effet (et sans entrée d'historique) si rien ne change. */
    public int replace(String from, String to) {
        ImageModel next = current.copy();
        int n = next.replace(from, to);
        if (n > 0) {
            push(next);
        }
        return n;
    }

    public int remove(String state) {
        ImageModel next = current.copy();
        int n = next.remove(state);
        if (n > 0 && next.count() > 0) {
            push(next);
            return n;
        }
        return 0;
    }

    public void rotate() {
        push(current.rotated());
    }

    public void mirror() {
        push(current.mirroredX());
    }

    public void scale(double k) {
        if (k <= 0 || Double.isNaN(k)) {
            throw new IllegalArgumentException("Échelle invalide");
        }
        push(current.scaled(k));
    }

    public boolean cropLayers(int minY, int maxY) {
        ImageModel next = current.cropLayers(minY, maxY);
        if (next.count() == 0) {
            return false;
        }
        push(next);
        return true;
    }

    public int hollow() {
        ImageModel next = current.copy();
        int n = next.hollow(false);
        if (n > 0) {
            push(next);
        }
        return n;
    }

    /** Remplace tout le modèle (nouvelle génération avec d'autres options) en gardant l'annulation. */
    public void replaceAll(ImageModel next) {
        push(next);
    }

    public boolean canUndo() {
        return !undo.isEmpty();
    }

    public boolean canRedo() {
        return !redo.isEmpty();
    }

    public boolean undo() {
        if (undo.isEmpty()) {
            return false;
        }
        redo.push(current);
        current = undo.pop();
        return true;
    }

    public boolean redo() {
        if (redo.isEmpty()) {
            return false;
        }
        undo.push(current);
        current = redo.pop();
        return true;
    }
}
