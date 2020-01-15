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


/**
 * 
 * @author Chris Ainsley
 *
 */
public class ChunkHelper {
   
   private String _staticMemVarPrefix;
   
   private java.util.LinkedHashMap<String, String> _lookups = new java.util.LinkedHashMap<String, String>();
   
   private int _nextId = 0;

   protected ChunkHelper(String staticMemVarPrefix) {
      _staticMemVarPrefix = staticMemVarPrefix;
      _nextId = 0;
   }

   public String getStaticMemVarPrefix() {
      return _staticMemVarPrefix;
   }

   public java.util.Map<String, String> getLookups() {
      return _lookups;
   }

   public String getNextMemVarId() {
      return _staticMemVarPrefix + (_nextId++);
      
   }
   
   public void register(String id, String quotedText) {
      _lookups.put(id, quotedText);
   }
   
}