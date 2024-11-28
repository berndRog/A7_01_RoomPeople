package de.rogallab.mobile.domain

interface IBaseRepository<T> {
   suspend fun create(item: T): ResultData<Unit>
   suspend fun create(items: List<T>): ResultData<Unit>
   suspend fun update(item: T): ResultData<Unit>
   suspend fun remove(item: T): ResultData<Unit>
}