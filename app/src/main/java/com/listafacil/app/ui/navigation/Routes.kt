package com.listafacil.app.ui.navigation

object Routes {
    const val LOGIN = "login"
    const val SIGN_UP = "sign_up"
    const val FORGOT_PASSWORD = "forgot_password"

    const val DASHBOARD = "dashboard"
    const val ACTIVE_LIST = "active_list/{listId}"
    fun activeList(listId: String) = "active_list/$listId"

    const val ADD_ITEM = "add_item/{listId}?itemId={itemId}"
    fun addItem(listId: String, itemId: String? = null) =
        "add_item/$listId" + (itemId?.let { "?itemId=$it" } ?: "")

    const val HISTORY = "history/{productId}"
    fun history(productId: String) = "history/$productId"

    const val FINISHED_LISTS = "finished_lists"
    const val FINISHED_DETAIL = "finished_detail/{listId}"
    fun finishedDetail(listId: String) = "finished_detail/$listId"

    const val REPORTS = "reports"
    const val COMPARATOR = "comparator"
    const val PROFILE = "profile"
    const val ESTABLISHMENTS = "establishments"
}
