package com.michalmarcin.namelist.testutil

import com.michalmarcin.namelist.data.db.Person

/** Fictional people for tests and screenshots (AGENTS.md rule 9). */
object SampleData {
    val people = listOf(
        Person(id = 2, name = "Anna Nováková", notes = "Met at the Brno tram stop", metCity = "Brno", photoFile = "anna.jpg", sortIndex = 1),
        Person(id = 3, name = "Peter Horváth", notes = "Plays badminton on Thursdays", sortIndex = 2),
        Person(id = 4, name = "Jana Svobodová", notes = "Colleague from the design team", metCity = "Praha", photoFile = "jana.jpg", sortIndex = 3),
        Person(id = 5, name = "Tomáš Král", notes = "", sortIndex = 4),
        Person(id = 6, name = "Eva Malá", notes = "Neighbour, has a black dog", metCity = "Jihlava", sortIndex = 5),
        Person(id = 7, name = "Šimon Dvořák", notes = "Friend of Peter", sortIndex = 6),
    )
    val anna = people.first().copy(notes = "Met at the Brno tram stop.\nLikes hiking and strong coffee.")
}
