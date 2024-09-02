package com.example.esds2s.Helpers.Enums

enum class AvailableLanguages {
    ARABIC,
    ENGLISH,
    SPANISH,
    FRENCH;

    companion object {
        fun getByIndex(index: Int): AvailableLanguages {
            return AvailableLanguages.values()[index];
        }
    }

}