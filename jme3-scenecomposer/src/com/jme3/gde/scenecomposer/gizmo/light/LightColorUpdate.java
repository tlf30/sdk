/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.scenecomposer.gizmo.light;

import com.jme3.light.Light;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;

/**
 *
 * @author dokthar
 */
public class LightColorUpdate extends AbstractControl {

    private Light light;
    private final ColorRGBA lastCol = new ColorRGBA();
    private ColorRGBA lightCol;
    private Material mat;
    private String name;

    public LightColorUpdate(Light l, Material mat, String name) {
        light = l;
        lightCol = light.getColor();
        this.mat = mat;
        this.name = name;
        mat.setColor(name, lightCol);
    }

    @Override
    protected void controlUpdate(float f) {
        if (!lightCol.equals(lastCol)) {
            lastCol.set(lightCol);
            mat.setColor(name, lastCol);
        }
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
    }

}
