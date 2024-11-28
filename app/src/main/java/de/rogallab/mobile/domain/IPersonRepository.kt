package de.rogallab.mobile.domain

import de.rogallab.mobile.domain.entities.Person
import kotlinx.coroutines.flow.Flow

interface IPersonRepository {

    fun getAll(): Flow<ResultData<List<Person>>>
    suspend fun getById(id: String): ResultData<Person?>
    suspend fun count(): ResultData<Int>

    suspend fun create(person: Person): ResultData<Unit>
    suspend fun create(people: List<Person>): ResultData<Unit>
    suspend fun update(person: Person): ResultData<Unit>
    suspend fun remove(person: Person): ResultData<Unit>

}