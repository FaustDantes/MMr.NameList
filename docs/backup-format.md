# Backup format

Settings, Backup, Export writes one `.zip` file. Import replaces all data with it.

## Content

| Entry | Content |
|---|---|
| `backup.json` | All people, tabs and relationships |
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
      "metLon": null, "isSelf": true, "sortIndex": 0, "createdAt": 1794700000000, "tabId": null }
  ],
  "relationships": [{ "id": 1, "from": 2, "to": 3, "type": "PARENT" }]
}
```

- A relationship (from = A, to = B, type = T) means "B is A's T" (ADR-021).
- Types are stored by name. An unknown type reads as `OTHER`.

## Import rules

1. The whole file is read and checked before the database changes. A file that is not a zip, has no `backup.json`, has another `format`, or a newer `version` is rejected, and nothing changes.
2. Photo names must be plain file names (`A-Z a-z 0-9 . _ -`, not only dots). Other entries are ignored, so a file cannot write outside the photo folder.
3. Links to people that are not in the file, and tabs that do not exist, are dropped.
4. If the file has no "Me", the app creates one.
5. Photos that no person uses after the import are deleted.

## Changing the format

Add fields as optional. Raise `version` only for changes an older app cannot read, and keep reading all older versions.
