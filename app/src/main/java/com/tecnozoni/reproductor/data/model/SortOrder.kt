package com.tecnozoni.reproductor.data.model

/** Criterios de orden de la lista. */
enum class SortOrder {
    NAME,
    DURATION,
    DATE_MODIFIED,
    CUSTOM, // orden personalizado arrastrable, persistido en Room
}

/** Dirección del orden (no aplica a CUSTOM, que es manual). */
enum class SortDirection {
    ASC,
    DESC,
}
