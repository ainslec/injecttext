/*
 * Copyright 2019, Chris Ainsley
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ainslec.injecttext;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * A simple Maven code generation plugin for reading in a UTF-8 encoded text file, and
 * embedding the content of the file inside a Java class as a String Literal.
 * @author Chris Ainsley
 *
 */
@Mojo( name = "generate", defaultPhase=LifecyclePhase.GENERATE_SOURCES)
public class InjectTextMavenPlugin extends AbstractMojo {
   
   private static final String UTF_8         = "UTF-8";
   private static final String OFF           = "off";
   private static final String SRC_GEN       = "src-gen";
   private static final String JAVA_SUFFIX   = ".java";

   @Parameter(required = true)
   private File inputFile;
   
   @Parameter(defaultValue = "${basedir}/src-gen/main/java")
   private File srcGenFolderBase;

   @Parameter(required = true)
   private String packageName;
   
   @Parameter(required = true)
   private String className;
   
   @Parameter(defaultValue = "on")
   private String safeMode;
   
   @Parameter(defaultValue = "T")
   private String constantName;

   @Override
   public void execute() throws MojoExecutionException, MojoFailureException {
      
      if (inputFile.exists() == false || inputFile.isFile() == false) {
         throw new MojoExecutionException("'inputFile' does not point to a valid file : " + inputFile.getAbsolutePath());
      }
      
      if (srcGenFolderBase.exists() == false || srcGenFolderBase.isFile()) {
         throw new MojoExecutionException("'srcGenFolderBase' does not point to a valid folder : " + srcGenFolderBase.getAbsolutePath());
      }
      
      boolean isSafeMode = OFF.equals(safeMode) == false;
      
      String canonicalPath = "";
      
      try {
         canonicalPath = srcGenFolderBase.getCanonicalPath();
      } catch (IOException e2) {
         throw new MojoExecutionException("Could not calculate canonical path for : " + srcGenFolderBase.getAbsolutePath());
      }
      
      if (isSafeMode && canonicalPath.contains(SRC_GEN) == false) {
         System.out.println("Skipping code generation as safeMode is enabled, and 'srcGenFolderBase' does not contain '"+SRC_GEN+"' in the path.");
         return;
      }

      className = className.trim();

      // We add this in later, so remove it if the plugin configurer accidentally adds it.
      if (className.endsWith(JAVA_SUFFIX)) {
         className = className.substring(0, className.length() - JAVA_SUFFIX.length());
      }
      
      if (className.trim().length() == 0) {
         throw new MojoExecutionException("Classname must be a non empty string.");
      }

      // TODO :: Not testing for Java language validity here.
      if (constantName.trim().length() == 0) {
         throw new MojoExecutionException("'constantName' must be non empty.");
      }
      
      // TODO :: We could validate that the Classname is a valid Java classname, and doesn't contain special characters here.
      
      final Charset utf8Charset  = Charset.forName(UTF_8);
      final Path inputFilePath   = inputFile.toPath();
      
      byte[] inputFileBytes;
      
      try {
         inputFileBytes = Files.readAllBytes(inputFilePath);
      } catch (IOException e1) {
         throw new MojoExecutionException("Problem reading file : " + inputFile.getAbsolutePath(),e1);
      }
      
      String inputFileContent = new String (inputFileBytes, utf8Charset); 
      
      // NOTE :: This currently is not intended to be used for very large files (greater than 5 megabytes)
      //         as it's not a streaming type encoding. Currently just a simple implementation.
      String classText = InjectText.createClassForStaticText(packageName, className, constantName, inputFileContent);

      
      final String packageFolderOffset = packageName.replace('.', '/').replace('\\', '/');
      File fileCreationFolder = new File (srcGenFolderBase, packageFolderOffset);
      
      if (fileCreationFolder.exists() == false) {
         boolean createdFolder = fileCreationFolder.mkdirs();
         
         if (!createdFolder) {
            throw new MojoExecutionException("Could not create folder : " + fileCreationFolder.getAbsolutePath());
         }
      }
      
      if (fileCreationFolder.isDirectory() == false) {
         throw new MojoExecutionException("Trying to create folder but file already exists with path : " + fileCreationFolder.getAbsolutePath());
      }
      
      File outputFile = new File(fileCreationFolder, className + JAVA_SUFFIX);
      
      // We overwrite files by default in this plugin, so must be very careful with parameters.
      
      final Path outputFilePath = outputFile.toPath();
      final byte[] classFileTextAsUtfBytes = classText.getBytes(utf8Charset);
      
      try {
         Files.write(outputFilePath, classFileTextAsUtfBytes);
      } catch (Exception e) {
         throw new MojoExecutionException("Problem generating .java file : " + outputFile.getAbsolutePath(),e);
      }
      
      System.out.println("Created : " + outputFile.getAbsolutePath());
      
   }

}
