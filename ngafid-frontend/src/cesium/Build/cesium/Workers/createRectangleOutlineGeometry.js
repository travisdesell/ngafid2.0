define(["./when-c2e8ef35", "./Check-c4f3a3fc", "./Math-d30358ed", "./Cartesian2-e875d9d2", "./Transforms-44592b02", "./RuntimeError-6122571f", "./WebGLConstants-4ae0db90", "./ComponentDatatype-5d3f6452", "./GeometryAttribute-4098b8de", "./GeometryAttributes-57608efc", "./IndexDatatype-e3260434", "./GeometryOffsetAttribute-e6e9672c", "./EllipsoidRhumbLine-5134246a", "./PolygonPipeline-84f0d07f", "./RectangleGeometryLibrary-bb0e14b8"], function (h, e, y, p, m, t, i, E, A, G, R, b, a, P, w) {
    "use strict";
    var _ = new m.BoundingSphere, v = new m.BoundingSphere, L = new p.Cartesian3, C = new p.Rectangle;

    function D(e, t) {
        var i = e._ellipsoid, a = t.height, r = t.width, n = t.northCap, o = t.southCap, l = a, u = 2, s = 0, p = 4;
        n && (--u, --l, s += 1, p -= 2), o && (--u, --l, s += 1, p -= 2), s += u * r + 2 * l - p;
        var d, c = new Float64Array(3 * s), f = 0, g = 0, h = L;
        if (n) w.RectangleGeometryLibrary.computePosition(t, i, !1, g, 0, h), c[f++] = h.x, c[f++] = h.y, c[f++] = h.z; else for (d = 0; d < r; d++) w.RectangleGeometryLibrary.computePosition(t, i, !1, g, d, h), c[f++] = h.x, c[f++] = h.y, c[f++] = h.z;
        for (d = r - 1, g = 1; g < a; g++) w.RectangleGeometryLibrary.computePosition(t, i, !1, g, d, h), c[f++] = h.x, c[f++] = h.y, c[f++] = h.z;
        if (g = a - 1, !o) for (d = r - 2; 0 <= d; d--) w.RectangleGeometryLibrary.computePosition(t, i, !1, g, d, h), c[f++] = h.x, c[f++] = h.y, c[f++] = h.z;
        for (d = 0, g = a - 2; 0 < g; g--) w.RectangleGeometryLibrary.computePosition(t, i, !1, g, d, h), c[f++] = h.x, c[f++] = h.y, c[f++] = h.z;
        for (var y = c.length / 3 * 2, m = R.IndexDatatype.createTypedArray(c.length / 3, y), b = 0, _ = 0; _ < c.length / 3 - 1; _++) m[b++] = _, m[b++] = _ + 1;
        m[b++] = c.length / 3 - 1, m[b++] = 0;
        var v = new A.Geometry({attributes: new G.GeometryAttributes, primitiveType: A.PrimitiveType.LINES});
        return v.attributes.position = new A.GeometryAttribute({
            componentDatatype: E.ComponentDatatype.DOUBLE,
            componentsPerAttribute: 3,
            values: c
        }), v.indices = m, v
    }

    function d(e) {
        var t = (e = h.defaultValue(e, h.defaultValue.EMPTY_OBJECT)).rectangle,
            i = h.defaultValue(e.granularity, y.CesiumMath.RADIANS_PER_DEGREE),
            a = h.defaultValue(e.ellipsoid, p.Ellipsoid.WGS84), r = h.defaultValue(e.rotation, 0),
            n = h.defaultValue(e.height, 0), o = h.defaultValue(e.extrudedHeight, n);
        this._rectangle = p.Rectangle.clone(t), this._granularity = i, this._ellipsoid = a, this._surfaceHeight = Math.max(n, o), this._rotation = r, this._extrudedHeight = Math.min(n, o), this._offsetAttribute = e.offsetAttribute, this._workerName = "createRectangleOutlineGeometry"
    }

    d.packedLength = p.Rectangle.packedLength + p.Ellipsoid.packedLength + 5, d.pack = function (e, t, i) {
        return i = h.defaultValue(i, 0), p.Rectangle.pack(e._rectangle, t, i), i += p.Rectangle.packedLength, p.Ellipsoid.pack(e._ellipsoid, t, i), i += p.Ellipsoid.packedLength, t[i++] = e._granularity, t[i++] = e._surfaceHeight, t[i++] = e._rotation, t[i++] = e._extrudedHeight, t[i] = h.defaultValue(e._offsetAttribute, -1), t
    };
    var c = new p.Rectangle, f = p.Ellipsoid.clone(p.Ellipsoid.UNIT_SPHERE), g = {
        rectangle: c,
        ellipsoid: f,
        granularity: void 0,
        height: void 0,
        rotation: void 0,
        extrudedHeight: void 0,
        offsetAttribute: void 0
    };
    d.unpack = function (e, t, i) {
        t = h.defaultValue(t, 0);
        var a = p.Rectangle.unpack(e, t, c);
        t += p.Rectangle.packedLength;
        var r = p.Ellipsoid.unpack(e, t, f);
        t += p.Ellipsoid.packedLength;
        var n = e[t++], o = e[t++], l = e[t++], u = e[t++], s = e[t];
        return h.defined(i) ? (i._rectangle = p.Rectangle.clone(a, i._rectangle), i._ellipsoid = p.Ellipsoid.clone(r, i._ellipsoid), i._surfaceHeight = o, i._rotation = l, i._extrudedHeight = u, i._offsetAttribute = -1 === s ? void 0 : s, i) : (g.granularity = n, g.height = o, g.rotation = l, g.extrudedHeight = u, g.offsetAttribute = -1 === s ? void 0 : s, new d(g))
    };
    var x = new p.Cartographic;
    return d.createGeometry = function (e) {
        var t, i, a = e._rectangle, r = e._ellipsoid,
            n = w.RectangleGeometryLibrary.computeOptions(a, e._granularity, e._rotation, 0, C, x);
        if (!y.CesiumMath.equalsEpsilon(a.north, a.south, y.CesiumMath.EPSILON10) && !y.CesiumMath.equalsEpsilon(a.east, a.west, y.CesiumMath.EPSILON10)) {
            var o, l = e._surfaceHeight, u = e._extrudedHeight;
            if (!y.CesiumMath.equalsEpsilon(l, u, 0, y.CesiumMath.EPSILON2)) {
                if (t = function (e, t) {
                    var i = e._surfaceHeight, a = e._extrudedHeight, r = e._ellipsoid, n = a, o = i, l = D(e, t),
                        u = t.height, s = t.width,
                        p = P.PolygonPipeline.scaleToGeodeticHeight(l.attributes.position.values, o, r, !1),
                        d = p.length, c = new Float64Array(2 * d);
                    c.set(p);
                    var f = P.PolygonPipeline.scaleToGeodeticHeight(l.attributes.position.values, n, r);
                    c.set(f, d), l.attributes.position.values = c;
                    var g = t.northCap, h = t.southCap, y = 4;
                    g && --y, h && --y;
                    var m = 2 * (c.length / 3 + y), b = R.IndexDatatype.createTypedArray(c.length / 3, m);
                    d = c.length / 6;
                    for (var _, v = 0, E = 0; E < d - 1; E++) b[v++] = E, b[v++] = E + 1, b[v++] = E + d, b[v++] = E + d + 1;
                    if (b[v++] = d - 1, b[v++] = 0, b[v++] = d + d - 1, b[v++] = d, b[v++] = 0, b[v++] = d, g) _ = u - 1; else {
                        var A = s - 1;
                        b[v++] = A, b[v++] = A + d, _ = s + u - 2
                    }
                    if (b[v++] = _, b[v++] = _ + d, !h) {
                        var G = s + _ - 1;
                        b[v++] = G, b[v] = G + d
                    }
                    return l.indices = b, l
                }(e, n), h.defined(e._offsetAttribute)) {
                    var s = t.attributes.position.values.length / 3, p = new Uint8Array(s);
                    p = e._offsetAttribute === b.GeometryOffsetAttribute.TOP ? b.arrayFill(p, 1, 0, s / 2) : (o = e._offsetAttribute === b.GeometryOffsetAttribute.NONE ? 0 : 1, b.arrayFill(p, o)), t.attributes.applyOffset = new A.GeometryAttribute({
                        componentDatatype: E.ComponentDatatype.UNSIGNED_BYTE,
                        componentsPerAttribute: 1,
                        values: p
                    })
                }
                var d = m.BoundingSphere.fromRectangle3D(a, r, l, v), c = m.BoundingSphere.fromRectangle3D(a, r, u, _);
                i = m.BoundingSphere.union(d, c)
            } else {
                if ((t = D(e, n)).attributes.position.values = P.PolygonPipeline.scaleToGeodeticHeight(t.attributes.position.values, l, r, !1), h.defined(e._offsetAttribute)) {
                    var f = t.attributes.position.values.length, g = new Uint8Array(f / 3);
                    o = e._offsetAttribute === b.GeometryOffsetAttribute.NONE ? 0 : 1, b.arrayFill(g, o), t.attributes.applyOffset = new A.GeometryAttribute({
                        componentDatatype: E.ComponentDatatype.UNSIGNED_BYTE,
                        componentsPerAttribute: 1,
                        values: g
                    })
                }
                i = m.BoundingSphere.fromRectangle3D(a, r, l)
            }
            return new A.Geometry({
                attributes: t.attributes,
                indices: t.indices,
                primitiveType: A.PrimitiveType.LINES,
                boundingSphere: i,
                offsetAttribute: e._offsetAttribute
            })
        }
    }, function (e, t) {
        return h.defined(t) && (e = d.unpack(e, t)), e._ellipsoid = p.Ellipsoid.clone(e._ellipsoid), e._rectangle = p.Rectangle.clone(e._rectangle), d.createGeometry(e)
    }
});
