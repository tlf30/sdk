/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.scenecomposer.gizmo.light;

import com.jme3.bounding.BoundingSphere;
import com.jme3.bounding.BoundingVolume;
import com.jme3.gde.core.sceneexplorer.nodes.JmeDirectionalLight;
import com.jme3.gde.scenecomposer.gizmo.NodeCallback;
import com.jme3.light.DirectionalLight;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

/**
 *
 * @author dokthar
 */
public class DirectionalLightGizmo extends NodeCallback {

    private Vector3f initalDirection;
    private JmeDirectionalLight jmeLight;
    private DirectionalLight light;

    public DirectionalLightGizmo(JmeDirectionalLight jmelight) {
        super("directional light gizmo", true, true, false);
        jmeLight = jmelight;
        light = jmeLight.getLookup().lookup(DirectionalLight.class);
        initalDirection = light.getDirection().clone();
    }

    @Override
    public void onTranslation(Vector3f oldTranslation, Vector3f newTranslation) {
    }

    @Override
    public void onResize(Vector3f oldScale, Vector3f newScale) {
    }

    @Override
    public void onRotation(Quaternion oldRotation, Quaternion newRotation) {
        light.setDirection(newRotation.mult(initalDirection));
        jmeLight.setValue("direction", light.getDirection());
    }

    private final BoundingSphere bv = new BoundingSphere(10f, getWorldTranslation());

    @Override
    public BoundingVolume getWorldBound() {
        return bv;
    }

}
