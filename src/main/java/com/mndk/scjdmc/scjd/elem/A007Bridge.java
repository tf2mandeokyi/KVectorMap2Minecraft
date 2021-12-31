package com.mndk.scjdmc.scjd.elem;

import com.mndk.scjdmc.scjd.Column;
import com.mndk.scjdmc.scjd.ScjdDefaultElement;
import org.opengis.feature.simple.SimpleFeature;

public class A007Bridge extends ScjdDefaultElement {

    @Column(jsonName = "man_made")
    public final String manMade = "bridge";

    @Column(jsonName = "layer")
    public final int layer = 1;

    public A007Bridge(SimpleFeature feature) {
        super(feature);
    }
}
