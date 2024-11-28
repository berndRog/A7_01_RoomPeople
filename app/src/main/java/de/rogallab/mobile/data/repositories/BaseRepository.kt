package de.rogallab.mobile.data.repositories

import de.rogallab.mobile.data.local.IBaseDao
import de.rogallab.mobile.domain.IBaseRepository
import de.rogallab.mobile.domain.ResultData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

open class BaseRepository<T, Dto, DAO : IBaseDao<Dto>>(
   private val _dao: DAO,
   private val _coroutineDispatcher: CoroutineDispatcher,
   private val transformToDto: (T) -> Dto,
): IBaseRepository<T> {

   override suspend fun create(entity: T): ResultData<Unit> =
      withContext(_coroutineDispatcher) {
         return@withContext try {
            val dto: Dto = transformToDto(entity)
            _dao.insert( dto )
            ResultData.Success(Unit)
         } catch (t: Throwable) {
            ResultData.Error(t)
         }
      }

   override suspend fun create(entities: List<T>): ResultData<Unit> =
      withContext(_coroutineDispatcher) {
         return@withContext try {
            val dtos: List<Dto> = entities.map { it: T -> transformToDto(it) }
            _dao.insert( dtos )
            ResultData.Success(Unit)
         } catch (t: Throwable) {
            ResultData.Error(t)
         }
      }

   override suspend fun update(entity: T): ResultData<Unit> =
      withContext(_coroutineDispatcher) {
         return@withContext try {
            val dto: Dto = transformToDto(entity)
            _dao.update( dto )
            ResultData.Success(Unit)
         } catch (t: Throwable) {
            ResultData.Error(t)
         }
      }

   override suspend fun remove(entity: T): ResultData<Unit> =
      withContext(_coroutineDispatcher) {
         return@withContext try {
            val dto: Dto = transformToDto(entity)
            _dao.delete( dto )
            ResultData.Success(Unit)
         } catch (t: Throwable) {
            ResultData.Error(t)
         }
      }
}