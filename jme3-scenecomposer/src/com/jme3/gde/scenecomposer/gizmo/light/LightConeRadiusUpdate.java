/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.scenecomposer.gizmo.light;

import com.jme3.light.SpotLight;
import com.jme3.math.FastMath;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.control.AbstractControl;

/**
 *
 * @author dokthar
 */
public class LightConeRadiusUpdate extends AbstractControl {

    private SpotLight light;
    private float lastInnerAngle = -1;
    private float lastOuterAngle = -1;
    private float lastRange = -1;
    private Geometry inner, outer;

    public LightConeRadiusUpdate(SpotLight l, Geometry inner, Geometry outer) {
        this.light = l;
        this.inner = inner;
        this.outer = outer;

        light.getSpotInnerAngle();
        light.getSpotOuterAngle();
    }

    private static float oppositeSide(float angle, float adjacent) {
        // tan(angle) = opposite / adjacent
        return FastMath.tan(angle) * adjacent;
    }

    @Override
    protected void controlUpdate(float f) {
        float a = light.getSpotInnerAngle();
        if (a != lastInnerAngle) {
            lastInnerAngle = a;
            float r = oppositeSide(lastInnerAngle, light.getSpotRange());
            inner.setLocalScale(r, r, r);
        }

        a = light.getSpotOuterAngle();
        if (a != lastOuterAngle) {
            lastOuterAngle = a;
            float r = oppositeSide(lastOuterAngle, light.getSpotRange());
            outer.setLocalScale(r, r, r);
            if (getSpatial() != null) {
                spatial.setLocalScale(r, spatial.getLocalScale().y, 1);
            }
        }

        a = light.getSpotRange();
        if (a != lastRange) {
            lastRange = a;
        }
        if (getSpatial() != null) {
            spatial.setLocalScale(spatial.getLocalScale().x, light.getSpotRange(), 1);
        }
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
    }

}
