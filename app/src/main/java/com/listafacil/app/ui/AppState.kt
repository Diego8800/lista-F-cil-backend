package com.listafacil.app.ui

import com.listafacil.app.domain.model.ShoppingList
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Lista ativa atual, compartilhada entre telas (menu lateral contextual). */
@Singleton
class ActiveListHolder @Inject constructor() {
    private val _activeList = MutableStateFlow<ShoppingList?>(null)
    val activeList: StateFlow<ShoppingList?> = _activeList

    fun set(list: ShoppingList?) {
        _activeList.value = list
    }
}
