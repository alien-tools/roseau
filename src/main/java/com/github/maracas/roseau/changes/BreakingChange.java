 package com.github.maracas.roseau.changes;


 import com.github.maracas.roseau.model.TypeDeclaration;

 public class BreakingChange {
     public BreakingChangeKind breakingChangeKind;
     public BreakingChangeElement breakingChangeElement;


     public BreakingChange(BreakingChangeKind breakingChangeKind, BreakingChangeElement breakingChangeElement) {
         this.breakingChangeKind = breakingChangeKind;
         this.breakingChangeElement = breakingChangeElement;
     }

     public BreakingChangeKind getBreakingChangeKind() {
         return breakingChangeKind;
     }

     public BreakingChangeElement getBreakingChangeElement() {
         return breakingChangeElement;
     }

     public void setBreakingChangeKind(BreakingChangeKind breakingChangeKind) {
         this.breakingChangeKind = breakingChangeKind;
     }

     public void setBreakingChangeElement(BreakingChangeElement breakingChangeElement) {
         this.breakingChangeElement = breakingChangeElement;
     }
 }