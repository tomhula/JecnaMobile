package me.tomasan7.jecnamobile.settings

data class Contributor(
    val name: String,
    val githubUsername: String
)
{
    val githubUrl: String
        get() = "https://github.com/$githubUsername"
}

val AUTHOR = Contributor("Tomáš Hůla", "tomhula")

val CONTRIBUTORS = listOf(
    Contributor("Jakub Žitník", "jzitnik-dev"),
    Contributor("Štěpán Végh", "Stevekk11")
)

const val PROJECT_GITHUB_URL = "https://github.com/tomhula/JecnaMobile"
const val PROJECT_ISSUES_URL = "https://github.com/tomhula/JecnaMobile/issues"
