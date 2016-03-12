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
package com.jme3.gde.blender.filetypes;

import com.jme3.asset.BlenderKey;
import com.jme3.gde.blender.BlenderTool;
import com.jme3.gde.blender.ConvertToJ3OAction;
import com.jme3.gde.core.assets.AssetDataNode;
import com.jme3.gde.core.assets.ProjectAssetManager;
import com.jme3.gde.core.assets.SpatialAssetDataObject;
import com.jme3.gde.core.util.Beans;
import com.jme3.gde.core.util.SpatialUtil;
import com.jme3.scene.Spatial;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import javax.swing.Action;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectExistsException;
import org.openide.loaders.MultiDataObject;
import org.openide.loaders.MultiFileLoader;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.lookup.ProxyLookup;

/**
 * The AbstractBlenderImportDataObject handles all possible Filetypes which should be automatically converted by Blender's Importer.
 * You have to extend this class and just set the correct suffix. See: {@link BlenderFbxDataObject}
 * @author normenhansen
 */
public abstract class AbstractBlenderImportDataObject extends SpatialAssetDataObject {

    protected String SUFFIX;

    public AbstractBlenderImportDataObject(FileObject pf, MultiFileLoader loader) throws DataObjectExistsException, IOException {
        super(pf, loader);
    }

    @Override
    public Spatial loadAsset() {
        if (savable != null) {
            return (Spatial) savable;
        }
        ProjectAssetManager mgr = getLookup().lookup(ProjectAssetManager.class);
        if (mgr == null) {
            DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.Message("File is not part of a project!\nCannot load without ProjectAssetManager."));
            return null;
        }
        FileObject outFile = importFile(mgr);
        String assetKeyName = mgr.getRelativeAssetPath(outFile.getPath());
        BlenderKey key = new BlenderKey(assetKeyName);
        Beans.copyProperties(key, getAssetKey());
        try {
            listListener.start();
            Spatial spatial = mgr.loadModel(key);
            replaceFiles();
            listListener.stop();
            SpatialUtil.storeOriginalPathUserData(spatial);
            savable = spatial;
            logger.log(Level.INFO, "Loaded asset {0}", getName());
            return spatial;
        } catch (Exception ex) {
            logger.log(Level.SEVERE, ex.getMessage(), ex);
            Exceptions.printStackTrace(ex);
        } finally {
            try {
                outFile.delete();
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        return null;
    }

    @Override
    public synchronized BlenderKey getAssetKey() {
        if (super.getAssetKey() instanceof BlenderKey) {
            return (BlenderKey) assetKey;
        }
        assetKey = new BlenderKey(super.getAssetKey().getName());
        return (BlenderKey) assetKey;
    }

    protected void replaceFiles() {
        for (int i = 0; i < assetList.size(); i++) {
            FileObject fileObject = assetList.get(i);
            if (fileObject.hasExt(BlenderTool.TEMP_SUFFIX)) {
                assetList.remove(i);
                assetKeyList.remove(i);
                assetList.add(i, getPrimaryFile());
                assetKeyList.add(getAssetKey());
                return;
            }
        }
    }
    
    /**
     * Use this method to convert this file into a blender model
     * Note: This requires a bit of additional code (creating the PAM) so use the version without parameters if you don't plan to modify the blend further
     * @param mgr The AssetManager which provides access to the Project Files
     * @return The FileObject of the .blend file
     */
    public FileObject importFile(ProjectAssetManager mgr) {
        if (SUFFIX == null) {
            throw new IllegalStateException("Suffix for blender filetype is null! Set SUFFIX = \"sfx\" in constructor!");
        }
        
        //make sure its actually closed and all data gets reloaded
        closeAsset();
        FileObject mainFile = getPrimaryFile();
        
        if (FileUtil.findBrother(mainFile, BlenderTool.TEMP_SUFFIX) != null) {
            logger.log(Level.SEVERE, "Cannot convert " + getName() + " to .blend because there is already a file with this name. Delete it and try again!");
            return null;
        }
        
        if (!BlenderTool.runConversionScript(SUFFIX, mainFile)) {
            logger.log(Level.SEVERE, "Failed to create model, running blender caused an error");
            return null;
        }
        mainFile.getParent().refresh();
        FileObject outFile = FileUtil.findBrother(mainFile, BlenderTool.TEMP_SUFFIX);
        if (outFile == null) {
            logger.log(Level.SEVERE, "Failed to create model, blend file cannot be found");
            return null;
        }
        int i = 1;
        FileObject blend1File = FileUtil.findBrother(mainFile, BlenderTool.TEMP_SUFFIX + i);
        while (blend1File != null) {
            try {
                blend1File.delete();
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
            i++;
            blend1File = FileUtil.findBrother(mainFile, BlenderTool.TEMP_SUFFIX + i);
        }
        
        return outFile;
    }
    
    /**
     * Use this method to simply convert any supported file into a .blend file.
     * @return The FileObject for the converted .blend file
     */
    public FileObject importFile() {
        ProjectAssetManager mgr = getLookup().lookup(ProjectAssetManager.class);
        if (mgr == null) {
            DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.Message("File is not part of a project!\nCannot load without ProjectAssetManager."));
            return null;
        }
        FileObject outFile = importFile(mgr);
        return outFile;
    }

    @Override
    protected Node createNodeDelegate() {
        MyAssetDataNode node = new MyAssetDataNode(this, Children.LEAF, new ProxyLookup(getCookieSet().getLookup(), contentLookup));
        node.setIconBaseWithExtension("com/jme3/gde/core/icons/model.gif");
        return node;
    }
    
    private class MyAssetDataNode extends AssetDataNode {
        public MyAssetDataNode(DataObject obj, Children ch) {
            super(obj, ch);
        }

        public MyAssetDataNode(DataObject obj, Children ch, Lookup lookup) {
            super(obj, ch, lookup);
        }

        @Override
        public Action getPreferredAction()
        {
            ArrayList<MultiDataObject> ctx = new ArrayList<MultiDataObject>();
            ctx.add((MultiDataObject)getDataObject());
            return new ConvertToJ3OAction(ctx);
        }   
    }
}
