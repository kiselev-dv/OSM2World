package org.osm2world.core.target.common.texcoord;

import static java.util.Collections.*;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.TextureData;
import org.osm2world.core.target.common.material.TextureDataDimensions;
import org.osm2world.core.target.common.material.TextureLayer;

/**
 * utility class for texture coordinate calculation
 */
public final class TexCoordUtil {

	/** prevents instantiation */
	private TexCoordUtil() {}

	/**
	 * calculates the texture coordinate lists based on the
	 * {@link TexCoordFunction} associated with each {@link TextureLayer}
	 */
	public static final List<List<VectorXZ>> texCoordLists(
			List<VectorXYZ> vs, Material material,
			Function<TextureDataDimensions, ? extends TexCoordFunction> defaultCoordFunctionGenerator) {

		List<TextureLayer> textureLayers = material.getTextureLayers();

		if (textureLayers.size() == 0) {

			return emptyList();

		} else if (textureLayers.size() == 1) {

			TextureData textureData = textureLayers.get(0).baseColorTexture;
			TexCoordFunction coordFunction = textureData.coordFunction;
			if (coordFunction == null) {
				coordFunction = defaultCoordFunctionGenerator.apply(textureData.dimensions());
			}

			return singletonList(coordFunction.apply(vs));

		} else {

			List<List<VectorXZ>> result = new ArrayList<>();

			for (TextureLayer textureLayer : textureLayers) {

				TextureData textureData = textureLayer.baseColorTexture;

				TexCoordFunction coordFunction = textureData.coordFunction;
				if (coordFunction == null) {
					coordFunction = defaultCoordFunctionGenerator.apply(textureData.dimensions());
				}

				result.add(coordFunction.apply(vs));

			}

			return result;

		}

	}

	/**
	 * equivalent of {@link #texCoordLists(List, Material, Function)}
	 * for a collection of triangle objects.
	 */
	public static final List<List<VectorXZ>> triangleTexCoordLists(
			Collection<TriangleXYZ> triangles, Material material,
			Function<TextureDataDimensions, ? extends TexCoordFunction> defaultCoordFunctionGenerator) {

		List<VectorXYZ> vs = new ArrayList<VectorXYZ>(triangles.size() * 3);

		for (TriangleXYZ triangle : triangles) {
			vs.add(triangle.v1);
			vs.add(triangle.v2);
			vs.add(triangle.v3);
		}

		return texCoordLists(vs, material, defaultCoordFunctionGenerator);

	}

	/** returns a horizontally flipped version of a {@link TexCoordFunction} */
	public static final TexCoordFunction mirroredHorizontally(TexCoordFunction texCoordFunction) {
		return (List<VectorXYZ> vs) -> {
			List<VectorXZ> result = texCoordFunction.apply(vs);
			return result.stream().map(v -> new VectorXZ(1 - v.x, v.z)).collect(toList());
		};
	}

	public static final Function<TextureDataDimensions, TexCoordFunction> mirroredHorizontally(
			Function<TextureDataDimensions, ? extends TexCoordFunction> generator) {
		return (TextureDataDimensions textureDimensions) -> mirroredHorizontally(generator.apply(textureDimensions));
	}

}