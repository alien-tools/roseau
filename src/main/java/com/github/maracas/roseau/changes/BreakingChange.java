 package com.github.maracas.roseau.changes;
 import com.github.maracas.roseau.model.TypeDeclaration;

 /**
  * Represents a breaking change identified during the comparison of APIs between the two library versions.
  * This class encapsulates information about the breaking change's kind, position, nature, and more.
  */
 public class BreakingChange {
     /**
      * The kind of the breaking change.
      */
     public BreakingChangeKind breakingChangeKind;

     /**
      * The type in which the breaking change is located.
      */
     public TypeDeclaration breakingChangeTypeDeclaration;

     /**
      * The exact position of the breaking change.
      */
     public String breakingChangePosition;

     /**
      * The nature of the breaking change ( Addition / deletion / mutation ).
      */
     public BreakingChangeNature breakingChangeNature;


     public BreakingChange(BreakingChangeKind breakingChangeKind,
                           TypeDeclaration breakingChangeTypeDeclaration,
                           String breakingChangePosition,
                           BreakingChangeNature breakingChangeNature) {
         this.breakingChangeKind = breakingChangeKind;
         this.breakingChangeTypeDeclaration = breakingChangeTypeDeclaration;
         this.breakingChangePosition = breakingChangePosition;
         this.breakingChangeNature = breakingChangeNature;
     }

     /**
      * Retrieves the kind of the breaking change.
      *
      * @return Breaking change's kind
      */
     public BreakingChangeKind getBreakingChangeKind() {
         return breakingChangeKind;
     }

     /**
      * Retrieves the type declaration in which the breaking change is located.
      *
      * @return Breaking change's type declaration
      */
     public TypeDeclaration getBreakingChangeTypeDeclaration() {
         return breakingChangeTypeDeclaration;
     }

     /**
      * Retrieves the position of the breaking change.
      *
      * @return Breaking change's position
      */
     public String getBreakingChangePosition() {
         return breakingChangePosition;
     }

     /**
      * Retrieves the nature of the breaking change (Addition / Deletion / Mutation).
      *
      * @return Breaking change's nature
      */
     public BreakingChangeNature getBreakingChangeNature() {
         return breakingChangeNature;
     }

 }
