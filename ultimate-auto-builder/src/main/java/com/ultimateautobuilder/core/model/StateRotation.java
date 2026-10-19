package com.ultimateautobuilder.core.model;

/**
 * Rotation horaire d'un état de bloc textuel autour de l'axe Y, utilisée lorsqu'un générateur
 * assemble des sous-structures (bâtiments d'une ville orientés vers la rue).
 * La rotation finale à la pose utilise l'API Minecraft, qui couvre tous les blocs.
 */
public final class StateRotation {
    private static final String[] CYCLE = {"north", "east", "south", "west"};

    private StateRotation() {
    }

    private static String rotateDir(String dir, int turns) {
        for (int i = 0; i < 4; i++) {
            if (CYCLE[i].equals(dir)) {
                return CYCLE[(i + turns) & 3];
            }
        }
        return dir;
    }

    public static String rotate(String state, int quarterTurns) {
        int turns = Math.floorMod(quarterTurns, 4);
        int open = state.indexOf('[');
        if (turns == 0 || open < 0 || !state.endsWith("]")) {
            return state;
        }
        String id = state.substring(0, open);
        String[] props = state.substring(open + 1, state.length() - 1).split(",");
        // Propriétés de connexion (clôtures, vitres) : les valeurs sont redistribuées entre directions.
        String[] connValues = new String[4];
        boolean hasConn = false;
        StringBuilder out = new StringBuilder(id).append('[');
        boolean first = true;
        for (String p : props) {
            int eq = p.indexOf('=');
            if (eq < 0) {
                continue;
            }
            String key = p.substring(0, eq);
            String value = p.substring(eq + 1);
            int dirIndex = indexOf(key);
            if (dirIndex >= 0) {
                connValues[(dirIndex + turns) & 3] = value;
                hasConn = true;
                continue;
            }
            if (key.equals("facing") || key.equals("horizontal_facing")) {
                value = rotateDir(value, turns);
            } else if (key.equals("axis") && (turns & 1) == 1) {
                if (value.equals("x")) {
                    value = "z";
                } else if (value.equals("z")) {
                    value = "x";
                }
            } else if (key.equals("rotation")) {
                try {
                    value = Integer.toString((Integer.parseInt(value) + turns * 4) & 15);
                } catch (NumberFormatException ignored) {
                    // Valeur non numérique : conservée telle quelle.
                }
            }
            if (!first) {
                out.append(',');
            }
            out.append(key).append('=').append(value);
            first = false;
        }
        if (hasConn) {
            for (int i = 0; i < 4; i++) {
                if (connValues[i] != null) {
                    if (!first) {
                        out.append(',');
                    }
                    out.append(CYCLE[i]).append('=').append(connValues[i]);
                    first = false;
                }
            }
        }
        return out.append(']').toString();
    }

    private static int indexOf(String key) {
        for (int i = 0; i < 4; i++) {
            if (CYCLE[i].equals(key)) {
                return i;
            }
        }
        return -1;
    }
}
