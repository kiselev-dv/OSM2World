package org.osm2world.core.world.modules.common;

import static java.lang.Math.PI;
import static java.util.Arrays.asList;
import static org.osm2world.core.world.modules.common.WorldModuleParseUtil.parseDirection;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapWay;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.map_data.data.Tag;
import org.osm2world.core.map_data.data.TagSet;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.world.modules.RoadModule;
import org.osm2world.core.world.modules.RoadModule.Road;
import org.osm2world.core.world.modules.TrafficSignModule;

/**
 * A class containing all the necessary information to render a traffic sign.
 */
public class TrafficSignModel {

	/** The {@link TrafficSignType}s on this sign */
	public List<TrafficSignType> types;

	/** The position this TrafficSignModel will be rendered on */
	public VectorXZ position;

	/** The direction the {@link TrafficSignModel#types} are facing */
	public double direction;

	/** The {@link org.osm2world.core.map_data.data.MapNode} the position of this model is relevant to */
	public MapNode node;

	public TrafficSignModel(MapNode node) {
		this.node = node;
	}

	/**
	 * Check if the top (bottom) of the given {@code way} is connected
	 * to another way that contains the same key-value tag. Applicable to
	 * traffic sign mapping cases where signs must be rendered both at
	 * the beginning and the end of the way they apply to
	 *
	 * @param topOrBottom
	 * A boolean value indicating whether to check the
	 * beginning or end of the {@code way}. True for beginning, false for end.
	 */
	public static boolean adjacentWayContains(boolean topOrBottom, MapWay way, String key, String value) {

		//decide whether to check beginning or end of the way
		int index = 0;
		MapNode wayNode;

		if (!topOrBottom) {

			//check the last node of the last segment

			index = way.getWaySegments().size() - 1;

			wayNode = way.getWaySegments().get(index).getEndNode();

		} else {

			//check the first node of the first segment

			wayNode = way.getWaySegments().get(index).getStartNode();
		}

		//if the node is also part of another way
		if (wayNode.getConnectedSegments().size() == 2) {

			for (MapWaySegment segment : wayNode.getConnectedWaySegments()) {

				//if current segment is part of this other way
				if (!segment.getWay().equals(way)) {

					//check if this other way also contains the key-value pair
					MapWay nextWay = segment.getWay();

					if (nextWay.getTags().contains(key, value)) {
						return true;
					} else {
						return false;
					}
				}
			}
		}

		return false;
	}

	/**
	 * Calculate the position this model will be rendered to, based on the
	 * {@code side} of the road it is supposed to be and the width of the road
	 */
	public void calculatePosition(MapWaySegment segment, boolean side, String key, boolean isNode) {

		//if way is not a road
		if (!RoadModule.isRoad(segment.getTags())) {
			this.position = node.getPos();
			return;
		}

		//get the rendered segment's width
		Road r = (Road) segment.getPrimaryRepresentation();

		double roadWidth = r.getWidth();

		//rightNormal() vector will always be orthogonal to the segment, no matter its direction,
		//so we use that to place the signs to the left/right of the way
		VectorXZ rightNormal = segment.getDirection().rightNormal();

		/*
		 * Explicit side tag overwrites all. Applies only to sign mapped on way nodes
		 */
		if (node.getTags().containsKey("side") && isNode) {
			if (node.getTags().getValue("side").equals("right")) {
				this.position = node.getPos().add(rightNormal.mult(roadWidth));
				return;
			} else if (node.getTags().getValue("side").equals("left")) {
				this.position = node.getPos().add(rightNormal.mult(-roadWidth));
				return;
			}
		}

		if (side) {

			if (key.contains("forward")) {
				this.position = node.getPos().add(rightNormal.mult(roadWidth));
				return;
			} else {
				//if backwards
				this.position = node.getPos().add(rightNormal.mult(-roadWidth));
				return;
			}

		} else {

			if (key.contains("forward")) {

				this.position = node.getPos().add(rightNormal.mult(-roadWidth));
				return;
			} else {
				this.position = node.getPos().add(rightNormal.mult(roadWidth));
				return;
			}

		}
	}

	/**
	 * Calculate the rotation angle of the model based
	 * on the direction of {@code segment}. The segment
	 * will always be either the first one of it's way
	 * or the last one.
	 */
	public void calculateDirection(MapWaySegment segment, String key) {

		VectorXZ wayDir = segment.getDirection();

		if (key.contains("forward")) {
			wayDir = wayDir.invert();
			this.direction = wayDir.angle();
		} else {
			this.direction = wayDir.angle();
		}
	}

	/**
	 * Same as {@link #calculateDirection(MapWaySegment, String)} but also parses the node's
	 * tags for the direction specification and takes into account the
	 * highway=give_way/stop special case. To be used on nodes that are part of ways.
	 */
	public void calculateDirection() {

		if (node.getConnectedWaySegments().isEmpty()) {
			System.err.println("Node " + node + " is not part of a way.");
			this.direction = PI;
			return;
		}

		TagSet nodeTags = node.getTags();

		//Direction the way the node is part of, is facing
		VectorXZ wayDir = node.getConnectedWaySegments().get(0).getDirection();

		if (nodeTags.containsKey("direction")) {

			if (!nodeTags.containsAny(asList("direction"), asList("forward", "backward"))) {

				//get direction mapped as angle or cardinal direction
				double dir = parseDirection(nodeTags, PI);
				this.direction = dir;
				return;

			} else {

				if (nodeTags.getValue("direction").equals("backward")) {

					this.direction = wayDir.angle();
					return;
				}

			}

		} else if (nodeTags.containsKey("traffic_sign:forward") || nodeTags.containsKey("traffic_sign:backward")) {

			String regex = "traffic_sign:(forward|backward)";
			Pattern pattern = Pattern.compile(regex);
			Matcher matcher;

			for (Tag tag : nodeTags) {

				matcher = pattern.matcher(tag.key);

				if (matcher.matches()) {

					if (matcher.group(1).equals("backward")) {
						this.direction = wayDir.angle();
						return;
					}
				}
			}
		} else if (nodeTags.containsAny(asList("highway"), asList("give_way", "stop"))) {

			/*
			 * if no direction is specified with any of the above cases and a
			 * sign of type highway=give_way/stop is mapped, calculate model's
			 * direction based on the position of the junction closest to the node
			 */

			if (RoadModule.getConnectedRoads(node, false).size() <= 2
					&& RoadModule.getConnectedRoads(node, false).size() > 0) {

				MapNode closestJunction = TrafficSignModule.findClosestJunction(node);

				if (closestJunction != null) {

					this.direction = (node.getPos().subtract(closestJunction.getPos())).normalize().angle();
					return;
				} else {
					this.direction = wayDir.angle();
				}
			}
		}

		/*
		 * Either traffic_sign:forward or direction=forward is specified or no forward/backward is specified at all.
		 * traffic_sign:forward affects vehicles moving in the same direction as
		 * the way. Therefore they must face the sign so the way's direction is inverted.
		 * Vehicles facing the sign is the most common case so it is set as the default case
		 * as well
		 */
		wayDir = wayDir.invert();
		this.direction = wayDir.angle();
		return;
	}
}