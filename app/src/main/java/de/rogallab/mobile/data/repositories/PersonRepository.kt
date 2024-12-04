package de.rogallab.mobile.data.repositories

import de.rogallab.mobile.data.local.IPersonDao
import de.rogallab.mobile.data.dtos.PersonDto
import de.rogallab.mobile.data.mapping.toPerson
import de.rogallab.mobile.data.mapping.toPersonDto
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.ResultData
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.domain.utilities.logDebug
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class PersonRepository(
   private val _personDao: IPersonDao,
   private val _coroutineDispatcher: CoroutineDispatcher
): IPersonRepository {

   override fun selectAll(): Flow<ResultData<List<Person>>> = flow {
      try {
         _personDao.selectAll().collect { personDtos: List<PersonDto> ->
            logDebug(TAG, "getAll: ${personDtos.size}")
            val people: List<Person> = personDtos.map { it.toPerson() }
            //throw RuntimeException("getAll() failed")

            emit(ResultData.Success(people))
         }
      } catch (t: Throwable) {
         emit(ResultData.Error(t))
      }
   }.flowOn(_coroutineDispatcher)

   override suspend fun findById(id: String): ResultData<Person?> =
      withContext(_coroutineDispatcher) {
         return@withContext try {
            logDebug(TAG, "getById()")
            val personDto: PersonDto? = _personDao.findById(id)
            val person: Person? = personDto?.toPerson()
            ResultData.Success(person)
         } catch (t: Throwable) {
            ResultData.Error(t)
         }
      }

   override suspend fun count(): ResultData<Int> =
      withContext(_coroutineDispatcher) {
         return@withContext try {
            ResultData.Success(_personDao.count())
         } catch (t: Throwable) {
            ResultData.Error(t)
         }
      }

   override suspend fun insert(person: Person): ResultData<Unit> =
      withContext(_coroutineDispatcher) {
         return@withContext try {
            logDebug(TAG, "create()")
            val person = person.toPersonDto()
            _personDao.insert(person)
            ResultData.Success(Unit)
         } catch (t: Throwable) {
            ResultData.Error(t)
         }
      }

   override suspend fun insert(people: List<Person>): ResultData<Unit> =
      withContext(_coroutineDispatcher) {
         return@withContext try {
            logDebug(TAG, "create()")
            val personDtos = people.map { it.toPersonDto() }
            _personDao.insert(personDtos)
            ResultData.Success(Unit)
         } catch (t: Throwable) {
            ResultData.Error(t)
         }
      }

   override suspend fun update(person: Person): ResultData<Unit> =
      withContext(_coroutineDispatcher) {
         return@withContext try {
            logDebug(TAG, "update()")
            val personDto = person.toPersonDto()
            _personDao.update(personDto)
            ResultData.Success(Unit)
         } catch (t: Throwable) {
            ResultData.Error(t)
         }
      }

   override suspend fun remove(person: Person): ResultData<Unit> =
      withContext(_coroutineDispatcher) {
         return@withContext try {
            logDebug(TAG, "remove()")
            val personDto = person.toPersonDto()
            _personDao.remove(personDto)
            ResultData.Success(Unit)
         } catch (t: Throwable) {
            ResultData.Error(t)
         }
      }

   companion object {
      private const val TAG = "<-PersonRepository"
   }
}