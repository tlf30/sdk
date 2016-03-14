# Maintaining the SDK  
This document is _only_ for developers who intend to maintain the SDK.  
If you don't fulfill this criteria, feel free to skip that part as it won't help you with anything.  
Actually this is not a real document either but simply a collection of links, hints, notes, etc.  

### How to Release a new Version
Since we have automated builds with travis you only have to tag a commit and it will be released for you.  
Keep in mind that I currently have the impression that you can only publish one release at a time.  
The OAUTH Key for this deployment is found in the .travis.yml along with instructions on how to generate such a file. You only need this when I'm (MeFisto94) no longer permitted to push to the repo.  

The build process with the netbeans installers (`ant build-installers`) is a bit fragile especially in Travis Environment (small diskspace, no root (because caching)) so handle with care.  

### What has to be done for a new Version?
Now this part is actually for me so I don't forget it :P  

Change http://wiki.jmonkeyengine.org/doku.php/sdk:welcome:3_1 every tag to keep up with the version number and then save it as nbres:/com/jme3/gde/docs/sdk/welcome/local.html  

### How to Upgrade Dependencies
See `nbi/stub/ext/infra/build/products/README` for now. It will be included in the docs and so the README will only be a link to that.  

See `resources/README.md` and `harness-override/README.md` for how to change the Netbeans Icon (on Windows)  