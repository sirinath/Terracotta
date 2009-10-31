
1. License generator is a GUI to generate Terracotta Enterprise license.

The jar files in the lib folder are mostly unchanged. If there are changes to projects
"license-common" or "ent-generate-license", you will need to rebuild license-generator.jar from 
tcbuild:

  a. check out an enterprise branch
  b. go to <branch>/community/code/base
  c. run command "tcbuild dist licensegen enterprise"
  d. license-generator will be created under "build/dist"
  e. copy it over to lib folder of this project to replace the old one 
  
  
2. To run license generator

Start run.sh or run.bat script

Then fill in the needed information for the license in the GUI.

3. This project can also be deployed as Java Webstart. It's currently to be deployed on 
http://kong.terracotta.lan/license-generator

If you want to move it to another domain, modify resources/webstart.jnlp

4. If you replace or add any jar files, please rerun signjars.sh to sign them
