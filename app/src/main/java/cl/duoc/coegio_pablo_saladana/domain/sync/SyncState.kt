package cl.duoc.coegio_pablo_saladana.domain.sync

/**
 * Estado de sincronización de una fila local respecto al backend.
 * Se guarda como TEXT en Room (columna syncState, migración 10→11).
 */
enum class SyncState {
    /** Creado localmente; el backend aún no lo conoce. */
    LOCAL_ONLY,

    /** Con cambios locales esperando enviarse al backend. */
    PENDING_UPLOAD,

    /** Confirmado por el backend con su identificador remoto. */
    SYNCED,

    /** Local y remoto divergieron; requiere la política de resolución documentada. */
    CONFLICT,

    /** El último intento de sincronización falló (ver syncError). */
    ERROR;

    companion object {
        /** Valor conservador para filas existentes: nunca asume que ya están en el servidor. */
        fun fromStored(value: String?): SyncState =
            entries.firstOrNull { it.name == value } ?: LOCAL_ONLY
    }
}

/**
 * Política de conflictos documentada (aún no se ejecuta automáticamente):
 * cuando el backend implemente `updatedAt`, gana la modificación más reciente
 * (last-write-wins). Mientras el backend no exponga esa marca, gana lo LOCAL,
 * porque es la única fuente completa del MVP y no se debe perder trabajo del usuario.
 */
enum class ConflictResolution { LOCAL_WINS, REMOTE_WINS }
