package de.rogallab.mobile.data.dtos

import androidx.room.Entity
import androidx.room.PrimaryKey
import de.rogallab.mobile.domain.utilities.newUuid

@Entity(
   tableName="Person"
)
data class PersonDto (
   val firstName: String = "",
   val lastName: String = "",
   val email: String? = null,
   val phone: String? = null,
   val imagePath: String? = null,
   @PrimaryKey
   val id: String = newUuid()  // Uuid
)