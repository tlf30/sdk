/*
 *  Copyright (c) 2009-2016 jMonkeyEngine
 *  All rights reserved.
 * 
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are
 *  met:
 * 
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 
 *  * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *  TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 *  PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 *  CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *  PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.gde.core.sceneexplorer.nodes;

import com.jme3.cinematic.MotionPath;
import com.jme3.gde.core.scene.SceneApplication;
import com.jme3.gde.core.scene.controller.SceneToolController;
import com.jme3.gde.core.sceneexplorer.SceneExplorerTopComponent;
import com.jme3.math.Vector3f;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.openide.loaders.DataObject;
import org.openide.nodes.Children;
import org.openide.nodes.Index;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;

/**
 * This Class is responsible for the management of all underlying Vector3fs as Nodes<br>
 * (In fact we currently only have one possible MotionPath but we keep this to be consistent with JmeBoneChildren).<br>
 * You have to ensure that you set the appropriate JmeMotionEvent for this class (this happens when JmeMotionEvent is creating it's Nodes)<br>
 * It will use this class as Children (which are JmeMotionPaths)<br>
 * @author MeFisto94
 */
public class JmeVector3fChildren extends Children.Keys<Vector3f> implements Index {

    protected JmeMotionPath jmeMotionPath;
    protected boolean readOnly = true;
    
    protected List<Vector3f> keys;
    protected LinkedList<ChangeListener> listeners = new LinkedList<ChangeListener>();
    private DataObject dataObject;

    public JmeVector3fChildren() {
    }

    public JmeVector3fChildren(JmeMotionPath jmeMotionPath) {
        this.jmeMotionPath = jmeMotionPath;
    }

    public void refreshChildren(boolean immediate) {
        keys = createKeys();
        setKeys(keys);
        refresh();
    }

    public void setReadOnly(boolean cookie) {
        this.readOnly = cookie;
    }

    @Override
    protected void addNotify() {
        super.addNotify();
        keys = createKeys();
        setKeys(keys);
    }

    protected List<Vector3f> createKeys() {
        try {
            return SceneApplication.getApplication().enqueue(new Callable<List<Vector3f>>() {

                @Override
                public List<Vector3f> call() throws Exception {
                    List<Vector3f> keys = new LinkedList<Vector3f>();
                    if (jmeMotionPath.getMotionPath() != null) {
                        for (int i = 0; i <jmeMotionPath.getMotionPath().getNbWayPoints(); i++) {
                            keys.add(jmeMotionPath.getMotionPath().getWayPoint(i));
                        }
                    }
                    
                    return keys;
                }
            }).get();
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        } catch (ExecutionException ex) {
            Exceptions.printStackTrace(ex);
        }
        return null;
    }
    
    @Override
    protected Node[] createNodes(Vector3f key) {
        Node n = new JmeVector3f(this, jmeMotionPath, key).setReadOnly(readOnly);
        n.setDisplayName("Waypoint " + keys.indexOf(key));
        return new Node[]{ n };
    }

    /**
     * This Method is used because before createNodes takes place we are called by the default constructor and have to pass things...
     * @param jmeMotionPath The JmeMotionPath instance which is this nodes parent. 
     */
    public void setJmeMotionPath(JmeMotionPath jmeMotionPath) {
        this.jmeMotionPath = jmeMotionPath;
    }

    public DataObject getDataObject() {
        return dataObject;
    }
    public void setDataObject(DataObject dataObject) {
        this.dataObject = dataObject;
    }

    @Override
    public int indexOf(Node node) {
        Vector3f key = node.getLookup().lookup(Vector3f.class); // reverseLookup.get(node);
        
        if (key == null)
            return -1;
        
        return keys.indexOf(key); // For this to work, keys has to have ALWAYS be in sync for EVERY Change to the MotionPath.Waypoints
    }

    /**
     * &quot;Invoke a dialog for reordering the children.&quot;
     */
    @Override
    public void reorder() {
        // We wont...
    }

    /**
     * Reorder all children with a given permutation.
     * TODO: Test this....
     * @param perm - permutation with the length of current nodes. The permutation lists the new positions of the original nodes, that is, for nodes [A,B,C,D] and permutation [0,3,1,2], the final order would be [A,C,D,B].
     * @throws IllegalArgumentException - if the permutation is not valid
     */
    @Override
    public void reorder(int[] perm) {
        List<Vector3f> oldKeys = Arrays.asList((Vector3f[])keys.toArray());
        
        for (int i = 0; i < perm.length; i++) {
            keys.get(i).set(oldKeys.get(perm[i]));
        }
        
        triggerChangeListeners();
        
        for (Node n: getNodes()) {
            ((JmeVector3f)n).moveBox();
        }
    }

    /**
     * Move the element at the x-th position to the y-th position. All elements after the y-th position are moved down.
     * @param x the position to remove the element from
     * @param y the position to insert the element to
     */
    @Override
    public void move(int x, int y) {
        MotionPath m = jmeMotionPath.getMotionPath();
        /* TODO: Check and Implement Method correctly. */
        if (x < y) { /* X above Y, means: remove x, shift everything until y up, move everything from y one down. set x to y's value. */
            Vector3f v_x = keys.get(x).clone(); // We can't use remove() because of the MotionPaths
            for (int i = x+1; i <= y - 1; i++) {
                moveUp(i); // This causes quite some rendering updates and messes up the selection, but when it's there, we use it.
            }
            
            for (int i = keys.size() - 2; i >= y; i--) {
                moveDown(i);
            }
            
            keys.get(y).set(v_x);
        } else { /* X below Y, means: remove x, shift everything until end up, move everyting from x one down. set y to x's value */
            // TODO IMPLEMENT
        }
        
        triggerChangeListeners();
    }

    /**
     * Exchange two elements.
     * @param x Position of the first Element
     * @param y Position of the second Element
     */
    @Override
    public void exchange(int x, int y) {
        
        /**
         * There are two ways to achieve this Exchange. Keep in mind we actually have two lists:
         * We have the "keys" and the motionPaths Waypoints (controlPoints). In order to exchange, we have:
         * 
         * Method A: Exchanging the Vector3f Reference in BOTH keys and motionPaths (this basically means reordering them, really!)
         *           This is problematic, because MotionPath doesn't support accessing it's list.
         * 
         * Method B: Exchanging the Vector3f's Values. This means The upper Node (Waypoint) will still be the upper, but it's contents just change
         *           This is easy to achieve using Vector3f#set(). The downside is that that since the keys have not been altered, they aren't recreated.
         *           We will just call the apropriate update Methods for the Visual Representations.
         *           Note: I don't know yet what netbeans thinks of Method B. Probably we need ugly reflection and Method A.
         */
        
        /* Clone because the contents will be altered */
        Vector3f v_x = keys.get(x).clone();
        Vector3f v_y = keys.get(y).clone();
        
        // Exchange keys
        /* METHOD A:
        //keys.set(x, v_y);
        //keys.set(y, v_x);*/
        
        keys.get(x).set(v_y);
        keys.get(y).set(v_x);
        
        MotionPath m = jmeMotionPath.getMotionPath();
        
        // Exchange Waypoints
        // This only works because waypoint y == keys.y (v_y). If this isn't true we messed up somewhere, really hard.
        // it would mean that the saved Motion Path derives from the shown...
        
        m.getWayPoint(x).set(v_y);
        m.getWayPoint(y).set(v_x);
        
        
        /* Let the nodes know that their content has been changed */
        ((JmeVector3f)getNodeAt(x)).moveBox();
        ((JmeVector3f)getNodeAt(y)).moveBox();
        //jmeMotionPath.updateSpline(true);// MoveBox calls UpdateSpline
        
        // Netbeans API wants that.
        triggerChangeListeners();
        SceneExplorerTopComponent.findInstance().setSelectedNode((AbstractSceneExplorerNode)getNodeAt(y)); // Speak: "Replace X with Y"
    }

    /**
     * Move an element up
     * @param i index of element to move up
     */
    @Override
    public void moveUp(int i) {
        if (i <= 0 || i >= keys.size()) // Can't move up
            throw new IndexOutOfBoundsException(); 
        
        exchange(i, i-1); // Clever Code reusing, huh? ;)
    }

    /**
     * Move an element down
     * @param i index of element to move down
     */
    @Override
    public void moveDown(int i) {
        if (i < 0 || i >= keys.size() - 1)
            throw new IndexOutOfBoundsException();
        
        exchange(i, i+1); // moveUp(i+1); wouldn't respect the setSelectedNode
    }
    
    private void triggerChangeListeners() {
        for (ChangeListener cl : listeners) {
            cl.stateChanged(new ChangeEvent(this));
        }
    }

    @Override
    public void addChangeListener(ChangeListener cl) {
        listeners.add(cl);
    }

    @Override
    public void removeChangeListener(ChangeListener cl) {
        listeners.remove(cl);
    }
}
