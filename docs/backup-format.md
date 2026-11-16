# Backup format

Settings, Backup, Export writes one file. Import replaces all data with it. The file is a `.zip` (plain) or an `.nlbak` file (the same zip, encrypted with a password, F17). Import finds the type from the first bytes, not from the name.

## Content

| Entry | Content |
|---|---|
| `backup.json` | All people, tags (`tabs`), relationship types and relationships |
| `photos/<file name>` | One file per person photo, the same name as in `photoFile` |

## backup.json (version 1)

```json
{
  "format": "mmr.namelist.backup",
  "version": 1,
  "exportedAt": 1794700000000,
  "tabs": [{ "id": 1, "name": "Work", "sortIndex": 1 }],
  "people": [
    { "id": 1, "name": "Me", "notes": "", "photoFile": null, "metCity": null, "metLat": null,
      "metLon": null, "firstMetOn": null, "nativeLanguage": null, "birthDate": null,
      "education": null, "currentWork": null, "askAbout": null, "lastUpdatedAt": null,
      "archivedAt": null, "isSelf": true, "sortIndex": 0, "createdAt": 1794700000000, "tabId": null }
  ],
  "relationTypes": [{ "id": 5, "name": null, "builtIn": "PARENT", "sortIndex": 4 },
                    { "id": 10, "name": "Mentor", "builtIn": null, "sortIndex": 9 }],
  "relationships": [{ "id": 1, "from": 2, "to": 3, "typeId": 5, "type": "PARENT" }]
}
```

- `firstMetOn` and `birthDate` are days since 1970-01-01. `lastUpdatedAt` and `archivedAt` are milliseconds. All of these fields are optional; files from version 1.0.0 do not have them and import without them. Archived people are in the file (`archivedAt` set) and stay archived after an import.
- A relationship (from = A, to = B, type = T) means "B is A's T" (ADR-021).
- Tags are in `tabs`; the app calls them tags since 1.3.0 (ADR-037). Each person lists its tags in `tabIds` (F28); `tabId` is the lowest one, for older app versions.
- `relationTypes` (since 1.3.0, F30): `name` is null for a built-in type the user did not rename; `builtIn` is null for a type the user made. Built-in types have fixed ids 1 to 9.
- A relationship names its type in `typeId`. `type` stays for older app versions: the built-in type, or `OTHER` for a type the user made.
- A file without `relationTypes` (1.2.0 and older) gets the nine built-in types, and `type` is read by name. An unknown type reads as `OTHER`.
- Tags whose names differ only in case are merged on import (F29). The file format version stays 1: all new fields are optional, so older apps still read new files.

## Encrypted file (F17, ADR-028)

```
offset  size  content
0       8     "MMRNLENC"
8       1     format version, 1
9       4     PBKDF2 iterations, big endian (1,000 to 10,000,000 accepted)
13      16    salt
29      ...   Tink AES-GCM-HKDF streaming ciphertext (256-bit key, 64 KiB segments) of the zip
```

- Key: PBKDF2-HMAC-SHA256 of the password (UTF-16 chars as Java `PBEKeySpec` takes them), the salt and the iteration count, 256 bits. The app writes 600,000 iterations. Each file has its own random salt.
- The 29 header bytes are the associated data of the stream. A wrong password, a changed byte or a cut-off file fails the check.
- A wrong password cannot be told from a damaged file; the app says "Wrong password, or the file is damaged".
- Everything in this section is public. The security rests on the password.

## Import rules

1. The whole file is read, decrypted and checked before the database changes. A file that is not a zip, has no `backup.json`, has another `format`, or a newer `version` is rejected, and nothing changes.
2. Photo names must be plain file names (`A-Z a-z 0-9 . _ -`, not only dots). Other entries are ignored, so a file cannot write outside the photo folder.
3. Links to people that are not in the file, and tabs that do not exist, are dropped.
4. If the file has no "Me", the app creates one.
5. Photos that no person uses after the import are deleted.

## Changing the format

Add fields as optional. Raise `version` only for changes an older app cannot read, and keep reading all older versions.
