"""Decode Natural Earth 110m TopoJSON (world-atlas) into a small binary file of country rings.

Output format (big-endian):
  4 bytes  magic "WMAP"
  int32    version (1)
  int32    ring count
  per ring: int32 point count, then point count x (float32 lon, float32 lat)
Antarctica is left out: on a Mercator map it covers the whole bottom edge.
Rings that cross the 180 degree meridian are unwrapped (longitudes made continuous) and
written twice, once shifted by 360 degrees, so both map edges show the right shape.
"""
import json, struct, sys

src, dst = sys.argv[1], sys.argv[2]
topo = json.load(open(src))
sx, sy = topo["transform"]["scale"]
tx, ty = topo["transform"]["translate"]

arcs = []
for arc in topo["arcs"]:
    x = y = 0
    pts = []
    for dx, dy in arc:
        x += dx; y += dy
        pts.append((x * sx + tx, y * sy + ty))
    arcs.append(pts)

def unwrap(pts):
    out = [pts[0]]
    shift = 0.0
    for (lon, lat), (plon, _) in zip(pts[1:], pts):
        if lon - plon > 180: shift -= 360
        elif lon - plon < -180: shift += 360
        out.append((lon + shift, lat))
    return out

def ring(indexes):
    out = []
    for i in indexes:
        pts = arcs[i] if i >= 0 else list(reversed(arcs[~i]))
        out.extend(pts if not out else pts[1:])
    return out

rings = []
skipped = 0
for geom in topo["objects"]["countries"]["geometries"]:
    if geom.get("properties", {}).get("name") == "Antarctica" or geom.get("id") == "010":
        skipped += 1
        continue
    polys = [geom["arcs"]] if geom["type"] == "Polygon" else geom["arcs"] if geom["type"] == "MultiPolygon" else []
    for poly in polys:
        for r in poly:
            pts = ring(r)
            if len(pts) < 3:
                continue
            pts = unwrap(pts)
            rings.append(pts)
            lo = min(p[0] for p in pts); hi = max(p[0] for p in pts)
            if hi > 180: rings.append([(lon - 360, lat) for lon, lat in pts])
            if lo < -180: rings.append([(lon + 360, lat) for lon, lat in pts])

with open(dst, "wb") as f:
    f.write(b"WMAP")
    f.write(struct.pack(">ii", 1, len(rings)))
    for pts in rings:
        f.write(struct.pack(">i", len(pts)))
        for lon, lat in pts:
            f.write(struct.pack(">ff", lon, lat))

print(f"rings={len(rings)} points={sum(map(len, rings))} skipped={skipped}")
