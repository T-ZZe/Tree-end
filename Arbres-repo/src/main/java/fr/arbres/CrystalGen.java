package fr.arbres;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Nouveau "biome" : la Forêt de cristaux. Des zones circulaires (80 à 180 blocs de large) dans les biomes terrestres
 * où poussent de hautes aiguilles d'améthyste, avec des amas lumineux, un socle de calcite et des petits cristaux au sol.
 * Le biome de la zone peut être changé pour teinter l'herbe (config crystals.biome, ex. cherry_grove).
 */
final class CrystalGen {
    private CrystalGen() {}

    private static final int CELL = 8; // une zone possible par bloc de 8x8 chunks

    private static double hash(long seed, int x, int y, int z) {
        long h = seed;
        h ^= x * 0x9E3779B97F4A7C15L;
        h = Long.rotateLeft(h, 31) * 0xBF58476D1CE4E5B9L;
        h ^= y * 0x94D049BB133111EBL;
        h = Long.rotateLeft(h, 27) * 0x9E3779B97F4A7C15L;
        h ^= z * 0xC2B2AE3D27D4EB4FL;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        return (h >>> 11) * (1.0 / (1L << 53));
    }

    /** Force de la zone au centre du chunk : 0 = hors zone, jusqu'à 1 au coeur de la zone. */
    static double strength(long seed, int chunkX, int chunkZ) {
        double wx = chunkX * 16.0 + 8, wz = chunkZ * 16.0 + 8;
        int cellX = Math.floorDiv(chunkX, CELL), cellZ = Math.floorDiv(chunkZ, CELL);
        double best = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int cx = cellX + dx, cz = cellZ + dz;
                if (hash(seed + 501, cx, 1, cz) >= 0.10) continue;
                double zx = (cx * CELL + 1 + hash(seed + 502, cx, 2, cz) * (CELL - 2)) * 16.0;
                double zz = (cz * CELL + 1 + hash(seed + 503, cx, 3, cz) * (CELL - 2)) * 16.0;
                double rad = 48 + hash(seed + 504, cx, 4, cz) * 42;
                double d = Math.sqrt((wx - zx) * (wx - zx) + (wz - zz) * (wz - zz));
                if (d < rad) best = Math.max(best, 1.0 - d / rad);
            }
        }
        return best;
    }

    private static boolean badBiome(String b) {
        return b.contains("ocean") || b.contains("river") || b.contains("beach") || b.contains("mushroom")
                || b.contains("swamp") || b.contains("ice_spikes") || b.contains("desert") || b.contains("badlands")
                || b.contains("peaks") || b.contains("cave") || b.contains("deep");
    }

    private static boolean crystalGround(Material m) {
        String n = m.name();
        return Blocks.ground(m) || n.equals("STONE") || n.equals("SAND") || n.equals("SNOW_BLOCK") || n.equals("GRAVEL")
                || n.equals("COARSE_DIRT") || n.equals("PODZOL") || n.equals("MOSS_BLOCK");
    }

    static int crystals(Canvas cv, int chunkX, int chunkZ, long seed, double density, String biomeKey) {
        double st = strength(seed, chunkX, chunkZ);
        if (st <= 0) return 0;
        int x0 = chunkX * 16, z0 = chunkZ * 16;
        int cy = ExtraGen.surface(cv, x0 + 8, z0 + 8);
        if (cy == Integer.MIN_VALUE || badBiome(cv.biome(x0 + 8, cy, z0 + 8))) return 0;
        // teinte du biome (herbe, eau) sur les chunks bien dans la zone
        if (biomeKey != null && !biomeKey.isBlank() && st > 0.15) {
            for (int bx = 0; bx < 16; bx += 4) {
                for (int bz = 0; bz < 16; bz += 4) {
                    for (int by = Math.max(cv.minY(), 40); by < Math.min(cv.maxY(), 160); by += 4) {
                        cv.setBiome(x0 + bx, by, z0 + bz, biomeKey);
                    }
                }
            }
        }
        Random r = new Random(TreeGen.mix(seed * 37L + chunkX * 341873128712L + chunkZ * 132897987541L + 8484L));
        int n = (int) Math.floor(st * density * 4.0 + r.nextDouble());
        int made = 0;
        for (int i = 0; i < n; i++) {
            int x = x0 + 2 + r.nextInt(12), z = z0 + 2 + r.nextInt(12);
            int y = ExtraGen.surface(cv, x, z);
            if (y == Integer.MIN_VALUE) continue;
            Material g = cv.type(x, y, z);
            if (g == null || !crystalGround(g) || badBiome(cv.biome(x, y, z))) continue;
            int h = 7 + r.nextInt(16);
            spire(cv, r, seed, x, y, z, h, 1.6 + r.nextDouble() * 1.8);
            made++;
        }
        return made;
    }

    private static BlockData mat(String name, Material fallback) {
        Material m = Material.getMaterial(name);
        return (m == null ? fallback : m).createBlockData();
    }

    private static void spire(Canvas cv, Random r, long seed, int x, int y, int z, int h, double baseR) {
        BlockData amethyst = mat("AMETHYST_BLOCK", Material.STONE);
        BlockData budding = mat("BUDDING_AMETHYST", Material.STONE);
        BlockData calcite = mat("CALCITE", Material.STONE);
        double ang = r.nextDouble() * Math.PI * 2;
        double lean = r.nextDouble() * 0.28;
        double lx = Math.cos(ang) * lean, lz = Math.sin(ang) * lean;
        long s = seed ^ (x * 73856093L) ^ (z * 19349663L);
        Map<TreeGen.P, Boolean> placed = new LinkedHashMap<>();
        for (int k = -1; k <= h; k++) {
            double t = Math.max(0, k) / (double) h;
            double rad = baseR * Math.pow(1.0 - t, 0.8) + 0.4;
            double ccx = x + lx * Math.max(0, k), ccz = z + lz * Math.max(0, k);
            int ir = (int) Math.ceil(rad) + 1;
            int cxi = (int) Math.round(ccx), czi = (int) Math.round(ccz);
            for (int dx = -ir; dx <= ir; dx++) {
                for (int dz = -ir; dz <= ir; dz++) {
                    int px = cxi + dx, pz = czi + dz;
                    double ddx = px - ccx, ddz = pz - ccz;
                    // section en losange : donne des arêtes de cristal
                    double d = (Math.abs(ddx) + Math.abs(ddz)) / (rad * 1.25);
                    double n = hash(s, px, y + k, pz);
                    if (d + (n - 0.5) * 0.3 > 1.0) continue;
                    Material cur = cv.type(px, y + k, pz);
                    if (cur == null) continue;
                    boolean ok = cur.isAir() || Blocks.replaceable(cur) || (k <= 0 && crystalGround(cur));
                    if (!ok) continue;
                    double n2 = hash(s + 1, px, y + k, pz);
                    BlockData b;
                    if (k <= 1 && d > 0.55) b = calcite;
                    else if (n2 < 0.07) b = budding;
                    else b = amethyst;
                    cv.set(px, y + k, pz, b);
                    if (k > 0) placed.put(new TreeGen.P(px, y + k, pz), Boolean.TRUE);
                }
            }
        }
        // amas de cristaux sur les faces exposées
        BlockFace[] faces = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP};
        List<TreeGen.P> list = new ArrayList<>(placed.keySet());
        for (TreeGen.P p : list) {
            for (BlockFace f : faces) {
                if (r.nextDouble() > 0.10) continue;
                int nx = p.x() + f.getModX(), ny = p.y() + f.getModY(), nz = p.z() + f.getModZ();
                Material q = cv.type(nx, ny, nz);
                if (q == null || !q.isAir()) continue;
                String name = r.nextDouble() < 0.35 ? "AMETHYST_CLUSTER" : (r.nextBoolean() ? "LARGE_AMETHYST_BUD" : "MEDIUM_AMETHYST_BUD");
                Material cm = Material.getMaterial(name);
                if (cm == null) continue;
                BlockData cd = cm.createBlockData();
                if (cd instanceof Directional dd) dd.setFacing(f);
                cv.set(nx, ny, nz, cd);
            }
        }
        // petits cristaux au sol autour
        int ground = 3 + r.nextInt(4);
        for (int i = 0; i < ground; i++) {
            double a = r.nextDouble() * Math.PI * 2;
            double d = 2.5 + r.nextDouble() * 4.5;
            int gx = x + (int) Math.round(Math.cos(a) * d), gz = z + (int) Math.round(Math.sin(a) * d);
            int gy = ExtraGen.surface(cv, gx, gz);
            if (gy == Integer.MIN_VALUE) continue;
            Material g = cv.type(gx, gy, gz);
            Material up = cv.type(gx, gy + 1, gz);
            if (g == null || !crystalGround(g) || up == null || !(up.isAir() || Blocks.replaceable(up))) continue;
            Material cm = Material.getMaterial(r.nextBoolean() ? "SMALL_AMETHYST_BUD" : "MEDIUM_AMETHYST_BUD");
            if (cm == null) continue;
            BlockData cd = cm.createBlockData();
            if (cd instanceof Directional dd) dd.setFacing(BlockFace.UP);
            cv.set(gx, gy + 1, gz, cd);
        }
    }
}
