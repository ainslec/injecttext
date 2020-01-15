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

import java.util.Map;

/**
 * A simple class to inject a large piece of text into a Java class, so it can be accessed at runtime without 
 * scanning the classpath or reflection.
 * <br><br>
 * The initial use-case of this is for GWT related injection.
 * @author Chris Ainsley
 *
 */
public class InjectText {
   
   // We set this low so that there is enough space for worst case encoding byte cost.
   // This is probably overkill, but can be tweaked later to proper amount.
   public static final int    SAFE_CHARLIMIT    = (65535 - 5) / 6;
   
   /** 
    * 
    * @param packageNameOptional If non null (and classNameOptional non null), then generate an outer class for the injection code.
    * @param classNameOptional If non null (and packageNameOptional non null), then generate an outer class for the injection code.
    * @param prefix The prefix for the member variable, this will also be used for member variables with sub-parts of the full injected text
    * @param content The textual content to embed in the static member variable (usually read from a UTF-8 encoded file).
    * @return Returns either lines of code corresponding to the string specified in the 'content' parameter, and if packageNameOptional and classNameOptional parameters are also provided, then returns the entire text of an outer classfile to contain the static member variable initialization code. 
    */
   public static String createClassForStaticText(String packageNameOptional, String classNameOptional, String prefix, String content) {
      
      StringBuilder sb = new StringBuilder();

      final boolean exportingClassFrame = packageNameOptional != null && classNameOptional != null && packageNameOptional.trim().length() > 0 && classNameOptional.trim().length() > 0;
      if (exportingClassFrame) {
         sb.append("package " + packageNameOptional + ";\n\n");
         sb.append("public class " + classNameOptional + " {\n\n");
      }
      
      prefix = prefix == null || prefix.trim().length() == 0 ? "F" : prefix.trim();
      
      // Note : Not truly sanitizing here. Possible to provide a prefix that does not adhere to java rules
      ChunkHelper chunkHelper = new ChunkHelper(prefix);
      String safe64KSplitLiteral = embedSafeStaticJavaStringConstantThatOvercomes64Limit(0, content, chunkHelper );
      
      final Map<String, String> lookups = chunkHelper.getLookups();
      final String stringKeyword = String.class.getName();
      
      for (String x : lookups.keySet()) {
         final String currentText   = lookups.get(x);
         sb.append("   private static " + stringKeyword + " " + x + " = "+currentText+";\n");
      }
      sb.append("public static final String "+prefix+" = " + safe64KSplitLiteral + ";\n");
      
      if (exportingClassFrame) {
         sb.append("\n}\n");
      }
      
      return sb.toString();
      
   }
   
   public static String embedSafeStaticJavaStringConstantThatOvercomes64Limit(int indent, String literalText, ChunkHelper chunkHelper) {
      
      
      StringBuilder sb      = new StringBuilder();
      
      /**
       * Java cannot specify a string constant in a class longer than 64 kilobytes.
       */
      
      if (indent > 0) {
         for (int i=0; i < indent; i++) {
            sb.append(" ");
         }
      }
      
      int literalTextLength = literalText.length();

      if (literalTextLength <= SAFE_CHARLIMIT) {
         sb.append("\"" + escapeJavaLiteralText(literalText)+ "\"");
      } else {

         sb.append("/* 64KB byte limit workaround via appending large strings */ ");
         
         final int finalBlockLength = literalTextLength % SAFE_CHARLIMIT;
         int numberOfBlocks         = (literalTextLength / SAFE_CHARLIMIT) + ( ( finalBlockLength > 0 ) ? 1 : 0 );
         for (int i=0 ; i < numberOfBlocks; i++) {
            int startIndex = SAFE_CHARLIMIT * i;
            boolean isFinalBlock = (i+1) == numberOfBlocks;
            String chunk  = literalText.substring(startIndex, startIndex + (isFinalBlock ? finalBlockLength : SAFE_CHARLIMIT));
            String nextId =   chunkHelper.getNextMemVarId();
            sb.append(nextId);
            chunkHelper.register(nextId, "\"" + escapeJavaLiteralText(chunk)+ "\"");
            if (!isFinalBlock) {
               sb.append(" + ");
            } else {
            }
         }
      }
      
      final String retVal = sb.toString();
      
      return retVal;
   }
   
   /**
    * A simple method to escape string that will appear in Java literal strings
    * @param input An input string
    * @return If input is null, returns null, otherwise returns escaped version of supplied input string
    */
   public static String escapeJavaLiteralText(String input) {
      
      // This code adapted from code in the 'Rion' project by same author but this specific snippet
      // is published here under Apache version 2 license.
      
      if (input == null) {
         return null;
      } else {
         final int numChars = input.length();
         StringBuilder sb = new StringBuilder();
         for (int i=0; i < numChars; i++) {
            char currentChar = input.charAt(i);
            if ('\\' == currentChar) {
               sb.append("\\\\");
            } else if ('\"' == currentChar) {
               sb.append("\\\"");
            } else if ('\n' == currentChar) {
               sb.append("\\n");
            } else if ('\r' == currentChar) {
               sb.append("\\r");
            } else if ('\t' == currentChar) {
               sb.append("\\t");
            } else if (currentChar < ' ' || currentChar > 126) {
               final char ch = currentChar;
               int a = (ch & 61440) >> 12;
               int b = (ch & 3840) >> 8;
               int c = (ch & 240) >> 4;
               int d = (ch & 15);
               sb.append("\\u");
               sb.append((char)(a < 10 ? ((char)('0' + a)) : ((char)(87 + a))) );
               sb.append((char)(b < 10 ? ((char)('0' + b)) : ((char)(87 + b))) );
               sb.append((char)(c < 10 ? ((char)('0' + c)) : ((char)(87 + c))) );
               sb.append((char)(d < 10 ? ((char)('0' + d)) : ((char)(87 + d))) );
            } else {
               sb.append(currentChar);
            }
         }
         return sb.toString();
      }
   }
   

}
