/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.scenecomposer.gizmo.light;

import com.jme3.gde.scenecomposer.gizmo.NodeCallback;
import com.jme3.light.Light;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.openide.util.Exceptions;

/**
 *
 * @author dokthar
 */
public class LightDirectionUpdate extends AbstractControl {

    private Light light;
    private NodeCallback gizmo;

    private final Vector3f lastDir = new Vector3f();
    private Vector3f lightDir;

    public LightDirectionUpdate(Light l, NodeCallback g) {
        gizmo = g;
        light = l;

        try {
            Method getDirection = light.getClass().getMethod("getDirection");
            lightDir = (Vector3f) getDirection.invoke(light);
        } catch (NoSuchMethodException ex) {
            //light type doesn't have a get position method, silancing the exception
        } catch (SecurityException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IllegalAccessException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IllegalArgumentException ex) {
            Exceptions.printStackTrace(ex);
        } catch (InvocationTargetException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    @Override
    protected void controlUpdate(float f) {
        if (!lightDir.equals(lastDir)) {
            lastDir.set(lightDir);

            Vector3f axis = Vector3f.UNIT_Y.cross(lastDir);
            float angle = Vector3f.UNIT_Y.angleBetween(lastDir);
            //Quaternion rotation = gizmo.getWorldRotation().inverse().mult(new Quaternion().fromAngleAxis(angle, axis));
            Quaternion rotation = new Quaternion().fromAngleAxis(angle, axis);
            gizmo.silentLocalRotation(rotation);
        }
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
    }

}
