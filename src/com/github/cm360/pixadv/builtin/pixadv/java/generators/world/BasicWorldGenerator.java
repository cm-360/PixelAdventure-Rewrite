package com.github.cm360.pixadv.builtin.pixadv.java.generators.world;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.github.cm360.pixadv.builtin.pixadv.java.tiles.types.luna.Luminite;
import com.github.cm360.pixadv.builtin.pixadv.java.tiles.types.terra.Dirt;
import com.github.cm360.pixadv.builtin.pixadv.java.tiles.types.terra.Stone;
import com.github.cm360.pixadv.world.storage.world.LocalWorld;
import com.github.cm360.pixadv.world.storage.world.World;
import com.github.cm360.pixadv.world.types.generators.WorldGenerator;
import com.github.cm360.pixadv.world.types.tiles.Tile;

import de.articdive.jnoise.JNoise;

public class BasicWorldGenerator implements WorldGenerator {

	protected long seed;
	protected Random random;
	protected JNoise noiseGen;
	
	protected String name;
	protected int width;
	protected int height;
	protected int chunkSize;
	protected int seaLevel;
	protected int maxMountainHeight;
	protected int maxOceanDepth;
	
	protected World world;
	protected Phase generationPhase = Phase.Waiting;
	
	private int[] heightmap;
	
	public BasicWorldGenerator(long seed) {
		// Pseudo-random generators
		this.seed = seed;
		random = new Random(seed);
		noiseGen = JNoise.newBuilder().fastSimplex().setSeed(seed).build();
		// World properties
		name = "New World";
		width = 100;
		height = 20;
		chunkSize = 20;
		seaLevel = (height * chunkSize) / 2;
		maxMountainHeight = chunkSize;
		maxOceanDepth = chunkSize;
	}
	
	@Override
	public World generate() {
		// Create world object and populate with empty chunks
		generationPhase = Phase.Init;
		Map<String, String> worldInfo = new HashMap<String, String>();
		worldInfo.put("name", name);
		world = new LocalWorld(width, height, chunkSize, worldInfo, null);
		for (int cx = 0; cx < width; cx++)
			for (int cy = 0; cy < height; cy++)
				world.createChunk(cx, cy);
		// Generate terrain
		generationPhase = Phase.Heightmap;
		int[] heightmap = generateHeightmap(0.2);
		generationPhase = Phase.Surface;
		for (int x = 0; x < heightmap.length; x++) {
			for (int y = 0; y <= heightmap[x]; y++) {
				Dirt grass = new Dirt();
				grass.grass = true;
				Dirt grass2 = new Dirt();
				grass2.grass = true;
				world.setTile((heightmap[x] - y) < 15 ? new Dirt() : new Stone(), x, y, 0);
				Tile otherTile = new Stone();
				if (random.nextInt(50) == 6)
					otherTile = new Luminite();
				world.setTile(heightmap[x] == y ? grass2 : ((heightmap[x] - y) < 15 ? new Dirt() : otherTile), x, y, 2);
			}
		}
		// Carve caves into terrain
		generationPhase = Phase.Caves;
		generateCaves();
		// Decorate surface
		generationPhase = Phase.Decorate;
		// Return completed world
		generationPhase = Phase.Complete;
		return world;
	}
	
	protected int[] generateHeightmap(double scale) {
		// Pseudo-random number generator
		int noiseZ = random.nextInt();
		// 
		heightmap = new int[width * chunkSize];
		for (int x = 0; x < heightmap.length; x++) {
			heightmap[x] = seaLevel + (int) Math.round(
					(chunkSize) * noiseGen.getNoise(
							Math.cos(((double) x / heightmap.length) * (2 * Math.PI))
									* (heightmap.length * (scale / 100.0)),
							Math.sin(((double) x / heightmap.length) * (2 * Math.PI))
									* (heightmap.length * (scale / 100.0)),
							noiseZ));
//			heightmap[i] = 1;
		}
		return heightmap;
	}
	
	protected void generateCaves() {
		generateCheeseCaves(0.2, 0.4, 0.5);
		generateNetworkCaves(0.06, 0.4, 0.5);
//		generateWormCaves(2, 0.1);
	}
	
	/**
	 * Generate cheese caves in this world.
	 *
	 * @param density The approximate percent of this world that should be cheese caves.
	 * @param scale   How much to scale the noise map used to generate caves.
	 * @param falloff How quickly the cave density decreases near the surface and world edges.
	 */
	protected void generateCheeseCaves(double density, double scale, double falloff) {
		int noiseZ = random.nextInt();
		for (int x = 0; x < width * chunkSize; x++) {
			for (int y = 0; y < height * chunkSize; y++) {
				double noise = noiseGen.getNoise(
						Math.cos(((double) x / heightmap.length) * (2 * Math.PI))
								* (heightmap.length * (scale / 100.0)),
						Math.sin(((double) x / heightmap.length) * (2 * Math.PI))
								* (heightmap.length * (scale / 100.0)),
						y / ((4 * Math.PI) / scale),
						noiseZ);
				if (((noise + 1) / 2) > (1 - density)) {
					world.setTile(null, x, y, 2);
				}
			}
		}
	}
	
	/**
	 * Generate cheese caves in this world.
	 *
	 * @param density The approximate percent of this world that should be cheese caves.
	 * @param scale   How much to scale the noise map used to generate caves.
	 * @param falloff How quickly the cave density decreases near the surface and world edges.
	 */
	protected void generateNetworkCaves(double density, double scale, double falloff) {
		int noiseZ = random.nextInt();
		for (int x = 0; x < width * chunkSize; x++) {
			for (int y = 0; y < height * chunkSize; y++) {
				double noise = noiseGen.getNoise(
						Math.cos(((double) x / heightmap.length) * (2 * Math.PI))
								* (heightmap.length * (scale / 100.0)),
						Math.sin(((double) x / heightmap.length) * (2 * Math.PI))
								* (heightmap.length * (scale / 100.0)),
						y / ((4 * Math.PI) / scale),
						noiseZ);
				if (Math.abs(noise) < density) {
					world.setTile(null, x, y, 2);
				}
			}
		}
	}
	
	/**
	 * Generate worm caves in this world.
	 *
	 * @param starts The number of times to attempt to generate a cave per chunk.
	 * @param chance The percent of starts that will actually generate a cave.
	 */
	protected void generateWormCaves(int starts, double chance) {
		for (int cx = 0; cx < width; cx++) {
			for (int cy = 0; cy < height; cy++) {
				for (int i = 0; i < starts; i++) {
					if ((1 - random.nextDouble(1)) >= chance) {
						generateWormCave(
								random.nextInt(cx + chunkSize),
								random.nextInt(cy + chunkSize),
								random.nextDouble(2 * Math.PI),
								5, 10, 10);
					}
				}
			}
		}
	}
	
	protected void generateWormCave(int x, int y, double angle, double weirdness, double length1, double length2) {
		
	}
	
	@Override
	public Phase getGenerationPhase() {
		return generationPhase;
	}
	
	@Override
	public BasicWorldGenerator setName(String name) {
		this.name = name;
		return this;
	}
	
	@Override
	public BasicWorldGenerator setWidth(int width) {
		this.width = width;
		return this;
	}
	
	@Override
	public BasicWorldGenerator setHeight(int height) {
		this.height = height;
		return this;
	}
	
	@Override
	public BasicWorldGenerator setChunkSize(int chunkSize) {
		this.chunkSize = chunkSize;
		return this;
	}

}
