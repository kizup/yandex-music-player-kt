package ru.kizapp.ymp.api.models.user

data class AccountResponse(
        val uid: Long?,
        val login: String?,
        val region: Long?,
        val fullName: String?,
        val secondName: String?,
        val firstName: String?,
        val displayName: String?,
        val serviceAvailable: Boolean?,
        val hostedUser: Boolean?
)