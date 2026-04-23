package am.loadboardbackend.service;

/**
 * @deprecated MongoDB GridFS support has been removed.
 * All file storage has been migrated to PostgreSQL BYTEA columns.
 * Use {@link DocumentFileService} and {@link DocumentStorageService} instead.
 */
@Deprecated(forRemoval = true)
public class MongoGridFsService {
    // This class is deprecated and should be removed after migration
}

