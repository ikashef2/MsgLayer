package app.msglayer.ui.navigation

sealed class Dest(val route: String) {
    data object Overview : Dest("overview")
    data object Inbox : Dest("inbox")
    data object Finance : Dest("finance")
    data object Ask : Dest("ask")
    data object Search : Dest("search")
    data object Rules : Dest("rules")
    data object Activity : Dest("activity")
    data object Settings : Dest("settings")
    data object MessageDetail : Dest("message/{id}") {
        fun path(id: String) = "message/$id"
    }
}
