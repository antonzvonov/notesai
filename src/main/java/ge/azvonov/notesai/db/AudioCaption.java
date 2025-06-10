package ge.azvonov.notesai.db;

public record AudioCaption(long id, long fileId, double startTime, double endTime, String speaker, String text) {}
