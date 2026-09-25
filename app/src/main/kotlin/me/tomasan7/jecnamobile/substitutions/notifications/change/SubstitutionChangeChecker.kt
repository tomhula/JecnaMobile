package me.tomasan7.jecnamobile.substitutions.notifications.change

import me.tomasan7.jecnamobile.timetable.SubstitutionData

interface SubstitutionChangeChecker
{
    fun checkForChanges(
        oldData: SubstitutionData,
        newData: SubstitutionData
    ): List<SubstitutionChange>
}
