define(["./when-c2e8ef35", "./Check-c4f3a3fc", "./Math-d30358ed", "./Cartesian2-e875d9d2", "./Transforms-44592b02", "./RuntimeError-6122571f", "./WebGLConstants-4ae0db90", "./ComponentDatatype-5d3f6452", "./GeometryAttribute-4098b8de", "./GeometryAttributes-57608efc", "./IndexDatatype-e3260434", "./IntersectionTests-ef7d18d8", "./Plane-2d882f9f", "./VertexFormat-ad523db1", "./arrayRemoveDuplicates-a580a060", "./ArcType-29cf2197", "./EllipsoidRhumbLine-5134246a", "./EllipsoidGeodesic-19bdf744", "./PolylinePipeline-3852f7d2", "./Color-9e7980a7"], function (J, e, j, K, Q, t, r, X, Z, $, ee, a, o, y, te, re, n, i, ae, oe) {
    "use strict";
    var ne = [];

    function ie(e, t, r, a, o) {
        var n, i = ne;
        i.length = o;
        var l = r.red, s = r.green, p = r.blue, d = r.alpha, c = a.red, u = a.green, y = a.blue, m = a.alpha;
        if (oe.Color.equals(r, a)) {
            for (n = 0; n < o; n++) i[n] = oe.Color.clone(r);
            return i
        }
        var f = (c - l) / o, h = (u - s) / o, v = (y - p) / o, C = (m - d) / o;
        for (n = 0; n < o; n++) i[n] = new oe.Color(l + n * f, s + n * h, p + n * v, d + n * C);
        return i
    }

    function m(e) {
        var t = (e = J.defaultValue(e, J.defaultValue.EMPTY_OBJECT)).positions, r = e.colors,
            a = J.defaultValue(e.width, 1), o = J.defaultValue(e.colorsPerVertex, !1);
        this._positions = t, this._colors = r, this._width = a, this._colorsPerVertex = o, this._vertexFormat = y.VertexFormat.clone(J.defaultValue(e.vertexFormat, y.VertexFormat.DEFAULT)), this._arcType = J.defaultValue(e.arcType, re.ArcType.GEODESIC), this._granularity = J.defaultValue(e.granularity, j.CesiumMath.RADIANS_PER_DEGREE), this._ellipsoid = K.Ellipsoid.clone(J.defaultValue(e.ellipsoid, K.Ellipsoid.WGS84)), this._workerName = "createPolylineGeometry";
        var n = 1 + t.length * K.Cartesian3.packedLength;
        n += J.defined(r) ? 1 + r.length * oe.Color.packedLength : 1, this.packedLength = n + K.Ellipsoid.packedLength + y.VertexFormat.packedLength + 4
    }

    m.pack = function (e, t, r) {
        var a;
        r = J.defaultValue(r, 0);
        var o = e._positions, n = o.length;
        for (t[r++] = n, a = 0; a < n; ++a, r += K.Cartesian3.packedLength) K.Cartesian3.pack(o[a], t, r);
        var i = e._colors;
        for (n = J.defined(i) ? i.length : 0, t[r++] = n, a = 0; a < n; ++a, r += oe.Color.packedLength) oe.Color.pack(i[a], t, r);
        return K.Ellipsoid.pack(e._ellipsoid, t, r), r += K.Ellipsoid.packedLength, y.VertexFormat.pack(e._vertexFormat, t, r), r += y.VertexFormat.packedLength, t[r++] = e._width, t[r++] = e._colorsPerVertex ? 1 : 0, t[r++] = e._arcType, t[r] = e._granularity, t
    };
    var f = K.Ellipsoid.clone(K.Ellipsoid.UNIT_SPHERE), h = new y.VertexFormat, v = {
        positions: void 0,
        colors: void 0,
        ellipsoid: f,
        vertexFormat: h,
        width: void 0,
        colorsPerVertex: void 0,
        arcType: void 0,
        granularity: void 0
    };
    m.unpack = function (e, t, r) {
        var a;
        t = J.defaultValue(t, 0);
        var o = e[t++], n = new Array(o);
        for (a = 0; a < o; ++a, t += K.Cartesian3.packedLength) n[a] = K.Cartesian3.unpack(e, t);
        var i = 0 < (o = e[t++]) ? new Array(o) : void 0;
        for (a = 0; a < o; ++a, t += oe.Color.packedLength) i[a] = oe.Color.unpack(e, t);
        var l = K.Ellipsoid.unpack(e, t, f);
        t += K.Ellipsoid.packedLength;
        var s = y.VertexFormat.unpack(e, t, h);
        t += y.VertexFormat.packedLength;
        var p = e[t++], d = 1 === e[t++], c = e[t++], u = e[t];
        return J.defined(r) ? (r._positions = n, r._colors = i, r._ellipsoid = K.Ellipsoid.clone(l, r._ellipsoid), r._vertexFormat = y.VertexFormat.clone(s, r._vertexFormat), r._width = p, r._colorsPerVertex = d, r._arcType = c, r._granularity = u, r) : (v.positions = n, v.colors = i, v.width = p, v.colorsPerVertex = d, v.arcType = c, v.granularity = u, new m(v))
    };
    var le = new K.Cartesian3, se = new K.Cartesian3, pe = new K.Cartesian3, de = new K.Cartesian3;
    return m.createGeometry = function (e) {
        var t, r, a, o = e._width, n = e._vertexFormat, i = e._colors, l = e._colorsPerVertex, s = e._arcType,
            p = e._granularity, d = e._ellipsoid,
            c = te.arrayRemoveDuplicates(e._positions, K.Cartesian3.equalsEpsilon), u = c.length;
        if (!(u < 2 || o <= 0)) {
            if (s === re.ArcType.GEODESIC || s === re.ArcType.RHUMB) {
                var y, m;
                m = s === re.ArcType.GEODESIC ? (y = j.CesiumMath.chordLength(p, d.maximumRadius), ae.PolylinePipeline.numberOfPoints) : (y = p, ae.PolylinePipeline.numberOfPointsRhumbLine);
                var f = ae.PolylinePipeline.extractHeights(c, d);
                if (J.defined(i)) {
                    var h = 1;
                    for (t = 0; t < u - 1; ++t) h += m(c[t], c[t + 1], y);
                    var v = new Array(h), C = 0;
                    for (t = 0; t < u - 1; ++t) {
                        var g = c[t], _ = c[t + 1], A = i[t], E = m(g, _, y);
                        if (l && t < h) {
                            var P = ie(0, 0, A, i[t + 1], E), b = P.length;
                            for (r = 0; r < b; ++r) v[C++] = P[r]
                        } else for (r = 0; r < E; ++r) v[C++] = oe.Color.clone(A)
                    }
                    v[C] = oe.Color.clone(i[i.length - 1]), i = v, ne.length = 0
                }
                c = s === re.ArcType.GEODESIC ? ae.PolylinePipeline.generateCartesianArc({
                    positions: c,
                    minDistance: y,
                    ellipsoid: d,
                    height: f
                }) : ae.PolylinePipeline.generateCartesianRhumbArc({
                    positions: c,
                    granularity: y,
                    ellipsoid: d,
                    height: f
                })
            }
            var w, T = 4 * (u = c.length) - 4, x = new Float64Array(3 * T), k = new Float64Array(3 * T),
                D = new Float64Array(3 * T), V = new Float32Array(2 * T), L = n.st ? new Float32Array(2 * T) : void 0,
                F = J.defined(i) ? new Uint8Array(4 * T) : void 0, G = 0, O = 0, R = 0, I = 0;
            for (r = 0; r < u; ++r) {
                var S, B;
                0 === r ? (w = le, K.Cartesian3.subtract(c[0], c[1], w), K.Cartesian3.add(c[0], w, w)) : w = c[r - 1], K.Cartesian3.clone(w, pe), K.Cartesian3.clone(c[r], se), r === u - 1 ? (w = le, K.Cartesian3.subtract(c[u - 1], c[u - 2], w), K.Cartesian3.add(c[u - 1], w, w)) : w = c[r + 1], K.Cartesian3.clone(w, de), J.defined(F) && (S = 0 === r || l ? i[r] : i[r - 1], r !== u - 1 && (B = i[r]));
                var U = r === u - 1 ? 2 : 4;
                for (a = 0 === r ? 2 : 0; a < U; ++a) {
                    K.Cartesian3.pack(se, x, G), K.Cartesian3.pack(pe, k, G), K.Cartesian3.pack(de, D, G), G += 3;
                    var N = a - 2 < 0 ? -1 : 1;
                    if (V[O++] = a % 2 * 2 - 1, V[O++] = N * o, n.st && (L[R++] = r / (u - 1), L[R++] = Math.max(V[O - 2], 0)), J.defined(F)) {
                        var M = a < 2 ? S : B;
                        F[I++] = oe.Color.floatToByte(M.red), F[I++] = oe.Color.floatToByte(M.green), F[I++] = oe.Color.floatToByte(M.blue), F[I++] = oe.Color.floatToByte(M.alpha)
                    }
                }
            }
            var H = new $.GeometryAttributes;
            H.position = new Z.GeometryAttribute({
                componentDatatype: X.ComponentDatatype.DOUBLE,
                componentsPerAttribute: 3,
                values: x
            }), H.prevPosition = new Z.GeometryAttribute({
                componentDatatype: X.ComponentDatatype.DOUBLE,
                componentsPerAttribute: 3,
                values: k
            }), H.nextPosition = new Z.GeometryAttribute({
                componentDatatype: X.ComponentDatatype.DOUBLE,
                componentsPerAttribute: 3,
                values: D
            }), H.expandAndWidth = new Z.GeometryAttribute({
                componentDatatype: X.ComponentDatatype.FLOAT,
                componentsPerAttribute: 2,
                values: V
            }), n.st && (H.st = new Z.GeometryAttribute({
                componentDatatype: X.ComponentDatatype.FLOAT,
                componentsPerAttribute: 2,
                values: L
            })), J.defined(F) && (H.color = new Z.GeometryAttribute({
                componentDatatype: X.ComponentDatatype.UNSIGNED_BYTE,
                componentsPerAttribute: 4,
                values: F,
                normalize: !0
            }));
            var W = ee.IndexDatatype.createTypedArray(T, 6 * u - 6), Y = 0, q = 0, z = u - 1;
            for (r = 0; r < z; ++r) W[q++] = Y, W[q++] = Y + 2, W[q++] = Y + 1, W[q++] = Y + 1, W[q++] = Y + 2, W[q++] = Y + 3, Y += 4;
            return new Z.Geometry({
                attributes: H,
                indices: W,
                primitiveType: Z.PrimitiveType.TRIANGLES,
                boundingSphere: Q.BoundingSphere.fromPoints(c),
                geometryType: Z.GeometryType.POLYLINES
            })
        }
    }, function (e, t) {
        return J.defined(t) && (e = m.unpack(e, t)), e._ellipsoid = K.Ellipsoid.clone(e._ellipsoid), m.createGeometry(e)
    }
});
