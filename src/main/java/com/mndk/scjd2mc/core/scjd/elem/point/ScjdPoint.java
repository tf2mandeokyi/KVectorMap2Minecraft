package com.mndk.scjd2mc.core.scjd.elem.point;

import com.mndk.scjd2mc.core.scjd.SuchijidoUtils;
import com.mndk.scjd2mc.core.scjd.elem.ScjdElement;
import com.mndk.scjd2mc.core.scjd.elem.ScjdLayer;
import com.mndk.scjd2mc.core.scjd.type.ElementGeometryType;
import com.mndk.scjd2mc.core.scjd.type.ElementStyleSelector;
import com.mndk.scjd2mc.core.scjd.type.ElementStyleSelector.ScjdElementStyle;
import com.mndk.scjd2mc.core.util.math.Vector2DH;
import com.mndk.scjd2mc.core.util.shape.BoundingBoxDouble;
import com.mndk.scjd2mc.core.util.shape.TriangleList;
import com.sk89q.worldedit.regions.FlatRegion;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

public class ScjdPoint extends ScjdElement {

	private final Vector2DH point;

	public ScjdPoint(ScjdLayer layer, String id, Vector2DH point, Map<String, Object> dataRow) {
		super(layer, id, dataRow, ElementGeometryType.POINT);
		this.point = point;
		this.bbox = new BoundingBoxDouble(point.x, point.z, point.x, point.z);
	}

	public ScjdPoint(ScjdLayer layer, String id, Vector2DH point, Object[] dataRow) {
		super(layer, id, dataRow, ElementGeometryType.POINT);
		this.point = point;
		this.bbox = new BoundingBoxDouble(point.x, point.z, point.x, point.z);
	}
	
	public Vector2DH getPosition() {
		return this.point;
	}

	@Override
	public void generateBlocks(FlatRegion region, World world, TriangleList triangles) {

		ScjdElementStyle[] styles = ElementStyleSelector.getStyle(this);
		if(styles == null) return;
		for(ScjdElementStyle style : styles) {
			if(style == null) return; if(style.state == null) return;
			
			if(region.contains(point.withHeight(region.getMinimumY()).toIntegerWorldEditVector())) {
				
				Vector2DH p = point.withHeight(triangles.interpolateHeight((int) Math.floor(point.x), (int) Math.floor(point.z)) + style.y);
				SuchijidoUtils.setBlock(world, new BlockPos(p.x, p.height, p.z), style.state);
			}
		}
	}

	@Override
	protected Map<String, Object> getSerializableMapGeometryData() {
		Map<String, Object> result = new HashMap<>();
		result.put("type", "Point");
		result.put("coordinates", this.point.toRoundDoubleList());
		return result;
	}


	@Override
	public String toString() {
		return "VMapPoint{type=" + parent.getType() + ",pos=" + this.point + "}";
	}
	
}