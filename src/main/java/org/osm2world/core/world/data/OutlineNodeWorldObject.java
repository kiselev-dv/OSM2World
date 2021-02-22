package org.osm2world.core.world.data;

import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_elevation.creation.EleConstraintEnforcer;
import org.osm2world.core.map_elevation.data.EleConnectorGroup;
import org.osm2world.core.math.AxisAlignedRectangleXZ;
import org.osm2world.core.math.BoundedObject;
import org.osm2world.core.math.PolygonXYZ;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.TriangleXZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.algorithms.TriangulationUtil;

/**
 * superclass for {@link NodeWorldObject}s that do have an outline
 * and are not just treated as an infinitely small point.
 *
 * @see NoOutlineNodeWorldObject
 */
public abstract class OutlineNodeWorldObject implements NodeWorldObject,
		BoundedObject, WorldObjectWithOutline {

	protected final MapNode node;

	private EleConnectorGroup connectors = null;

	protected OutlineNodeWorldObject(MapNode node) {
		this.node = node;
	}

	@Override
	public abstract SimplePolygonXZ getOutlinePolygonXZ();

	@Override
	public final MapNode getPrimaryMapElement() {
		return node;
	}

	@Override
	public EleConnectorGroup getEleConnectors() {

		if (connectors == null) {

			SimplePolygonXZ outlinePolygonXZ = getOutlinePolygonXZ();

			if (outlinePolygonXZ == null) {

				connectors = EleConnectorGroup.EMPTY;

			} else {

				connectors = new EleConnectorGroup();
				connectors.addConnectorsFor(outlinePolygonXZ.getVertices(),
						node, getGroundState());

			}

		}

		return connectors;

	}

	@Override
	public void defineEleConstraints(EleConstraintEnforcer enforcer) {}

	@Override
	public AxisAlignedRectangleXZ boundingBox() {
		if (getOutlinePolygonXZ() != null) {
			return getOutlinePolygonXZ().boundingBox();
		} else {
			return node.getPos().boundingBox();
		}
	}

	@Override
	public PolygonXYZ getOutlinePolygon() {
		return connectors.getPosXYZ(getOutlinePolygonXZ());
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "(" + node + ")";
	}

	/**
	 * @return  a triangulation of the area covered by this junction
	 */
	protected List<TriangleXYZ> getTriangulation() {

		if (getOutlinePolygonXZ() == null) return emptyList();

		Collection<TriangleXZ> trianglesXZ = TriangulationUtil.triangulate(
				getOutlinePolygonXZ(),
				Collections.<SimplePolygonXZ>emptyList());

		List<TriangleXYZ> trianglesXYZ = new ArrayList<>(trianglesXZ.size());

		for (TriangleXZ triangleXZ : trianglesXZ) {
			VectorXYZ v1 = connectors.getPosXYZ(triangleXZ.v1);
			VectorXYZ v2 = connectors.getPosXYZ(triangleXZ.v2);
			VectorXYZ v3 = connectors.getPosXYZ(triangleXZ.v3);
			if (triangleXZ.isClockwise()) {
				trianglesXYZ.add(new TriangleXYZ(v3, v2, v1));
			} else  {
				trianglesXYZ.add(new TriangleXYZ(v1, v2, v3));
			}
		}

		return trianglesXYZ;

	}

}
