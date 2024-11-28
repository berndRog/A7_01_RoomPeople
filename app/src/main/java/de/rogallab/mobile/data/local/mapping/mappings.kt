package de.rogallab.mobile.data.local.mapping

import de.rogallab.mobile.data.local.dtos.PersonDto
import de.rogallab.mobile.domain.entities.Person

fun PersonDto.toPerson(): Person = Person(
   firstName = firstName,
   lastName = lastName,
   email = email,
   phone = phone,
   imagePath = imagePath,
   id = id
)
fun Person.toPersonDto(): PersonDto = PersonDto(
   firstName = firstName,
   lastName = lastName,
   email = email,
   phone = phone,
   imagePath = imagePath,
   id = id
)


