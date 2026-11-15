# World map data

`app/src/main/assets/world_110m.bin` holds the country borders for the map screen (M9).

- Source: [Natural Earth](https://www.naturalearthdata.com/) Admin 0 countries, 1:110m, version 4.1.0, public domain.
- Package: npm `world-atlas` 2.0.2 (ISC), file `countries-110m.json` (TopoJSON).
- Antarctica is left out. Rings across the 180° meridian are written twice (see `convert.py`).

Rebuild:

```
npm pack world-atlas@2.0.2 && tar -xzf world-atlas-2.0.2.tgz
python3 tools/map/convert.py package/countries-110m.json app/src/main/assets/world_110m.bin
```

The file is read by `platform/WorldMapData.kt`. Format: see the top of `convert.py`.
