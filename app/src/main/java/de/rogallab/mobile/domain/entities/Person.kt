package de.rogallab.mobile.domain.entities
import java.util.UUID
import de.rogallab.mobile.domain.utilities.newUuid

data class Person (
   val firstName: String = "",
   val lastName: String = "",
   val email: String? = null,
   val phone: String? = null,
   val imagePath: String? = null,
   val id: String  // Uuid as String

   // Relations to other domainModel classes
)